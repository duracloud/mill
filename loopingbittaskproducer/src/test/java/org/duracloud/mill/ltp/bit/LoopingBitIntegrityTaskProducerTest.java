/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;

import java.io.File;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.duracloud.common.queue.local.LocalTaskQueue;
import org.duracloud.common.util.WaitUtil;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.db.repo.JpaBitIntegrityReportRepo;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducer;
import org.duracloud.mill.ltp.PathFilterManager;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
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
public class LoopingBitIntegrityTaskProducerTest extends EasyMockSupport {

    @Mock
    private CredentialsRepo credentialsRepo;
    @Mock
    private StorageProviderFactory storageProviderFactory;
    @Mock
    private StorageProvider store;

    @Mock
    private NotificationManager notificationManager;

    private StateManager<BitIntegrityMorsel> stateManager;

    private TaskQueue bitQueue;

    @Mock
    private LoopingBitTaskProducerConfigurationManager config;

    @Mock
    private JpaBitIntegrityReportRepo bitReportRepo;

    private TaskQueue bitReportQueue;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        File stateFile = File.createTempFile("state", "json");
        stateFile.deleteOnExit();
        stateManager = new StateManager<>(stateFile.getAbsolutePath(), BitIntegrityMorsel.class);
        bitQueue = new LocalTaskQueue();
        bitReportQueue = new LocalTaskQueue();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
    }

    //scenarios
    //verify that each space is processed completely and the queue emptied, before a report
    //task is added and the next space is processed.

    /**
     * Test method for {@link org.duracloud.mill.ltp.LoopingTaskProducer#run()}.
     *
     * @throws CredentialsRepoException
     */
    @Test
    public void testRun() throws CredentialsRepoException, ParseException {

        int morselCount = 2;
        int sourceCount = 2000;

        setupStore(morselCount, sourceCount);
        setupStorageProviderFactory(morselCount * 2);
        setupCredentialsRepo();
        setupNotificationManager(1);
        setupLoopingTaskProducerConfig(1);
        setupBitReportRepo(2);
        int maxTaskQueueSize = calculateMaxQueueSize(morselCount, sourceCount);
        replayAll();
        LoopingTaskProducer<BitIntegrityMorsel> ltp = createTaskProducer(maxTaskQueueSize);

        ltp.run();

        //after first run, queue should be loaded with the first morsel.
        Assert.assertEquals(sourceCount, bitQueue.size().intValue());

        //running ltp again should result in no change to the taskqueue.
        ltp.run();

        Assert.assertEquals(sourceCount, bitQueue.size().intValue());

        //drain queue and run again
        int tasksProcessed = drainQueue(bitQueue);

        Assert.assertEquals(0, bitQueue.size().intValue());

        ltp.run();
        //now we now expect a single report task to appear on the queue + all of the next spaces items sans report task.
        Assert.assertEquals(sourceCount, bitQueue.size().intValue());
        Assert.assertEquals(1, bitReportQueue.size().intValue());

        tasksProcessed += drainQueue(bitQueue);

        //process second morsel
        ltp.run();
        Assert.assertEquals(0, bitQueue.size().intValue());
        tasksProcessed += drainQueue(bitQueue);

        //verify that the total number of tasks processed equals the sum of all bit integrity
        //and report tasks.
        Assert.assertEquals(tasksProcessed, maxTaskQueueSize);

        Assert.assertEquals(morselCount, bitReportQueue.size().intValue());
    }

    @Test
    public void testRunWithZeroFrequency() throws CredentialsRepoException, ParseException {

        replayAll();
        LoopingTaskProducer<BitIntegrityMorsel> ltp = createTaskProducer(100, "0s");

        ltp.run();
        WaitUtil.wait(2);

        //after first run, queue should be loaded with the first morsel.
        Assert.assertEquals(0, bitQueue.size().intValue());
        Assert.assertNull(this.stateManager.getCurrentRunStartDate());
        Assert.assertNull(this.stateManager.getNextRunStartDate());

    }

    private void setupBitReportRepo(int times) {
        expect(bitReportRepo
                   .findFirstByAccountAndStoreIdAndSpaceIdOrderByCompletionDateDesc(EasyMock.isA(String.class),
                                                                                    EasyMock.isA(String.class),
                                                                                    EasyMock.isA(String.class)))
            .andReturn(null).times(times);
    }

    private void setupLoopingTaskProducerConfig(int times) {
        expect(this.config.getWorkDirectoryPath()).andReturn("java.io.tmpdir").times(times);
    }

    private void setupNotificationManager(int times) {
        notificationManager.sendEmail(isA(String.class), isA(String.class));
        expectLastCall().times(times);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNonExistentSpace() throws CredentialsRepoException, ParseException {
        int morselCount = 1;
        expectNotFoundOnGetSpaceContentsChunked(store);
        setupBitReportRepo(1);

        expectGetSpaces(morselCount);
        setupStorageProviderFactory(morselCount);
        setupCredentialsRepo();
        setupNotificationManager(2);

        setupLoopingTaskProducerConfig(1);

        replayAll();

        runLoopingTaskProducer(1);
        Assert.assertEquals(0, bitQueue.size().intValue());
        Assert.assertEquals(1, bitReportQueue.size().intValue());

    }

    /**
     *
     */
    private void setupEmptySourceSpacesGetChunked() {
        EasyMock.expect(store.getSpaceContentsChunked(EasyMock.isA(String.class),
                                                      EasyMock.isNull(String.class),
                                                      EasyMock.anyInt(),
                                                      EasyMock.isA(String.class)))
                .andReturn(new LinkedList<String>());
    }

    /**
     * @param tasksProcessed
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
     * @throws ParseException
     */
    private void runLoopingTaskProducer(int maxQueueSize) throws ParseException {

        LoopingBitIntegrityTaskProducer producer = createTaskProducer(maxQueueSize);
        producer.setWaitBetweenRetriesMs(1);
        producer.run();
    }

    /**
     * @param maxQueueSize
     * @return
     * @throws ParseException
     */
    private LoopingBitIntegrityTaskProducer createTaskProducer(int maxQueueSize)
        throws ParseException {
        return createTaskProducer(maxQueueSize, "1s");
    }

    private LoopingBitIntegrityTaskProducer createTaskProducer(int maxQueueSize, String frequency)
        throws ParseException {
        LoopingBitIntegrityTaskProducer producer =
            new LoopingBitIntegrityTaskProducer(credentialsRepo,
                                                bitReportRepo,
                                                storageProviderFactory,
                                                bitQueue,
                                                bitReportQueue,
                                                stateManager,
                                                maxQueueSize,
                                                new Frequency(frequency),
                                                notificationManager,
                                                new PathFilterManager(),
                                                this.config);
        producer.setWaitTimeInMsBeforeQueueSizeCheck(1);
        return producer;
    }

    /**
     * @param morselCount
     */
    private void expectEmptyListFromGetSpaceContents(StorageProvider store, int morselCount) {
        EasyMock.expect(store.getSpaceContents(EasyMock.isA(String.class), EasyMock.isNull(String.class)))
                .andAnswer(new IAnswer<Iterator<String>>() {
                    /* (non-Javadoc)
                     * @see org.easymock.IAnswer#answer()
                     */
                    @Override
                    public Iterator<String> answer() throws Throwable {
                        return new LinkedList<String>().iterator();
                    }
                })
                .times(morselCount);
    }

    private void expectNotFoundOnGetSpaceContentsChunked(StorageProvider store) {
        EasyMock.expect(store.getSpaceContentsChunked(EasyMock.isA(String.class),
                                                      EasyMock.isNull(String.class),
                                                      EasyMock.anyInt(),
                                                      EasyMock.isNull(String.class)))
                .andThrow(new NotFoundException("test")).times(4);
    }

    /**
     * @param morselCount
     * @param storeCount
     * @return
     */
    private int calculateMaxQueueSize(int morselCount,
                                      int storeCount) {
        return (morselCount * storeCount);
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

        AccountCredentials creds = new AccountCredentials(account,
                                                          Arrays.asList(storageCreds));

        EasyMock.expect(credentialsRepo.getStorageProviderCredentials(
            EasyMock.isA(String.class), EasyMock.isA(String.class)))
                .andReturn(storageCreds).atLeastOnce();

        EasyMock.expect(
            credentialsRepo.getAccountCredentials(
                EasyMock.isA(String.class)))
                .andReturn(creds).atLeastOnce();

        EasyMock.expect(credentialsRepo.getActiveAccounts()).andReturn(accounts);

        EasyMock.expect(credentialsRepo
                            .isAccountActive(EasyMock.isA(String.class))).andReturn(true).atLeastOnce();

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
     * @param sourceCount
     */
    private void setupStore(final int morselCount, int sourceCount) {
        final List<String> sourceContentItems = new LinkedList<>();

        for (int i = 0; i < sourceCount; i++) {
            sourceContentItems.add("item" + i);
        }

        for (int i = 0; i < morselCount; i++) {

            int count = 1000;

            String spaceId = "space" + i;
            EasyMock.expect(store.getSpaceContentsChunked(spaceId, null, 1000, null))
                    .andReturn(sourceContentItems.subList(0, Math.min(count, sourceCount)));

            while (count <= sourceCount) {
                EasyMock.expect(store.getSpaceContentsChunked(spaceId, null, 1000,
                                                              sourceContentItems.subList(count - 1, count).get(0)))
                        .andReturn(sourceContentItems.subList(count, sourceCount)).atLeastOnce();
                count += 1000;

            }
        }

        expectGetSpaces(morselCount);
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
