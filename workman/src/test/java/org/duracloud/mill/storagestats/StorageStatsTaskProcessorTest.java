/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.duracloud.mill.common.storageprovider.StorageStatsTask;
import org.duracloud.mill.db.model.SpaceStats;
import org.duracloud.mill.storagestats.aws.BucketStats;
import org.duracloud.mill.storagestats.aws.CloudWatchStorageStatsGatherer;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.*;
/**
 * @author Daniel Bernstein
 *         Date: Mar 3, 2016
 */
@RunWith(EasyMockRunner.class)
public class StorageStatsTaskProcessorTest  extends EasyMockSupport {


    
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private StorageStatsTask storageStatsTask;
    @Mock
    private SpaceStatsManager spaceStatsManager;
    @Mock
    private CloudWatchStorageStatsGatherer storageStatsGatherer;

    @Mock 
    private StorageStatsTask task;
    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
    }


    @Test
    public void testS3() throws Exception{
        testS3Based(StorageProviderType.AMAZON_S3);
    }

    @Test
    public void testGlacier() throws Exception{
        testS3Based(StorageProviderType.AMAZON_GLACIER);
    }

    @Test
    public void testSnapshot() throws Exception{
        testS3Based(StorageProviderType.SNAPSHOT);
    }

    private void testS3Based(StorageProviderType storageProviderType) throws Exception {
        String spaceId = "space-id";
        String storeId = "store-id";
        String account = "account";
        long byteCount = 100l;
        long objectCount = 101l;
        
        expect(task.getSpaceId()).andReturn(spaceId).atLeastOnce();
        expect(task.getStoreId()).andReturn(storeId).atLeastOnce();
        expect(task.getAccount()).andReturn(account).atLeastOnce();
        expect(task.getAttempts()).andReturn(0);

      
        expect(spaceStatsManager
                .addSpaceStats(isA(Date.class),
                               eq(account),
                               eq(storeId),
                               eq(spaceId),
                               eq(byteCount),
                               eq(objectCount))).andReturn(new SpaceStats());
        BucketStats stats = createMock(BucketStats.class);
        expect(stats.getTotalBytes()).andReturn(byteCount);
        expect(stats.getTotalItems()).andReturn(objectCount);
        expect(storageStatsGatherer.getBucketStats(spaceId)).andReturn(stats);
        replayAll();
        
        StorageStatsTaskProcessor processor = new StorageStatsTaskProcessor(task,
                                                                            storageProvider,
                                                                            storageProviderType,
                                                                            spaceStatsManager,
                                                                            storageStatsGatherer);
        processor.execute();
    }

    @Test
    public void testNonS3() throws Exception {
        String spaceId = "space-id";
        String storeId = "store-id";
        String account = "account";
        Long byteCount = 100l;
        Long objectCount = 101l;
        
        expect(task.getSpaceId()).andReturn(spaceId).atLeastOnce();
        expect(task.getStoreId()).andReturn(storeId).atLeastOnce();
        expect(task.getAccount()).andReturn(account).atLeastOnce();
        expect(task.getAttempts()).andReturn(0);

        StorageProviderType storageProviderType = StorageProviderType.SDSC;
        Map<String,String> props = new HashMap<>();
        props.put(StorageProvider.PROPERTIES_SPACE_COUNT, objectCount.toString());
        props.put(StorageProvider.PROPERTIES_SPACE_SIZE, byteCount.toString());

        expect(this.storageProvider.getSpaceProperties(spaceId)).andReturn(props);
        expect(spaceStatsManager
                .addSpaceStats(isA(Date.class),
                               eq(account),
                               eq(storeId),
                               eq(spaceId),
                               eq(byteCount.longValue()),
                               eq(objectCount.longValue()))).andReturn(new SpaceStats());
        replayAll();
        
        StorageStatsTaskProcessor processor = new StorageStatsTaskProcessor(task,
                                                                            storageProvider,
                                                                            storageProviderType,
                                                                            spaceStatsManager,
                                                                            storageStatsGatherer);
        processor.execute();
    }

}
 
