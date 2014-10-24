/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.duracloud.common.util.DateUtil;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.db.model.ManifestItem;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.*;

/**
 * @author Daniel Bernstein Date: 5/7/2014
 */
@RunWith(EasyMockRunner.class)
public class SpaceComparisonTaskProcessorTest extends EasyMockSupport {

    private static final String account = "account-id";
    private static final String storeId = "store-id";
    private static final String spaceId = "space-id";
    private String contentId = "contentId";
    private String checksum = "checksum";

    @Mock
    private BitIntegrityCheckReportTask task;

    @Mock
    private BitLogStore bitLogStore;

    private SpaceComparisonTaskProcessor taskProcessor;

    @Mock
    private StorageProvider store;

    @Mock
    private ManifestStore manifestStore;

    @Mock
    private ManifestItem manifestItem;

    private StorageProviderType storageProviderType = StorageProviderType.AMAZON_S3;

    private void createTestSubject() {
        taskProcessor = new SpaceComparisonTaskProcessor(task,
                                                         bitLogStore,
                                                         manifestStore,
                                                         store,
                                                         storageProviderType);
    }

    private void setupTask() {
        expect(task.getAccount()).andReturn(account);
        expect(task.getStoreId()).andReturn(storeId);
        expect(task.getSpaceId()).andReturn(spaceId);
    }

    @After
    public void teardown() {
        verifyAll();
    }

    @Test
    public void testNoErrors() throws Exception {
        setupTask();

        setupSpaceContentsIterator();
        setupGetManifestItem(false);

        expect(manifestItem.isDeleted()).andReturn(false);

        setupNoMissingContent();
        replayAll();
        createTestSubject();
        taskProcessor.execute();
    }

    private void setupNoMissingContent() {
        ManifestItem manifestItem2 = setupManifestIterator();

        Map<String,String> map = createMock(Map.class);
        expect(store.getContentProperties(eq(spaceId), eq(contentId))).andReturn(map);

        expect(manifestItem2.isMissingFromStorageProvider()).andReturn(false);
    }
    
    @Test
    public void testNoErrorsAndResetManifestMissingStorageProviderFlag() throws Exception {
        setupTask();

        setupSpaceContentsIterator();
        setupGetManifestItem(false);

        expect(manifestItem.isDeleted()).andReturn(false);

        ManifestItem manifestItem2 = setupManifestIterator();

        Map<String,String> map = createMock(Map.class);
        expect(store.getContentProperties(eq(spaceId), eq(contentId))).andReturn(map);

        expect(manifestItem2.isMissingFromStorageProvider()).andReturn(true);
        manifestStore.updateMissingFromStorageProviderFlag(eq(account), eq(storeId), eq(spaceId), eq(contentId), eq(false));
        expectLastCall();
        replayAll();
        createTestSubject();
        taskProcessor.execute();
    }

    @Test
    public void
            testMissingManifestItemAndMissingAndMissingContent() throws Exception {
        setupTask();

        setupSpaceContentsIterator();
        setupGetManifestItem(false);
        expect(manifestItem.isDeleted()).andReturn(true);
        int daysAgo = 2;
        setupGetContentProperties(daysAgo);
        setupMissingManifestBitLogWrite();
        ManifestItem manifestItem2 = setupManifestIterator();
        expect(manifestItem2.getContentChecksum()).andReturn(checksum);
        expect(manifestItem2.isDeleted()).andReturn(false);
        expect(manifestItem2.isMissingFromStorageProvider()).andReturn(true);
        expect(store.getContentProperties(eq(spaceId), eq(contentId)))
                .andThrow(new NotFoundException(""));
        setupMissingContentBitLogWrite();
        replayAll();
        createTestSubject();
        taskProcessor.execute();
    }

    @Test
    public void
            testNullManifestItemLessThanADayOld() throws Exception {
        setupTask();
        setupSpaceContentsIterator();
        setupGetManifestItem(true);
        int daysAgo = 0;
        setupGetContentProperties(daysAgo);
        setupNoMissingContent();
        replayAll();
        createTestSubject();
        taskProcessor.execute();
    }

    
    private void setupMissingContentBitLogWrite() {
        expect(bitLogStore.write(eq(account),
                                 eq(storeId),
                                 eq(spaceId),
                                 eq(contentId),
                                 isA(Date.class),
                                 eq(storageProviderType),
                                 eq(BitIntegrityResult.ERROR),
                                 isNull(String.class),
                                 isNull(String.class),
                                 eq(checksum),
                                 isA(String.class))).andReturn(null);
    }

    private ManifestItem setupManifestIterator() {
        Iterator<ManifestItem> manifestIt = createMock(Iterator.class);
        expect(manifestIt.hasNext()).andReturn(true);
        ManifestItem manifestItem2 = createMock(ManifestItem.class);
        expect(manifestIt.next()).andReturn(manifestItem2);
        expect(manifestItem2.getContentId()).andReturn(contentId);

        expect(manifestIt.hasNext()).andReturn(false);
        expect(manifestStore.getItems(eq(storeId), eq(spaceId)))
                .andReturn(manifestIt);
        return manifestItem2;
    }

    private void setupGetContentProperties(int daysAgo) {
        Map<String, String> map = createMock(Map.class);
        expect(map.get(eq(StorageProvider.PROPERTIES_CONTENT_CHECKSUM)))
                .andReturn(checksum);
        expect(map.get(eq(StorageProvider.PROPERTIES_CONTENT_MODIFIED)))
                .andReturn(new SimpleDateFormat(DateUtil.DateFormat.DEFAULT_FORMAT
                        .getPattern()).format(getDayFromXDaysAgo(daysAgo)));
        expect(store.getContentProperties(eq(spaceId), eq(contentId)))
                .andReturn(map);
    }

    private void setupMissingManifestBitLogWrite() {
        expect(bitLogStore.write(eq(account),
                                 eq(storeId),
                                 eq(spaceId),
                                 eq(contentId),
                                 isA(Date.class),
                                 eq(storageProviderType),
                                 eq(BitIntegrityResult.ERROR),
                                 isNull(String.class),
                                 eq(checksum),
                                 isNull(String.class),
                                 isA(String.class))).andReturn(null);
    }

    private void
            setupGetManifestItem(boolean returnNull) throws org.duracloud.error.NotFoundException {
        expect(this.manifestStore.getItem(eq(account),
                                          eq(storeId),
                                          eq(spaceId),
                                          eq(contentId)))
                .andReturn(returnNull ? null : manifestItem);
    }

    private void setupSpaceContentsIterator() {
        Iterator<String> it = createMock(Iterator.class);
        expect(it.hasNext()).andReturn(true);
        expect(it.next()).andReturn(contentId);
        expect(it.hasNext()).andReturn(false);
        expect(this.store.getSpaceContents(eq(spaceId), isNull(String.class)))
                .andReturn(it);
    }

    /**
     * @param i
     * @return
     */
    private Date getDayFromXDaysAgo(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1 * days);
        return c.getTime();
    }

}
