/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialRepo;
import org.duracloud.mill.credentials.ProviderCredentials;
import org.duracloud.mill.domain.DuplicationTask;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.domain.StorageProviderType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Bernstein Date: Oct 25, 2013
 */
public class DuplicationTaskProcessorFactoryTest {

    @Test
    public void test() {
        List<StorageProviderType[]> p = new LinkedList<>();
        p.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.AMAZON_GLACIER });
        p.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.SDSC });
        p.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.RACKSPACE });
        p.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.RACKSPACE });
        
        //successes
        for(StorageProviderType[] a : p){
            testDuplicationTaskProcessorFactory(a[0], a[1], true);
        }
        
        //expected failures
        List<StorageProviderType[]> f = new LinkedList<>();
        f.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.EMC });
        f.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.HP });
        f.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.IRODS });
        f.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.MICROSOFT_AZURE });
        f.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.UNKNOWN });
        for(StorageProviderType[] a : f){
            testDuplicationTaskProcessorFactory(a[0], a[1], false);
        }
 
    }
    
    private void testDuplicationTaskProcessorFactory(StorageProviderType source,
            StorageProviderType destination, boolean expectedOutcome) {

        CredentialRepo repo = EasyMock.createMock(CredentialRepo.class);

        AccountCredentials a = new AccountCredentials();
        a.setSProviderCredentials(Arrays.asList(new ProviderCredentials[] {
                new ProviderCredentials("0", "test", "test",
                        source),
                new ProviderCredentials("1", "test", "test",
                        destination)}));

        EasyMock.expect(repo.getAccoundCredentials(EasyMock.isA(String.class)))
                .andReturn(a);
        EasyMock.replay(repo);

        DuplicationTaskProcessorFactory f = new DuplicationTaskProcessorFactory(
                repo);

        DuplicationTask dupTask = new DuplicationTask();
        dupTask.setAccount("account");
        dupTask.setSourceStoreId("0");
        dupTask.setDestStoreId("1");

        Task task = dupTask.writeTask();
        TaskProcessor p = null;
        
        try { 
            p = f.create(task);
            if(!expectedOutcome){
                Assert.assertTrue(false);
            }
        }catch(Exception ex){
            Assert.assertFalse(expectedOutcome);
        }

        if(expectedOutcome){
            Assert.assertNotNull(p);
            Assert.assertEquals(DuplicationTaskProcessor.class, p.getClass());
        }

        EasyMock.verify(repo);
    }

}
