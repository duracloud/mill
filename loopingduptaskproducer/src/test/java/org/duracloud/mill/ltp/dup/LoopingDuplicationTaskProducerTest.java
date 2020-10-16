/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.duracloud.common.queue.local.LocalTaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducerConfigurationManager;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.notification.NotificationManager;
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
 * Date: Nov 6, 2013
 */
@RunWith(EasyMockRunner.class)
public class LoopingDuplicationTaskProducerTest extends EasyMockSupport {

    private static final String CACHE_NAME = "test";

    @Mock
    private CredentialsRepo credentialsRepo;
    @Mock
    private StorageProviderFactory storageProviderFactory;
    @Mock
    private StorageProvider sourceStore;
    @Mock
    private StorageProvider destStore;
    private static Cache cache;
    @Mock
    private DuplicationPolicyManager policyManager;
    @Mock
    private StateManager<DuplicationMorsel> stateManager;

    @Mock
    private NotificationManager notificationManager;

    private TaskQueue taskQueue;

    private StorageProviderCredentials sourceCreds;
    private StorageProviderCredentials destCreds;

    @Mock
    private LoopingTaskProducerConfigurationManager config;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        taskQueue = new LocalTaskQueue();
        sourceCreds = new StorageProviderCredentials();
        sourceCreds.setPrimary(true);
        sourceCreds.setProviderId("0");
        sourceCreds.setAccessKey("access-key");
        sourceCreds.setSecretKey("password-key");

        destCreds = new StorageProviderCredentials();
        destCreds.setPrimary(false);
        destCreds.setProviderId("1");
        destCreds.setAccessKey("access-key");
        destCreds.setSecretKey("password-key");

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
        cache.removeAll();
    }

    /**
     * Test method for {@link org.duracloud.mill.ltp.LoopingTaskProducer#run()}.
     *
     * @throws CredentialsRepoException
     */
    @Test
    public void testRun() throws CredentialsRepoException, ParseException {

        int morselCount = 2;
        int sourceCount = 2000;
        int destCount = 100;

        setupSourceStore(morselCount, sourceCount);
        setupDestStore(morselCount, destCount);
        setupStorageProviderFactory(morselCount * 2);
        setupCredentialsRepo(4 * morselCount);
        setupPolicyManager(morselCount);
        setupStateManager(morselCount, sourceCount);
        setupDatesOnRunCompletion();
        setupLoopingTaskProducerConfig(1);
        setupNotificationManager();
        setupCache();
        int maxTaskQueueSize = calculateMaxQueueSize(morselCount, sourceCount, destCount);
        replayAll();
        runLoopingTaskProducer(maxTaskQueueSize + 1);
        Assert.assertEquals(maxTaskQueueSize, taskQueue.size().intValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNonExistentSpace() throws CredentialsRepoException, ParseException {
        int morselCount = 1;
        expectNotFoundOnGetSpaceContentsChunked(sourceStore);

        expectNotFoundOnGetSpaceContents(sourceStore);

        expectEmptyListFromGetSpaceContents(destStore, morselCount);

        setupGetSpaces(morselCount);
        setupStorageProviderFactory(morselCount);
        setupCredentialsRepo(morselCount * 2);
        setupPolicyManager(morselCount);
        setupNotificationManager();

        expectGetMorsels(new LinkedHashSet<DuplicationMorsel>(), 1);
        setupLoopingTaskProducerConfig(1);

        stateManager.setMorsels(EasyMock.isA(LinkedHashSet.class));
        EasyMock.expectLastCall().times(morselCount);

        setupCheckDatesFirstTimeRun();
        setupDatesOnRunCompletion();

        setupCache();
        int maxQueueSize = 1;
        replayAll();

        runLoopingTaskProducer(maxQueueSize);
        Assert.assertEquals(maxQueueSize, taskQueue.size().intValue());
    }

    private void setupLoopingTaskProducerConfig(int times) {
        expect(this.config.getWorkDirectoryPath()).andReturn("java.io.tmpdir").times(times);
    }

    private void setupNotificationManager() {
        notificationManager.sendEmail(isA(String.class), isA(String.class));
        expectLastCall().once();
    }

    /**
     *
     */
    private void setupDatesOnRunCompletion() {
        EasyMock.expect(stateManager.getCurrentRunStartDate()).andReturn(new Date());
        stateManager.setNextRunStartDate(EasyMock.isA(Date.class));
        EasyMock.expectLastCall().once();
        stateManager.setCurrentRunStartDate(null);
        EasyMock.expectLastCall().once();
    }

    /**
     * Tests pathway with no deletes.
     *
     * @throws Exception
     */
    @Test
    public void testSpaceLargerThanMaxQueueSizeNoDeletes() throws Exception {
        testSpaceLargerThanMaxQueueSize(1500, 0);
    }

    /**
     * Tests pathway with deletes.
     *
     * @throws Exception
     */
    @Test
    public void testSpaceLargerThanMaxQueueSizeWithDeletes() throws Exception {
        testSpaceLargerThanMaxQueueSize(1500, 100);
    }

    /**
     * This test ensures that all items in a space are duplicated only once within a run
     * when multiple looping task producer "sessions" are required.  A "session" is a single
     * java process lifecycle. Multiple sessions for a run occur when a max task queue limit is
     * reached before all the morsels can be consumed.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void testSpaceLargerThanMaxQueueSize(int sourceCount, int destCount) throws Exception {
        int morselCount = 1;

        setupSourceStore(morselCount, sourceCount);

        final LinkedHashSet<DuplicationMorsel> morsels = new LinkedHashSet<>();

        expectGetMorsels(new LinkedHashSet<DuplicationMorsel>(), 1);
        expectGetMorsels(morsels, 1);
        setupLoopingTaskProducerConfig(1);
        stateManager.setMorsels(EasyMock.isA(LinkedHashSet.class));
        StateManager<DuplicationMorsel> stateManagerDelegate =
            new StateManager<DuplicationMorsel>("fakepath", DuplicationMorsel.class) {
                @Override
                public void setMorsels(LinkedHashSet<DuplicationMorsel> morsels2) {
                    morsels.clear();
                    morsels.addAll(morsels2);
                }
            };
        EasyMock.expectLastCall().andDelegateTo(stateManagerDelegate).times(2);

        setupDestStore(1, destCount);
        setupStorageProviderFactory(2);

        setupCredentialsRepo(6);
        setupPolicyManager(morselCount);
        setupCheckDatesFirstTimeRun();
        setupCheckDatesInProgressRun();
        setupDatesOnRunCompletion();
        setupCache();
        setupNotificationManager();
        int maxTaskQueueSize = 1000;

        replayAll();

        int tasksProcessed = 0;
        runLoopingTaskProducer(maxTaskQueueSize);
        tasksProcessed = drainQueue(tasksProcessed);
        runLoopingTaskProducer(maxTaskQueueSize);
        tasksProcessed = drainQueue(tasksProcessed);

        Assert.assertEquals(sourceCount + destCount, tasksProcessed);
        Assert.assertEquals(0, morsels.size());

    }

    /**
     *
     */
    private void setupEmptySourceSpacesGetChunked() {
        EasyMock.expect(sourceStore.getSpaceContentsChunked(EasyMock.isA(String.class),
                                                            EasyMock.isNull(String.class),
                                                            EasyMock.anyInt(),
                                                            EasyMock.isA(String.class)))
                .andReturn(new LinkedList<String>());
    }

    /**
     * @param tasksProcessed
     * @return
     */
    private int drainQueue(int tasksProcessed) {
        while (taskQueue.size() > 0) {
            try {
                taskQueue.take();
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

        LoopingDuplicationTaskProducer producer =
            new LoopingDuplicationTaskProducer(credentialsRepo,
                                               storageProviderFactory,
                                               policyManager,
                                               taskQueue,
                                               cache,
                                               stateManager,
                                               maxQueueSize,
                                               new Frequency("1d"),
                                               notificationManager,
                                               config);
        producer.run();
    }

    /**
     * @param times
     */
    private void expectGetMorsels(LinkedHashSet<DuplicationMorsel> set, int times) {
        EasyMock.expect(stateManager.getMorsels()).andReturn(set).times(times);
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

    private void expectNotFoundOnGetSpaceContents(StorageProvider store) {
        EasyMock.expect(store.getSpaceContents(EasyMock.isA(String.class), EasyMock.isNull(String.class)))
                .andThrow(new NotFoundException("test"));
    }

    private void expectNotFoundOnGetSpaceContentsChunked(StorageProvider store) {
        EasyMock.expect(store.getSpaceContentsChunked(EasyMock.isA(String.class),
                                                      EasyMock.isNull(String.class),
                                                      EasyMock.anyInt(),
                                                      EasyMock.isNull(String.class)))
                .andThrow(new NotFoundException("test"));
    }

    /**
     * @param morselCount
     * @param sourceCount
     * @param destCount
     * @return
     */
    private int calculateMaxQueueSize(int morselCount,
                                      int sourceCount,
                                      int destCount) {
        return (morselCount * sourceCount) + (morselCount * destCount);
    }

    /**
     * @param morsel
     * @throws CredentialsRepoException
     */
    private void setupCredentialsRepo(int morsel)
        throws CredentialsRepoException {

        EasyMock.expect(credentialsRepo.isAccountActive(EasyMock.isA(String.class)))
                .andReturn(true).atLeastOnce();

        EasyMock.expect(credentialsRepo.getStorageProviderCredentials(EasyMock.isA(String.class),
                                                                      EasyMock.eq("0")))
                .andReturn(sourceCreds).atLeastOnce();

        EasyMock.expect(credentialsRepo.getStorageProviderCredentials(EasyMock.isA(String.class),
                                                                      EasyMock.eq("1")))
                .andReturn(destCreds).atLeastOnce();

        AccountCredentials accountCredentials =
            new AccountCredentials("subDomainA", Arrays.asList(sourceCreds, destCreds));
        EasyMock.expect(credentialsRepo.getAccountCredentials(EasyMock.isA(String.class)))
                .andReturn(accountCredentials).atLeastOnce();

    }

    /**
     * @param rounds
     */
    private void setupStorageProviderFactory(int rounds) {
        EasyMock.expect(storageProviderFactory.create(EasyMock.eq(sourceCreds)))
                .andReturn(sourceStore);

        if (rounds > 0) {
            EasyMock.expect(storageProviderFactory.create(EasyMock.eq(sourceCreds)))
                    .andReturn(sourceStore).times(rounds);

            EasyMock.expect(storageProviderFactory.create(EasyMock.eq(destCreds)))
                    .andReturn(destStore).times(rounds);

        }
    }

    /**
     * @param times
     * @param destCount
     */
    private void setupDestStore(int times, int destCount) {
        final List<String> destContentItems = new LinkedList<>();

        for (int i = 0; i < destCount; i++) {
            destContentItems.add("tobedeleted" + i);
        }

        EasyMock.expect(
            destStore.getSpaceContents(EasyMock.isA(String.class),
                                       EasyMock.isNull(String.class)))
                .andAnswer(new IAnswer<Iterator<String>>() {
                    /*
                     * (non-Javadoc)
                     *
                     * @see org.easymock.IAnswer#answer()
                     */
                    @Override
                    public Iterator<String> answer() throws Throwable {
                        return destContentItems.iterator();
                    }
                }).times(times);
    }

    private void setupGetSpaces(int morselCount) {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < morselCount; i++) {
            list.add("testspace" + i);
        }

        expect(sourceStore.getSpaces()).andReturn(list.iterator());
    }

    private void setupSourceStore(int morselCount, int sourceCount) {
        setupGetSpaces(morselCount);

        final List<String> sourceContentItems = new LinkedList<>();

        for (int i = 0; i < sourceCount; i++) {
            sourceContentItems.add("item" + i);
        }

        EasyMock.expect(sourceStore.getSpaceContentsChunked(EasyMock.isA(String.class),
                                                            EasyMock.isNull(String.class),
                                                            EasyMock.anyInt(),
                                                            EasyMock.isNull(String.class)))
                .andReturn(sourceContentItems).times(morselCount);

        EasyMock.expect(
            sourceStore.getSpaceContentsChunked(
                EasyMock.isA(String.class),
                EasyMock.isNull(String.class),
                EasyMock.anyInt(),
                EasyMock.isA(String.class)))
                .andReturn(new ArrayList<String>()).times(morselCount);

        EasyMock.expect(sourceStore.getSpaceContents(EasyMock.isA(String.class),
                                                     EasyMock.isNull(String.class)))
                .andAnswer(new IAnswer<Iterator<String>>() {
                    /* (non-Javadoc)
                     * @see org.easymock.IAnswer#answer()
                     */
                    @Override
                    public Iterator<String> answer() throws Throwable {
                        return sourceContentItems.iterator();
                    }
                }).times(morselCount);
    }

    /**
     *
     */
    private void setupCache() {
        if (cache == null) {
            CacheManager cacheManager = CacheManager.getInstance();
            cacheManager.addCache(CACHE_NAME);
            cache = cacheManager.getCache(CACHE_NAME);
        }
    }

    /**
     * @param morselCount
     * @param sourceCount
     */
    private void setupStateManager(int morselCount, int sourceCount) {
        expectGetMorsels(new LinkedHashSet<DuplicationMorsel>(), 1);
        stateManager.setMorsels(EasyMock.isA(LinkedHashSet.class));
        EasyMock.expectLastCall().times(morselCount * sourceCount / 1000);
        setupCheckDatesFirstTimeRun();
    }

    /**
     *
     */
    private void setupCheckDatesFirstTimeRun() {
        EasyMock.expect(stateManager.getNextRunStartDate()).andReturn(null);
        EasyMock.expect(stateManager.getCurrentRunStartDate()).andReturn(null);
        stateManager.setCurrentRunStartDate(EasyMock.isA(Date.class));
        EasyMock.expectLastCall().once();
    }

    private void setupCheckDatesInProgressRun() {
        EasyMock.expect(stateManager.getNextRunStartDate()).andReturn(null);
        EasyMock.expect(stateManager.getCurrentRunStartDate()).andReturn(new Date());
    }

    /**
     *
     */
    private void setupPolicyManager(int morselCount) {
        Set<String> accounts = new HashSet<>();
        accounts.add("subdomainA");
        EasyMock.expect(policyManager.getDuplicationAccounts()).andReturn(accounts);
        DuplicationPolicy policy = new DuplicationPolicy();
        DuplicationStorePolicy dupStore = new DuplicationStorePolicy();
        dupStore.setSrcStoreId(sourceCreds.getProviderId());
        dupStore.setDestStoreId(destCreds.getProviderId());

        for (int i = 0; i < morselCount; i++) {
            policy.addDuplicationStorePolicy("testspace" + i, dupStore);
        }

        EasyMock.expect(policyManager.getDuplicationPolicy("subdomainA")).andReturn(policy);

    }
}
