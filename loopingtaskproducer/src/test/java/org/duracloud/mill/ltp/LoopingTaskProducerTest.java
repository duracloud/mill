/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.duracloud.mill.ltp.LoopingTaskProducer#run()}.
     * @throws CredentialsRepoException 
     */
    @Test
    public void testRun() throws CredentialsRepoException {
        
        int morselCount = 2;
        int sourceCount = 2000;
        int destCount = 100;

        CredentialsRepo credentialsRepo = 
                EasyMock.createMock(CredentialsRepo.class);

        StorageProviderFactory storageProviderFactory = 
                EasyMock.createMock(StorageProviderFactory.class);

        StorageProvider sourceStore = 
                EasyMock.createMock("sourceStore", StorageProvider.class);
        
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
        
        final List<String> destContentItems = new LinkedList<>();

        for(int i = 0; i < destCount; i++){
            destContentItems.add("tobedeleted"+i);
        }

        StorageProvider destStore = EasyMock.createMock("destStore", StorageProvider.class);

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

        for(int i = 0; i < (morselCount*2); i++){
            EasyMock.expect(storageProviderFactory.create(EasyMock.isA(StorageProviderCredentials.class)))
            .andReturn(sourceStore)
            .andReturn(destStore);
        }

        
        EasyMock.expect(
                credentialsRepo.getStorageProviderCredentials(
                        EasyMock.isA(String.class), EasyMock.isA(String.class)))
                .andReturn(new StorageProviderCredentials()).times(4*morselCount);
        
        DuplicationPolicyManager policyManager = EasyMock.createMock(DuplicationPolicyManager.class);
        Set<String> accounts = new HashSet<>();
        accounts.add("subdomainA");
        
        EasyMock.expect(policyManager.getDuplicationAccounts()).andReturn(accounts);
        
        StateManager stateManager = EasyMock.createMock(StateManager.class);
        EasyMock.expect(stateManager.getMorsels()).andReturn(new HashSet<Morsel>());
        stateManager.setMorsels(EasyMock.isA(HashSet.class));
        EasyMock.expectLastCall().times(morselCount*sourceCount/1000);
        
        DuplicationPolicy policy = new DuplicationPolicy();
        DuplicationStorePolicy dupStore = new DuplicationStorePolicy();
        dupStore.setSrcStoreId("0");
        dupStore.setDestStoreId("1");
        
        for(int i = 0; i < morselCount; i++){
            policy.addDuplicationStorePolicy("testspace"+i, dupStore);
        }

        EasyMock.expect(policyManager.getDuplicationPolicy("subdomainA")).andReturn(policy);
        TaskQueue taskQueue = new LocalTaskQueue();
        
        
        Cache cache = new Cache("test", 100000, true, true, 1000, 1000);
        CacheManager manager = new CacheManager();
        manager.addCache(cache);
        
        int maxTaskQueueSize = (morselCount*sourceCount)+(morselCount*destCount);
        
        
        
        EasyMock.replay(credentialsRepo, 
                        storageProviderFactory, 
                        policyManager,
                        stateManager,
                        sourceStore,
                        destStore);
        
        LoopingTaskProducer producer = new LoopingTaskProducer(credentialsRepo, 
                                                               storageProviderFactory, 
                                                               policyManager,
                                                               taskQueue, 
                                                               cache, 
                                                               stateManager, 
                                                               maxTaskQueueSize);
        producer.run();
        
        
        Assert.assertEquals(maxTaskQueueSize, taskQueue.size().intValue());

        EasyMock.verify(credentialsRepo, 
                storageProviderFactory, 
                policyManager,
                stateManager,
                sourceStore,
                destStore);

    }

}
