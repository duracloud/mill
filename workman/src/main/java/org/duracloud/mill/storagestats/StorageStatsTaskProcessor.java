/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats;

import java.util.Date;

import org.duracloud.mill.common.storageprovider.StorageStatsTask;
import org.duracloud.mill.db.repo.JpaManifestItemRepo;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessorBase;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes StorageStats Tasks.
 *
 * @author Daniel Bernstein
 * Date: 10/15/2014
 */
public class StorageStatsTaskProcessor extends TaskProcessorBase {

    private static final Logger log = LoggerFactory.getLogger(StorageStatsTaskProcessor.class);

    private StorageStatsTask storageStatsTask;
    private StorageProvider store;
    private StorageProviderType storageProviderType;
    private SpaceStatsManager spaceStatsManager;
    private JpaManifestItemRepo manifestRepo;

    //the current time is protected so that it can be overridden by unit tests.
    protected Date currentTime = null;

    public StorageStatsTaskProcessor(StorageStatsTask storageStatsTask,
                                     StorageProvider store,
                                     StorageProviderType storageProviderType,
                                     SpaceStatsManager spaceStatsManager,
                                     JpaManifestItemRepo manifestRepo) {
        super(storageStatsTask);
        this.storageStatsTask = storageStatsTask;
        this.store = store;
        this.storageProviderType = storageProviderType;
        this.spaceStatsManager = spaceStatsManager;
        this.manifestRepo = manifestRepo;
    }

    @Override
    protected void executeImpl() throws TaskExecutionFailedException {
        String spaceId = this.storageStatsTask.getSpaceId();
        String storeId = this.storageStatsTask.getStoreId();
        String accountId = this.storageStatsTask.getAccount();
        Object[] stats =
            this.manifestRepo.getStorageStatsByAccountAndStoreIdAndSpaceId(accountId,
                                                                           storeId,
                                                                           spaceId);
        long itemCount = 0;
        long byteCount = 0;
        Object[] statRow = (Object[]) stats[0];

        if (statRow[0] != null) {
            itemCount = ((Number) statRow[0]).longValue();
        }
        if (statRow[1] != null) {
            byteCount = ((Number) statRow[1]).longValue();
        }

        addSpaceStats(accountId, storeId, spaceId, byteCount, itemCount);
    }

    private void addSpaceStats(String accountId, String storeId, String spaceId, long byteCount, long itemCount) {
        spaceStatsManager.addSpaceStats(new Date(),
                                        accountId,
                                        storeId,
                                        spaceId,
                                        byteCount,
                                        itemCount);
    }
}
