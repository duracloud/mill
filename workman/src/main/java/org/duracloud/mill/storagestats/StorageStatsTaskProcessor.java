/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats;

import java.util.Date;
import java.util.Map;

import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.mill.common.storageprovider.StorageStatsTask;
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
           Date: 10/15/2014
 */
public class StorageStatsTaskProcessor extends
                                           TaskProcessorBase {

    private static final Logger log = LoggerFactory
            .getLogger(StorageStatsTaskProcessor.class);

    private StorageStatsTask storageStatsTask;
    private StorageProvider store;
    private StorageProviderType storageProviderType;
    private SpaceStatsManager spaceStatsManager;
    private CloudWatchStorageStatsGatherer storageStatsGatherer;
    public StorageStatsTaskProcessor(StorageStatsTask storageStatsTask,
                                          StorageProvider store,
                                          StorageProviderType storageProviderType, 
                                          SpaceStatsManager spaceStatsManager, 
                                          CloudWatchStorageStatsGatherer storageStatsGatherer) {
        super(storageStatsTask);
        this.storageStatsTask = storageStatsTask;
        this.store = store;
        this.storageProviderType = storageProviderType;
        this.spaceStatsManager = spaceStatsManager;
        if(storageStatsGatherer == null && store instanceof S3StorageProvider){
            throw new DuraCloudRuntimeException(
                                                "The storageStatsGatherer must "
                                                + "be non-null when the store "
                                                + "is an instance of S3StorageProvider "
                                                + "or one of its subclasses");
        }

        this.storageStatsGatherer = storageStatsGatherer;
    }


    @Override
    protected void executeImpl() throws TaskExecutionFailedException {
        String spaceId = this.storageStatsTask.getSpaceId();
        if(storageProviderType.equals(StorageProviderType.AMAZON_S3) || 
            storageProviderType.equals(StorageProviderType.AMAZON_GLACIER) || 
                storageProviderType.equals(StorageProviderType.SNAPSHOT)){
            
            BucketStats stats = this.storageStatsGatherer.getBucketStats(spaceId);
            addSpaceStats(spaceId, stats.getTotalBytes(), stats.getTotalItems());
        }else{
            Map<String,String> props = store.getSpaceProperties(spaceId);
            Long itemCount = parseLong(props,StorageProvider.PROPERTIES_SPACE_COUNT);
            Long byteCount = parseLong(props,StorageProvider.PROPERTIES_SPACE_SIZE);
            addSpaceStats(spaceId, byteCount, itemCount);
        }
        
    }


    /**
     * @param string
     * @return
     */
    private Long parseLong(Map<String,String> props, String key) {
        String value = props.get(key);
        if( value != null){
            try{
                return Long.valueOf(value);
            }catch(Exception ex){
                log.error("failed to parse value as long : " + ex.getMessage());
            }
        }else{
            log.warn("No value found for key {}", key);
        }
        return 0l;
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
