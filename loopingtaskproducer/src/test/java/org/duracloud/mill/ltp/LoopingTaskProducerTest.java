/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.duracloud.storage.error.NotFoundException;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.local.LocalTaskQueue;
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
public class LoopingTaskProducerTest {

    private CredentialsRepo credentialsRepo;
    private StorageProviderFactory storageProviderFactory;
    private StorageProvider sourceStore;
    private StorageProvider destStore;
    private static Cache cache;
    private DuplicationPolicyManager policyManager;
    private StateManager stateManager;
    private TaskQueue taskQueue;

    /**
     * @throws java.lang.Exception
     */
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
        LoopingTaskProducer producer = new LoopingTaskProducer(credentialsRepo, 
                                                               storageProviderFactory, 
                                                               policyManager,
                                                               taskQueue, 
                                                               cache, 
                                                               stateManager, 
                                                               maxTaskQueueSize,
                                                               new Frequency("1d"));
        producer.run();
        Assert.assertEquals(maxTaskQueueSize, taskQueue.size().intValue());
    }
    
    @Test
    public void testNonExistentSpace() throws CredentialsRepoException, ParseException {
        
        int morselCount = 1;

        

        EasyMock.expect(sourceStore.getSpaceContentsChunked(EasyMock.isA(String.class),
                                            EasyMock.isNull(String.class), 
                                            EasyMock.anyInt(),
                                            EasyMock.isNull(String.class)))
                .andThrow(new NotFoundException("test"));

        EasyMock.expect(sourceStore.getSpaceContents(EasyMock.isA(String.class),
                                            EasyMock.isNull(String.class)
                                            ))
                .andThrow(new NotFoundException("test"));

        EasyMock.expect(destStore.getSpaceContents(EasyMock.isA(String.class), EasyMock.isNull(String.class)))
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
        
        setupStorageProviderFactory(morselCount);
        setupCredentialsRepo(morselCount*2);
        setupPolicyManager(morselCount);
        
        EasyMock.expect(stateManager.getMorsels()).andReturn(new HashSet<Morsel>());
        stateManager.setMorsels(EasyMock.isA(HashSet.class));
        EasyMock.expectLastCall().times(morselCount);
        EasyMock.expect(stateManager.getNextRunStartDate()).andReturn(null);
        EasyMock.expect(stateManager.getCurrentRunStartDate()).andReturn(null);
        EasyMock.expect(stateManager.getCurrentRunStartDate()).andReturn(new Date());
        
        stateManager.setCurrentRunStartDate(EasyMock.isA(Date.class));
        EasyMock.expectLastCall();

        stateManager.setCurrentRunStartDate(null);
        EasyMock.expectLastCall();

        stateManager.setNextRunStartDate(EasyMock.isA(Date.class));
        EasyMock.expectLastCall();
        
        setupCache();
        replay();
        LoopingTaskProducer producer = new LoopingTaskProducer(credentialsRepo, 
                                                               storageProviderFactory, 
                                                               policyManager,
                                                               taskQueue, 
                                                               cache, 
                                                               stateManager, 
                                                               1,
                                                               new Frequency("1d"));
        producer.run();
        Assert.assertEquals(1, taskQueue.size().intValue());
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
     * @param morselCount
     */
    private void setupStorageProviderFactory(int morselCount) {
        for(int i = 0; i < (morselCount); i++){
            EasyMock.expect(storageProviderFactory.create(EasyMock.isA(StorageProviderCredentials.class)))
            .andReturn(sourceStore)
            .andReturn(destStore);
        }
    }

    /**
     * @param morselCount
     * @param destCount
     */
    private void setupDestStore(int morselCount, int destCount) {
        final List<String> destContentItems = new LinkedList<>();

        for(int i = 0; i < destCount; i++){
            destContentItems.add("tobedeleted"+i);
        }


        EasyMock.expect(destStore.getSpaceContents(EasyMock.isA(String.class), EasyMock.isNull(String.class)))
                .andAnswer(new IAnswer<Iterator<String>>() {
                    /* (non-Javadoc)
                     * @see org.easymock.IAnswer#answer()
                     */
                    @Override
                    public Iterator<String> answer() throws Throwable {
                      return destContentItems.iterator();
                    }
                  })
                .times(morselCount);
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
                .andReturn(sourceContentItems.subList(0, 1000)).times(morselCount);

        EasyMock.expect(
                sourceStore.getSpaceContentsChunked(
                        EasyMock.isA(String.class),
                        EasyMock.isNull(String.class), 
                        EasyMock.anyInt(),
                        EasyMock.isA(String.class)))
                .andReturn(sourceContentItems.subList(1000, sourceCount)).times(morselCount);

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
        EasyMock.expect(stateManager.getMorsels()).andReturn(new HashSet<Morsel>());
        stateManager.setMorsels(EasyMock.isA(HashSet.class));
        EasyMock.expectLastCall().times(morselCount*sourceCount/1000);
        EasyMock.expect(stateManager.getNextRunStartDate()).andReturn(null);
        EasyMock.expect(stateManager.getCurrentRunStartDate()).andReturn(null);
        stateManager.setCurrentRunStartDate(EasyMock.isA(Date.class));
        EasyMock.expectLastCall();
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
