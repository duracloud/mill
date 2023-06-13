/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats;

import static org.duracloud.storage.domain.StorageProviderType.AMAZON_GLACIER;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Date;

import org.duracloud.mill.common.storageprovider.StorageStatsTask;
import org.duracloud.mill.db.model.SpaceStats;
import org.duracloud.mill.db.repo.JpaManifestItemRepo;
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
    public void testGlacier() throws Exception {
        test(StorageProviderType.AMAZON_GLACIER);
    }

    @Test
    public void testChronopolis() throws Exception {
        test(StorageProviderType.CHRONOPOLIS);
    }

    @Test
    public void testS3() throws Exception {
        test(StorageProviderType.AMAZON_S3);
    }

    private void test(StorageProviderType storageProviderType) throws Exception {

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
            .andReturn(new Object[] {new Object[] {objectCount, byteCount}});
        expect(spaceStatsManager.addSpaceStats(isA(Date.class),
                                               eq(account),
                                               eq(storeId),
                                               eq(spaceId),
                                               eq(byteCount),
                                               eq(objectCount))).andReturn(new SpaceStats());

        replayAll();

        StorageStatsTaskProcessor processor = createProcessor(AMAZON_GLACIER);

        processor.execute();

    }

    private StorageStatsTaskProcessor createProcessor(StorageProviderType providerType) {
        return new StorageStatsTaskProcessor(task,
                                             storageProvider,
                                             providerType,
                                             spaceStatsManager,
                                             manifestItemRepo);
    }
}

