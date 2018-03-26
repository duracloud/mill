/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.duracloud.mill.common.storageprovider.StorageStatsTask;
import org.duracloud.mill.db.model.SpaceStats;
import org.duracloud.mill.db.repo.JpaManifestItemRepo;
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

/**
 * @author Daniel Bernstein
 * Date: Mar 3, 2016
 */
@RunWith(EasyMockRunner.class)
public class StorageStatsTaskProcessorTest extends EasyMockSupport {

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

    @Mock
    private JpaManifestItemRepo manifestItemRepo;

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
    public void testS3() throws Exception {
        testS3Based(StorageProviderType.AMAZON_S3);
    }

    @Test
    public void testOutOfTimeWindow() throws Exception {
        String spaceId = "space-id";
        String storeId = "store-id";
        String account = "account";
        expect(task.getSpaceId()).andReturn(spaceId).atLeastOnce();
        expect(task.getStoreId()).andReturn(storeId).atLeastOnce();
        expect(task.getAccount()).andReturn(account).atLeastOnce();
        expect(task.getAttempts()).andReturn(0);
        replayAll();
        StorageStatsTaskProcessor processor = new StorageStatsTaskProcessor(task,
                                                                            storageProvider,
                                                                            StorageProviderType.AMAZON_S3,
                                                                            spaceStatsManager,
                                                                            storageStatsGatherer,
                                                                            manifestItemRepo);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.HOUR_OF_DAY, 19);
        processor.currentTime = c.getTime();
        processor.execute();
    }

    @Test
    public void testGlacier() throws Exception {
        String spaceId = "space-id";
        String storeId = "store-id";
        String account = "account";
        long byteCount = 100l;
        long objectCount = 101l;

        expect(task.getSpaceId()).andReturn(spaceId).atLeastOnce();
        expect(task.getStoreId()).andReturn(storeId).atLeastOnce();
        expect(task.getAccount()).andReturn(account).atLeastOnce();
        expect(task.getAttempts()).andReturn(0);

        expect(this.manifestItemRepo.getStorageStatsByAccountAndStoreIdAndSpaceId(account, storeId, spaceId))
            .andReturn(new Object[] {new Object[] {new Long(objectCount), new Long(byteCount)}});
        expect(spaceStatsManager.addSpaceStats(isA(Date.class),
                                               eq(account),
                                               eq(storeId),
                                               eq(spaceId),
                                               eq(byteCount),
                                               eq(objectCount))).andReturn(new SpaceStats());

        replayAll();

        StorageStatsTaskProcessor processor = createProcessor(StorageProviderType.AMAZON_GLACIER);

        processor.execute();

    }

    private StorageStatsTaskProcessor createProcessor(StorageProviderType providerType) {
        StorageStatsTaskProcessor processor = new StorageStatsTaskProcessor(task,
                                                                            storageProvider,
                                                                            providerType,
                                                                            spaceStatsManager,
                                                                            storageStatsGatherer,
                                                                            manifestItemRepo);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.HOUR_OF_DAY, 21);
        processor.currentTime = c.getTime();
        return processor;
    }

    @Test
    public void testDpn() throws Exception {
        testS3Based(StorageProviderType.DPN);
    }

    @Test
    public void testChronopolis() throws Exception {
        testS3Based(StorageProviderType.CHRONOPOLIS);
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

        expect(spaceStatsManager.addSpaceStats(isA(Date.class),
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

        StorageStatsTaskProcessor processor = createProcessor(storageProviderType);
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
        Map<String, String> props = new HashMap<>();
        props.put(StorageProvider.PROPERTIES_SPACE_COUNT, objectCount.toString());
        props.put(StorageProvider.PROPERTIES_SPACE_SIZE, byteCount.toString());

        expect(this.storageProvider.getSpaceProperties(spaceId)).andReturn(props);
        expect(spaceStatsManager.addSpaceStats(isA(Date.class),
                                               eq(account),
                                               eq(storeId),
                                               eq(spaceId),
                                               eq(byteCount.longValue()),
                                               eq(objectCount.longValue()))).andReturn(new SpaceStats());
        replayAll();

        StorageStatsTaskProcessor processor = createProcessor(storageProviderType);
        processor.execute();
    }

}

