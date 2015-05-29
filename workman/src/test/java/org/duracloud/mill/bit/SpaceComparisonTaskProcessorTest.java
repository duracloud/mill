/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;

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
        setupNoMissingContent();
        replayAll();
        createTestSubject();
        taskProcessor.execute();
    }

    private void setupNoMissingContent() {
        ManifestItem manifestItem2 = setupManifestIterator();
        store = setStorageProvider(contentId);
        
        expect(manifestItem2.isMissingFromStorageProvider()).andReturn(false);
    }
    
    /**
     * @return
     */
    private StorageProvider setStorageProvider(String... contentIds) {
        expect(store.getSpaceContents(spaceId, null)).andReturn(Arrays.asList(contentIds).iterator());
        return store;
    }

    @Test
    public void testNoErrorsAndResetManifestMissingStorageProviderFlag() throws Exception {
        setupTask();



        ManifestItem manifestItem2 = setupManifestIterator();
        store = setStorageProvider(contentId);
        expect(manifestItem2.isMissingFromStorageProvider()).andReturn(true);
        manifestStore.updateMissingFromStorageProviderFlag(eq(account), eq(storeId), eq(spaceId), eq(contentId), eq(false));
        expectLastCall();
        replayAll();
        createTestSubject();
        taskProcessor.execute();
    }

    @Test
    public void
            testMissingContent() throws Exception {
        setupTask();
        store = setStorageProvider("notthecontentidimlookingfor");
        ManifestItem manifestItem2 = setupManifestIterator();
        expect(manifestItem2.getContentChecksum()).andReturn(checksum);
        expect(manifestItem2.isDeleted()).andReturn(false);
        expect(manifestItem2.isMissingFromStorageProvider()).andReturn(true);
        setupMissingContentBitLogWrite();
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
        expect(manifestStore.getItems(eq(account), eq(storeId), eq(spaceId)))
                .andReturn(manifestIt);
        return manifestItem2;
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

}
