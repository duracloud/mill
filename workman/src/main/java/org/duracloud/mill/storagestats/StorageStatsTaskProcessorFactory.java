/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats;

import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.storageprovider.StorageStatsTask;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.db.repo.JpaManifestItemRepo;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorCreationFailedException;
import org.duracloud.mill.workman.TaskProcessorFactoryBase;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: 03/03/2016
 */
public class StorageStatsTaskProcessorFactory
    extends TaskProcessorFactoryBase {

    private static Logger log =
        LoggerFactory.getLogger(StorageStatsTaskProcessorFactory.class);

    private StorageProviderFactory storageProviderFactory;
    private SpaceStatsManager spaceStatsManager;
    private JpaManifestItemRepo manifestItemRepo;

    /**
     * @param repo
     * @param storageProviderFactory
     * @param spaceStatsManager
     * @param manifestItemRepo
     */
    public StorageStatsTaskProcessorFactory(CredentialsRepo repo,
                                            StorageProviderFactory storageProviderFactory,
                                            SpaceStatsManager spaceStatsManager,
                                            JpaManifestItemRepo manifestItemRepo) {
        super(repo);
        this.storageProviderFactory = storageProviderFactory;
        this.spaceStatsManager = spaceStatsManager;
        this.manifestItemRepo = manifestItemRepo;
    }

    @Override
    public boolean isSupported(Task task) {
        return task.getType().equals(Task.Type.STORAGE_STATS);
    }

    @Override
    protected TaskProcessor createImpl(Task task)
        throws TaskProcessorCreationFailedException {

        StorageStatsTask storageStatsTask = new StorageStatsTask();
        storageStatsTask.readTask(task);
        String subdomain = storageStatsTask.getAccount();

        try {
            StorageProviderCredentials credentials =
                getCredentialRepo().getStorageProviderCredentials(subdomain, storageStatsTask.getStoreId());
            StorageProvider store = storageProviderFactory.create(credentials);

            StorageProviderType storageProviderType = credentials.getProviderType();
            return new StorageStatsTaskProcessor(storageStatsTask,
                                                 store,
                                                 storageProviderType,
                                                 spaceStatsManager,
                                                 manifestItemRepo);
        } catch (Exception e) {
            log.error("failed to create TaskProcessor: unable to locate" +
                      " credentials for subdomain: " + e.getMessage(), e);
            throw new TaskProcessorCreationFailedException(
                "failed to create TaskProcessor: unable to locate credentials " +
                "for subdomain: " + subdomain, e);
        }
    }
}
