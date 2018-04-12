/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.mill.common.storageprovider.StorageStatsTask;
import org.duracloud.mill.db.repo.JpaManifestItemRepo;
import org.duracloud.mill.storagestats.aws.BucketStats;
import org.duracloud.mill.storagestats.aws.CloudWatchStorageStatsGatherer;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessorBase;
import org.duracloud.s3storage.S3StorageProvider;
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
    private CloudWatchStorageStatsGatherer storageStatsGatherer;
    private JpaManifestItemRepo manifestRepo;

    //the current time is protected so that it can be overridden by unit tests.
    protected Date currentTime = null;

    public StorageStatsTaskProcessor(StorageStatsTask storageStatsTask,
                                     StorageProvider store,
                                     StorageProviderType storageProviderType,
                                     SpaceStatsManager spaceStatsManager,
                                     CloudWatchStorageStatsGatherer storageStatsGatherer,
                                     JpaManifestItemRepo manifestRepo) {
        super(storageStatsTask);
        this.storageStatsTask = storageStatsTask;
        this.store = store;
        this.storageProviderType = storageProviderType;
        this.spaceStatsManager = spaceStatsManager;
        this.manifestRepo = manifestRepo;
        if (storageStatsGatherer == null && store instanceof S3StorageProvider) {
            throw new DuraCloudRuntimeException("The storageStatsGatherer must be non-null when the store " +
                                                "is an instance of S3StorageProvider or one of its subclasses");
        }

        this.storageStatsGatherer = storageStatsGatherer;
    }

    @Override
    protected void executeImpl() throws TaskExecutionFailedException {
        //check that this task is executing between 20:00 and 23:59 UTC
        //in order to ensure that AWS  returns valid results.
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (currentTime == null) {
            currentTime = c.getTime();
        }
        c.set(Calendar.HOUR_OF_DAY, 20);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        Date eightPmUTC = c.getTime();

        //a few seconds before midnight in case this task executes too close to the wire.
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 45);
        Date midnightUTC = c.getTime();

        if (currentTime.before(eightPmUTC) || currentTime.after(midnightUTC)) {
            log.warn("Skipping processing of this task ({}):  current time ({}) is " +
                     "outside of valid processing window( ie between {} and {})",
                     this.storageStatsTask,
                     currentTime,
                     eightPmUTC,
                     midnightUTC);
            return;
        }

        String spaceId = this.storageStatsTask.getSpaceId();
        if (storageProviderType.equals(StorageProviderType.AMAZON_S3) ||
            storageProviderType.equals(StorageProviderType.DPN) ||
            storageProviderType.equals(StorageProviderType.CHRONOPOLIS)) {

            BucketStats stats = this.storageStatsGatherer.getBucketStats(spaceId);
            addSpaceStats(spaceId, stats.getTotalBytes(), stats.getTotalItems());
        } else if (storageProviderType.equals(StorageProviderType.AMAZON_GLACIER)) {
            Object[] stats =
                this.manifestRepo.getStorageStatsByAccountAndStoreIdAndSpaceId(this.storageStatsTask.getAccount(),
                                                                               this.storageStatsTask.getStoreId(),
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
            addSpaceStats(spaceId, byteCount, itemCount);
        }

    }

    private void addSpaceStats(String spaceId, long byteCount, long itemCount) {
        spaceStatsManager.addSpaceStats(new Date(),
                                        storageStatsTask.getAccount(),
                                        storageStatsTask.getStoreId(),
                                        spaceId,
                                        byteCount,
                                        itemCount);
    }
}
