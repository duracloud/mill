/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.domain.DuplicationTask;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.domain.StorageProviderType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Daniel Bernstein Date: Oct 25, 2013
 */
public class DuplicationTaskProcessorFactoryTest {

    @Test
    public void test() throws CredentialsRepoException {
        List<StorageProviderType[]> successfulProviderList = new LinkedList<>();
        successfulProviderList.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.AMAZON_GLACIER });
        successfulProviderList.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.SDSC });
        successfulProviderList.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.RACKSPACE });
        successfulProviderList.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.RACKSPACE });
        
        //successes
        for(StorageProviderType[] a : successfulProviderList){
            testDuplicationTaskProcessorFactory(a[0], a[1], true);
        }
        
        //expected failures
        List<StorageProviderType[]> failedProviderList = new LinkedList<>();
        failedProviderList.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.IRODS });
        failedProviderList.add(new StorageProviderType[] { StorageProviderType.AMAZON_S3,
                StorageProviderType.UNKNOWN });
        for(StorageProviderType[] a : failedProviderList){
            testDuplicationTaskProcessorFactory(a[0], a[1], false);
        }
 
    }
    
    private void testDuplicationTaskProcessorFactory(
            StorageProviderType source, StorageProviderType destination,
            boolean expectedOutcome) throws CredentialsRepoException {

        CredentialsRepo repo = EasyMock.createMock(CredentialsRepo.class);

        AccountCredentials a = new AccountCredentials();
        a.setSProviderCredentials(Arrays.asList(new StorageProviderCredentials[] {
                new StorageProviderCredentials("0", "test", "test", source),
                new StorageProviderCredentials("1", "test", "test", destination) }));

        EasyMock.expect(
                repo.getStorageProviderCredentials(EasyMock.isA(String.class),
                        EasyMock.isA(String.class)))
                .andReturn(
                        new StorageProviderCredentials("0", "test", "test",
                                source))
                .andReturn(
                        new StorageProviderCredentials("1", "test", "test",
                                destination));

        EasyMock.replay(repo);

        DuplicationTaskProcessorFactory f =
            new DuplicationTaskProcessorFactory(repo, new File("workdir"));

        DuplicationTask dupTask = new DuplicationTask();
        dupTask.setAccount("account");
        dupTask.setSourceStoreId("0");
        dupTask.setDestStoreId("1");

        Task task = dupTask.writeTask();
        TaskProcessor processor = null;

        try {
            processor = f.create(task);
            if (!expectedOutcome) {
                Assert.assertTrue(false);
            }
        } catch (Exception ex) {
            Assert.assertFalse(expectedOutcome);
        }

        if (expectedOutcome) {
            Assert.assertNotNull(processor);
            Assert.assertEquals(DuplicationTaskProcessor.class,
                    processor.getClass());
        }

        EasyMock.verify(repo);
    }

}
