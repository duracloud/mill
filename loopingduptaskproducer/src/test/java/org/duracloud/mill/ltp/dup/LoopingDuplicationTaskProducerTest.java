/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.duracloud.common.queue.local.LocalTaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *	       Date: Nov 6, 2013
 */
public class LoopingDuplicationTaskProducerTest {

    private CredentialsRepo credentialsRepo;
    private StorageProviderFactory storageProviderFactory;
    private StorageProvider sourceStore;
    private StorageProvider destStore;
    private static Cache cache;
    private DuplicationPolicyManager policyManager;
    private StateManager<DuplicationMorsel> stateManager;
    private TaskQueue taskQueue;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        credentialsRepo = EasyMock.createMock(CredentialsRepo.class);
        storageProviderFactory = EasyMock.createMock(StorageProviderFactory.class);
        sourceStore = EasyMock.createMock("sourceStore", StorageProvider.class);
        destStore = EasyMock.createMock("destStore", StorageProvider.class);
        policyManager = EasyMock.createMock(DuplicationPolicyManager.class);
        stateManager = EasyMock.createMock(StateManager.class);
        taskQueue = new LocalTaskQueue();

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        EasyMock.verify(credentialsRepo, 
                storageProviderFactory, 
                policyManager,
                stateManager,
                sourceStore,
                destStore);
    }
    
    /**
     * 
     */
    private void replay() {
        EasyMock.replay(credentialsRepo, 
                        storageProviderFactory, 
                        policyManager,
                        stateManager,
                        sourceStore,
                        destStore);
    }

    /**
     * Test method for {@link org.duracloud.mill.ltp.LoopingTaskProducer#run()}.
     * @throws CredentialsRepoException 
     */
    @Test
    public void testRun() throws CredentialsRepoException, ParseException {
        
        int morselCount = 2;
        int sourceCount = 2000;
        int destCount = 100;

        setupSourceStore(morselCount, sourceCount);
        setupDestStore(morselCount, destCount);
        setupStorageProviderFactory(morselCount*2);
        setupCredentialsRepo(4*morselCount);
        setupPolicyManager(morselCount);
        setupStateManager(morselCount, sourceCount);
        setupCache();
        int maxTaskQueueSize = calculateMaxQueueSize(morselCount, sourceCount, destCount);
        replay();
        runLoopingTaskProducer(maxTaskQueueSize);
        Assert.assertEquals(maxTaskQueueSize, taskQueue.size().intValue());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testNonExistentSpace() throws CredentialsRepoException, ParseException {
        int morselCount = 1;
        expectNotFoundOnGetSpaceContentsChunked(sourceStore);

        expectNotFoundOnGetSpaceContents(sourceStore);

        expectEmptyListFromGetSpaceContents(destStore,morselCount);
        
        setupStorageProviderFactory(morselCount);
        setupCredentialsRepo(morselCount*2);
        setupPolicyManager(morselCount);
        
        expectGetMorsels(new HashSet<DuplicationMorsel>(),1);

        stateManager.setMorsels(EasyMock.isA(HashSet.class));
        EasyMock.expectLastCall().times(morselCount);
        
        setupCheckDatesFirstTimeRun();
        setupDatesOnRunCompletion();

        
        setupCache();
        int maxQueueSize = 1;
        replay();

        runLoopingTaskProducer(maxQueueSize);
        Assert.assertEquals(maxQueueSize, taskQueue.size().intValue());
    }
    
    /**
     * 
     */
    private void setupDatesOnRunCompletion() {
        EasyMock.expect(stateManager.getCurrentRunStartDate()).andReturn(new Date());
        stateManager.setNextRunStartDate(EasyMock.isA(Date.class));
        EasyMock.expectLastCall();
        stateManager.setCurrentRunStartDate(null);
        EasyMock.expectLastCall();
    }
    

    
    /**
     * Tests pathway with no deletes.
     * @throws Exception
     */
    @Test
    public void testSpaceLargerThanMaxQueueSizeNoDeletes() throws Exception{
        testSpaceLargerThanMaxQueueSize(1500, 0);
    }

    /**
     * Tests pathway with deletes.
     * @throws Exception
     */
    @Test
    public void testSpaceLargerThanMaxQueueSizeWithDeletes() throws Exception{
        testSpaceLargerThanMaxQueueSize(500,1000);
    }

    /**
     * This test ensures that all items in a space are duplicated only once within a run
     * when multiple looping task producer "sessions" are required.  A "session" is a single
     * java process lifecycle. Multiple sessions for a run occur when a max task queue limit is
     * reached before all the morsels can be consumed. 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void testSpaceLargerThanMaxQueueSize(int sourceCount, int destCount) throws Exception{
        int morselCount = 1;

        setupSourceStore(morselCount, sourceCount);

        final HashSet<DuplicationMorsel> morsels = new HashSet<>();
        
        setupEmptySourceSpacesGetChunked();
        
        expectGetMorsels(new HashSet<DuplicationMorsel>(), 1);
        expectGetMorsels(morsels, 1);
        
        stateManager.setMorsels(EasyMock.isA(HashSet.class));
        StateManager<DuplicationMorsel> stateManagerDelegate = new StateManager<DuplicationMorsel>("fakepath") {
            @Override
            public void setMorsels(Set<DuplicationMorsel> morsels2) {
                morsels.clear();
                morsels.addAll(morsels2);
            }
        };
        EasyMock.expectLastCall().andDelegateTo(stateManagerDelegate).times(3);
        
        setupDestStore(1, destCount);
        setupStorageProviderFactory(3);
        setupCredentialsRepo(6);
        setupPolicyManager(morselCount);
        setupCheckDatesFirstTimeRun();
        setupCheckDatesInProgressRun();
        setupDatesOnRunCompletion();
        setupCache();
        int maxTaskQueueSize = 1000;

        replay();
        
        int tasksProcessed = 0;
        runLoopingTaskProducer(maxTaskQueueSize);
        tasksProcessed = drainQueue(tasksProcessed);
        runLoopingTaskProducer(maxTaskQueueSize);
        tasksProcessed = drainQueue(tasksProcessed);

        Assert.assertEquals(sourceCount+destCount,tasksProcessed);
        Assert.assertEquals(0, morsels.size());

    }

    /**
     * 
     */
    private void setupEmptySourceSpacesGetChunked() {
        EasyMock.expect(
                sourceStore.getSpaceContentsChunked(
                        EasyMock.isA(String.class),
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
        while(taskQueue.size() > 0){
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

        LoopingDuplicationTaskProducer producer = new LoopingDuplicationTaskProducer(credentialsRepo, 
                                                               storageProviderFactory, 
                                                               policyManager,
                                                               taskQueue, 
                                                               cache, 
                                                               stateManager, 
                                                               maxQueueSize,
                                                               new Frequency("1d"));
        producer.run();
    }

    /**
     * @param times 
     * 
     */
    private void expectGetMorsels(HashSet<DuplicationMorsel> set, int times) {
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
        EasyMock.expect(store.getSpaceContents(EasyMock.isA(String.class),
                                            EasyMock.isNull(String.class)
                                            ))
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
        return (morselCount*sourceCount)+(morselCount*destCount);
    }

    /**
     * @param morselCount
     * @throws CredentialsRepoException
     */
    private void setupCredentialsRepo(int count)
            throws CredentialsRepoException {
        EasyMock.expect(
                credentialsRepo.getStorageProviderCredentials(
                        EasyMock.isA(String.class), EasyMock.isA(String.class)))
                .andReturn(new StorageProviderCredentials()).times(count);
    }

    /**
     * @param rounds
     */
    private void setupStorageProviderFactory(int rounds) {
        for(int i = 0; i < (rounds); i++){
            EasyMock.expect(storageProviderFactory.create(EasyMock.isA(StorageProviderCredentials.class)))
            .andReturn(sourceStore)
            .andReturn(destStore);
        }
    }

    /**
     * @param times
     * @param destCount
     */
    private void setupDestStore(int times, int destCount) {
        final List<String> destContentItems = new LinkedList<>();

        for(int i = 0; i < destCount; i++){
            destContentItems.add("tobedeleted"+i);
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

    /**
     * @param morselCount
     * @param sourceCount
     */
    private void setupSourceStore(int morselCount, int sourceCount) {
        final List<String> sourceContentItems = new LinkedList<>();

        for(int i = 0; i < sourceCount; i++){
            sourceContentItems.add("item"+i);
        }

        
        EasyMock.expect(sourceStore.getSpaceContentsChunked(EasyMock.isA(String.class),
                                            EasyMock.isNull(String.class), 
                                            EasyMock.anyInt(),
                                            EasyMock.isNull(String.class)))
                .andReturn(sourceContentItems.subList(0, Math.min(1000,sourceCount))).times(morselCount);

        if(sourceCount > 1000){
        EasyMock.expect(
                sourceStore.getSpaceContentsChunked(
                        EasyMock.isA(String.class),
                        EasyMock.isNull(String.class), 
                        EasyMock.anyInt(),
                        EasyMock.isA(String.class)))
                .andReturn(sourceContentItems.subList(1000, sourceCount)).times(morselCount);
        }
        
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
        if(cache == null){
            cache = new Cache("test", 100000, true, true, 1000, 1000);
            CacheManager manager = CacheManager.getInstance();
            manager.addCache(cache);
        }
    }

    /**
     * @param morselCount
     * @param sourceCount
     */
    private void setupStateManager(int morselCount, int sourceCount) {
        expectGetMorsels(new HashSet<DuplicationMorsel>(),1);
        stateManager.setMorsels(EasyMock.isA(HashSet.class));
        EasyMock.expectLastCall().times(morselCount*sourceCount/1000);
        setupCheckDatesFirstTimeRun();
    }

    /**
     * 
     */
    private void setupCheckDatesFirstTimeRun() {
        EasyMock.expect(stateManager.getNextRunStartDate()).andReturn(null);
        EasyMock.expect(stateManager.getCurrentRunStartDate()).andReturn(null);
        stateManager.setCurrentRunStartDate(EasyMock.isA(Date.class));
        EasyMock.expectLastCall();
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
        dupStore.setSrcStoreId("0");
        dupStore.setDestStoreId("1");
        
        for(int i = 0; i < morselCount; i++){
            policy.addDuplicationStorePolicy("testspace"+i, dupStore);
        }

        EasyMock.expect(policyManager.getDuplicationPolicy("subdomainA")).andReturn(policy);

    }
}
