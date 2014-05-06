/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.audit.AuditLogItem;
import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.dynamodb.DynamoDBAuditLogItem;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.contentindex.client.ContentIndexItem;
import org.duracloud.error.NotFoundException;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isNull;

/**
 * @author Erik Paulsson
 *         Date: 5/5/14
 */
public class BitIntegrityCheckTaskProcessorTest {

    private static final String account = "account-id";
    private static final String storeId = "store-id";
    private static final String spaceId = "space-id";
    private static final String contentId = "content-id";
    private static final String checksum = "checksum";
    private static final String badChecksum = "bad-checksum";


    private BitIntegrityCheckTask task;
    private StorageProvider store;
    private AuditLogStore auditLogStore;
    private ContentIndexClient contentIndexClient;
    private BitLogStore bitLogStore;
    private ChecksumUtil checksumUtil;
    private BitIntegrityCheckTaskProcessor taskProcessor;

    @Before
    public void setup() throws IOException {
        store = EasyMock.createMock(StorageProvider.class);
        auditLogStore = EasyMock.createMock(AuditLogStore.class);
        bitLogStore = EasyMock.createMock(BitLogStore.class);
        contentIndexClient = EasyMock.createMock(ContentIndexClient.class);
        checksumUtil = EasyMock.createMock(ChecksumUtil.class);

        task = new BitIntegrityCheckTask();
        task.setAccount(account);
        task.setStoreId(storeId);
        task.setSpaceId(spaceId);
        task.setContentId(contentId);
    }

    @After
    public void teardown() {
        EasyMock.verify(store, auditLogStore, bitLogStore, contentIndexClient, checksumUtil);
    }

    private void replayMocks() {
        EasyMock.replay(store, auditLogStore, bitLogStore, contentIndexClient, checksumUtil);
    }

    private Map<String, String> createChecksumProps(String value) {
        Map<String, String> props = new HashMap<>();
        props.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, value);
        return props;
    }

    private void storeMockValidChecksum() {
        Map<String, String> props = createChecksumProps(checksum);
        EasyMock.expect(store.getContentProperties(spaceId, contentId))
                .andReturn(props);
    }

    private void storeMockInvalidChecksum() {
        Map<String, String> props = createChecksumProps(badChecksum);
        EasyMock.expect(store.getContentProperties(spaceId, contentId))
                .andReturn(props);
    }

    private void auditLogStoreMockValidChecksum() throws NotFoundException {
        AuditLogItem item = new DynamoDBAuditLogItem(contentId, null, account,
                                                     storeId, spaceId, contentId,
                                                     checksum, null, null, null,
                                                     null, null, null, null, null,
                                                     System.currentTimeMillis());
        EasyMock.expect(auditLogStore.getLatestLogItem(
            account, storeId, spaceId, contentId)).andReturn(item);
    }

    private void bitLogStoreMockValid(StorageProviderType storeType)
        throws  Exception {

        EasyMock.expect(bitLogStore.write(eq(account), eq(storeId), eq(spaceId),
                                          eq(contentId), EasyMock.anyLong(),
                                          eq(storeType), eq(BitIntegrityResult.SUCCESS),
                                          eq(checksum), isNull(String.class),
                                          isNull(String.class), isNull(String.class),
                                          isNull(String.class)))
                .andReturn(EasyMock.createMock(BitLogItem.class));
    }

    private void bitLogStoreMockInvalid(StorageProviderType storeType,
                                        String contentChecksum,
                                        String storeChecksum,
                                        String auditLogChecksum,
                                        String contentIndexChecksum)
        throws  Exception {

        EasyMock.expect(bitLogStore.write(eq(account), eq(storeId), eq(spaceId),
                                          eq(contentId), EasyMock.anyLong(),
                                          eq(storeType), eq(BitIntegrityResult.FAILURE),
                                          eq(contentChecksum), eq(storeChecksum),
                                          eq(auditLogChecksum),
                                          eq(contentIndexChecksum), isNull(
            String.class)))
                .andReturn(EasyMock.createMock(BitLogItem.class));
    }

    private void auditLogStoreMockInvalidChecksum()  throws NotFoundException {
        AuditLogItem item = new DynamoDBAuditLogItem(contentId, null, account,
                                                     storeId, spaceId, contentId,
                                                     badChecksum, null, null, null,
                                                     null, null, null, null, null,
                                                     System.currentTimeMillis());
        EasyMock.expect(auditLogStore.getLatestLogItem(
            account, storeId, spaceId, contentId)).andReturn(item);
    }

    private void contentIndexClientMockValidChecksum() {
        Map<String, String> props = createChecksumProps(checksum);
        ContentIndexItem item = new ContentIndexItem(account, storeId, spaceId,
                                                     contentId);
        item.setProps(props);
        EasyMock.expect(contentIndexClient.get(
            account, storeId, spaceId, contentId)).andReturn(item);
    }

    private void contentIndexClientMockInvalidChecksum() {
        Map<String, String> props = createChecksumProps(badChecksum);
        ContentIndexItem item = new ContentIndexItem(account, storeId, spaceId,
                                                     contentId);
        item.setProps(props);
        EasyMock.expect(contentIndexClient.get(
            account, storeId, spaceId, contentId)).andReturn(item);
    }

    private void checksumUtilMockValid() {
        InputStream is = EasyMock.createMock(InputStream.class);
        EasyMock.expect(store.getContent(spaceId, contentId)).andReturn(is);
        EasyMock.expect(checksumUtil.generateChecksum(is)).andReturn(checksum);
    }

    private void checksumUtilMockInvalid() {
        InputStream is = EasyMock.createMock(InputStream.class);
        EasyMock.expect(store.getContent(spaceId, contentId)).andReturn(is);
        EasyMock.expect(checksumUtil.generateChecksum(is)).andReturn(badChecksum);
    }

    @Test
    public void testS3ContentValid() throws Exception {
        taskProcessor = new BitIntegrityCheckTaskProcessor(task, store,
                                                           StorageProviderType.AMAZON_S3,
                                                           auditLogStore,
                                                           bitLogStore,
                                                           contentIndexClient,
                                                           checksumUtil);
        storeMockValidChecksum();
        auditLogStoreMockValidChecksum();
        contentIndexClientMockValidChecksum();
        checksumUtilMockValid();
        bitLogStoreMockValid(StorageProviderType.AMAZON_S3);

        replayMocks();
        taskProcessor.execute();
    }

    @Test
    public void testS3ContentInvalid() throws Exception {
        taskProcessor = new BitIntegrityCheckTaskProcessor(task, store,
                                                           StorageProviderType.AMAZON_S3,
                                                           auditLogStore,
                                                           bitLogStore,
                                                           contentIndexClient,
                                                           checksumUtil);
        storeMockValidChecksum();
        auditLogStoreMockValidChecksum();
        contentIndexClientMockValidChecksum();
        checksumUtilMockInvalid();
        bitLogStoreMockInvalid(StorageProviderType.AMAZON_S3,
                               badChecksum, checksum, checksum, checksum);

        replayMocks();
        taskProcessor.execute();
    }

    @Test
    public void testGlacierContentValid() throws Exception {
        taskProcessor = new BitIntegrityCheckTaskProcessor(task, store,
                                                           StorageProviderType.AMAZON_GLACIER,
                                                           auditLogStore,
                                                           bitLogStore,
                                                           contentIndexClient,
                                                           checksumUtil);
        storeMockValidChecksum();
        auditLogStoreMockValidChecksum();
        contentIndexClientMockValidChecksum();
        bitLogStoreMockValid(StorageProviderType.AMAZON_GLACIER);

        replayMocks();
        taskProcessor.execute();
    }

    @Test
    public void testGlacierContentInvalid() throws Exception {
        taskProcessor = new BitIntegrityCheckTaskProcessor(task, store,
                                                           StorageProviderType.AMAZON_GLACIER,
                                                           auditLogStore,
                                                           bitLogStore,
                                                           contentIndexClient,
                                                           checksumUtil);
        storeMockValidChecksum();
        auditLogStoreMockValidChecksum();
        contentIndexClientMockInvalidChecksum();
        bitLogStoreMockInvalid(StorageProviderType.AMAZON_GLACIER, null, checksum,
                               checksum, badChecksum);

        replayMocks();
        taskProcessor.execute();
    }

    @Test
    public void testSDSCContentValid() throws Exception {
        taskProcessor = new BitIntegrityCheckTaskProcessor(task, store,
                                                           StorageProviderType.SDSC,
                                                           auditLogStore,
                                                           bitLogStore,
                                                           contentIndexClient,
                                                           checksumUtil);
        storeMockValidChecksum();
        auditLogStoreMockValidChecksum();
        contentIndexClientMockValidChecksum();
        checksumUtilMockValid();
        bitLogStoreMockValid(StorageProviderType.SDSC);

        replayMocks();
        taskProcessor.execute();
    }

    @Test
    public void testSDSCContentInvalid() throws Exception {
        taskProcessor = new BitIntegrityCheckTaskProcessor(task, store,
                                                           StorageProviderType.SDSC,
                                                           auditLogStore,
                                                           bitLogStore,
                                                           contentIndexClient,
                                                           checksumUtil);
        storeMockValidChecksum();
        auditLogStoreMockInvalidChecksum();
        contentIndexClientMockValidChecksum();
        // null is passed for the content stream generated checksum because
        // the processor doesn't get to that point since the audit log
        // checksum was invalid.
        bitLogStoreMockInvalid(StorageProviderType.SDSC, null, checksum,
                               badChecksum, checksum);

        replayMocks();
        taskProcessor.execute();
    }
}
