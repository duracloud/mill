/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.domain.DuplicationTask;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorCreationFailedException;
import org.duracloud.mill.workman.TaskProcessorFactoryBase;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;


/**
 * This class is responsible for creating DuplicationTaskProcessors 
 * 
 * @author Daniel Bernstein
 * 
 */
public class DuplicationTaskProcessorFactory extends TaskProcessorFactoryBase {
    private StorageProviderFactory storageProviderFactory;

    public DuplicationTaskProcessorFactory(CredentialsRepo repo){
        super(repo);
        this.storageProviderFactory = new StorageProviderFactory();
    }
    
    @Override
    protected boolean isSupported(Task task) {
        return task.getType().equals(Task.Type.DUP);
    }

    @Override
    protected TaskProcessor createImpl(Task task) throws TaskProcessorCreationFailedException {

        DuplicationTask dtask = new DuplicationTask();
        dtask.readTask(task);
        String subdomain = dtask.getAccount();

        try {
            StorageProvider sourceStore = createStorageProvider(
                    dtask.getSourceStoreId(), subdomain);
            StorageProvider destStore = createStorageProvider(
                    dtask.getDestStoreId(), subdomain);
            return new DuplicationTaskProcessor(task, sourceStore, destStore);
        } catch (Exception e) {
            throw new TaskProcessorCreationFailedException(
                    "failed to create task: unable to locate credentials for subdomain: "
                            + subdomain);
        }
    }

    /**
     * @param sourceStoreId
     * @param account
     * @return
     */
    public StorageProvider createStorageProvider(String providerId,
            String subdomain) {
        
        StorageProviderCredentials c;
        try {
            c = getCredentialRepo().getStorageProviderCredentials(subdomain,
                    providerId);

            return this.storageProviderFactory.create(c);
        } catch (CredentialsRepoException e) {
            throw new RuntimeException(e);
        }
    }
}
