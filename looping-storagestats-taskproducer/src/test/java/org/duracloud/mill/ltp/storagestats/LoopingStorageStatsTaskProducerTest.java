/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.storagestats;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;

import java.io.File;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.duracloud.common.queue.local.LocalTaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.PathFilterManager;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 * Date: May 1, 2014
 */
@RunWith(EasyMockRunner.class)
public class LoopingStorageStatsTaskProducerTest extends EasyMockSupport {

    @Mock
    private CredentialsRepo credentialsRepo;
    @Mock
    private StorageProviderFactory storageProviderFactory;
    @Mock
    private StorageProvider store;

    @Mock
    private NotificationManager notificationManager;

    private StateManager<StorageStatsMorsel> stateManager;

    private TaskQueue queue;

    @Mock
    private LoopingStorageStatsTaskProducerConfigurationManager config;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        File stateFile = File.createTempFile("state", "json");
        stateFile.deleteOnExit();
        stateManager = new StateManager<>(stateFile.getAbsolutePath(), StorageStatsMorsel.class);
        queue = new LocalTaskQueue();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
    }

    //scenarios
    //verify that each space is processed completely and the queue emptied

    @Test
    public void testRun() throws CredentialsRepoException, ParseException {

        int morselCount = 2;

        expectGetSpaces(morselCount);
        setupStorageProviderFactory(morselCount * 2);
        setupCredentialsRepo();
        setupNotificationManager();
        setupLoopingTaskProducerConfig();
        int maxTaskQueueSize = 10000;
        replayAll();
        LoopingStorageStatsTaskProducer ltp = createTaskProducer(maxTaskQueueSize, null);

        ltp.run();

        //after first run, queue should be loaded with the first morsel.
        Assert.assertEquals(morselCount, queue.size().intValue());

        //running ltp again should result in no change to the taskqueue.
        ltp.run();

        Assert.assertEquals(morselCount, queue.size().intValue());

    }

    @Test
    public void testStartLaterInTheDay() throws CredentialsRepoException, ParseException {

        int maxTaskQueueSize = 10000;
        replayAll();
        LoopingStorageStatsTaskProducer ltp = createTaskProducer(maxTaskQueueSize, LocalTime.parse("23:59:59"));

        ltp.run();

        //after first run, queue should be loaded with the first morsel.
        Assert.assertEquals(0, queue.size().intValue());
    }

    private void setupLoopingTaskProducerConfig() {
        expect(this.config.getWorkDirectoryPath()).andReturn(System.getProperty("java.io.tmpdir")).times(1);
    }

    private void setupNotificationManager() {
        notificationManager.sendEmail(isA(String.class), isA(String.class));
        expectLastCall().once();
    }

    /**
     * @return
     */
    private int drainQueue(TaskQueue queue) {
        int tasksProcessed = 0;
        while (queue.size() > 0) {
            try {
                queue.take();
                tasksProcessed++;
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        return tasksProcessed;
    }

    /**
     * @param maxQueueSize
     * @return
     * @throws ParseException
     */
    private LoopingStorageStatsTaskProducer createTaskProducer(int maxQueueSize, LocalTime startTime)
        throws ParseException {
        LoopingStorageStatsTaskProducer producer = new LoopingStorageStatsTaskProducer(credentialsRepo,
                                                                                       storageProviderFactory,
                                                                                       queue,
                                                                                       stateManager,
                                                                                       maxQueueSize,
                                                                                       new Frequency("1h"),
                                                                                       startTime,
                                                                                       notificationManager,
                                                                                       new PathFilterManager(),
                                                                                       this.config);
        producer.setWaitTimeInMsBeforeQueueSizeCheck(1);
        return producer;
    }

    /**
     * @throws CredentialsRepoException
     */
    private void setupCredentialsRepo() throws CredentialsRepoException {
        List<String> accounts = new LinkedList<>();
        String account = "test-account";
        accounts.add(account);

        StorageProviderCredentials storageCreds = new StorageProviderCredentials("id",
                                                                                 "test-access-key",
                                                                                 "test-secrety-key",
                                                                                 StorageProviderType.AMAZON_S3,
                                                                                 null,
                                                                                 true);

        AccountCredentials creds = new AccountCredentials(account, Arrays.asList(storageCreds));

        EasyMock.expect(credentialsRepo.getStorageProviderCredentials(
            EasyMock.isA(String.class), EasyMock.isA(String.class)))
                .andReturn(storageCreds).atLeastOnce();

        EasyMock.expect(credentialsRepo.getAccountCredentials(
            EasyMock.isA(String.class)))
                .andReturn(creds).atLeastOnce();

        EasyMock.expect(credentialsRepo.getActiveAccounts()).andReturn(accounts);
        EasyMock.expect(credentialsRepo.isAccountActive(EasyMock.isA(String.class))).andReturn(true).times(2);

    }

    /**
     * @param rounds
     */
    private void setupStorageProviderFactory(int rounds) {
        EasyMock.expect(storageProviderFactory.create(EasyMock.isA(StorageProviderCredentials.class)))
                .andReturn(store).atLeastOnce();
    }

    /**
     * @param morselCount
     */
    private void expectGetSpaces(final int morselCount) {
        EasyMock.expect(store.getSpaces()).andAnswer(new IAnswer<Iterator<String>>() {
            /* (non-Javadoc)
             * @see org.easymock.IAnswer#answer()
             */
            @Override
            public Iterator<String> answer() throws Throwable {
                List<String> spaces = new LinkedList<>();
                for (int i = 0; i < morselCount; ++i) {
                    spaces.add("space" + i);
                }
                return spaces.iterator();
            }
        }).anyTimes();
    }
}
