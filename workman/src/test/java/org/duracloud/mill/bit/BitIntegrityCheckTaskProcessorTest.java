/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.duracloud.audit.AuditLogItem;
import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.AuditLogWriteFailedException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.contentindex.client.ContentIndexClientValidationException;
import org.duracloud.contentindex.client.ContentIndexItem;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Erik Paulsson Date: 5/5/14
 */
@RunWith(EasyMockRunner.class)
public class BitIntegrityCheckTaskProcessorTest extends EasyMockSupport {

    private static final String            account     = "account-id";
    private static final String            storeId     = "store-id";
    private static final String            spaceId     = "space-id";
    private static final String            contentId   = "content-id";
    private static final String            user        = "user";
    private static final String            mimetype    = "text/plain";
    private static final String            contentSize = "10240";

    private static final String            checksum    = "checksum";
    private static final String            badChecksum = "bad-checksum";

    private BitIntegrityCheckTask          task;
    @Mock
    private StorageProvider                store;
    @Mock
    private AuditLogStore                  auditLogStore;
    @Mock
    private ContentIndexClient             contentIndexClient;
    @Mock
    private BitLogStore                    bitLogStore;
    @Mock
    private ChecksumUtil                   checksumUtil;
    @Mock
    private TaskQueue                      bitErrorQueue;
    @Mock
    private TaskQueue                      auditQueue;

    private BitIntegrityCheckTaskProcessor taskProcessor;

    @Before
    public void setup() throws IOException {
        task = createBitIntegrityCheckTask(1);
    }

    /**
     * @return
     * 
     */
    private BitIntegrityCheckTask createBitIntegrityCheckTask(final int attempts) {

        BitIntegrityCheckTask task = new BitIntegrityCheckTask() {
            @Override
            public int getAttempts() {
                return attempts;
            }
        };

        task.setAccount(account);
        task.setStoreId(storeId);
        task.setSpaceId(spaceId);
        task.setContentId(contentId);
        return task;
    }

    @After
    public void teardown() {
        verifyAll();
    }

    private Map<String, String> createChecksumProps(String checksum) {
        Map<String, String> props = new HashMap<>();
        props.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        props.put(StorageProvider.PROPERTIES_CONTENT_SIZE, contentSize);
        props.put(StorageProvider.PROPERTIES_CONTENT_MIMETYPE, mimetype);
        props.put(StorageProvider.PROPERTIES_CONTENT_CREATOR, user);

        return props;
    }

    private void storeMockValidChecksum() {
        Map<String, String> props = createChecksumProps(checksum);
        storeMockGetProperties(props);
    }

    private void storeMockInvalidChecksum() {
        Map<String, String> props = createChecksumProps(badChecksum);
        storeMockGetProperties(props);
    }

    private void storeMockNotFound() {
        EasyMock.expect(store.getContentProperties(spaceId, contentId))
                .andThrow(new NotFoundException("test")).times(4);
        EasyMock.expect(store.getContent(spaceId, contentId)).andThrow(
                new NotFoundException("test")).times(4);
    }

    /**
     * @param props
     */
    private void storeMockGetProperties(Map<String, String> props) {
        EasyMock.expect(store.getContentProperties(spaceId, contentId))
                .andReturn(props);
    }

    private void bitLogStoreMockValid(StorageProviderType storeType)
            throws Exception {

        EasyMock.expect(
                bitLogStore.write(eq(account), eq(storeId), eq(spaceId),
                        eq(contentId), EasyMock.anyLong(), eq(storeType),
                        eq(BitIntegrityResult.SUCCESS), eq(checksum),
                        isNull(String.class), isNull(String.class),
                        isNull(String.class), isNull(String.class))).andReturn(
                EasyMock.createMock(BitLogItem.class));
    }

    private void bitLogStoreMockInvalid(StorageProviderType storeType,
            String contentChecksum,
            String storeChecksum,
            String auditLogChecksum,
            String contentIndexChecksum) throws Exception {

        EasyMock.expect(
                bitLogStore.write(eq(account), eq(storeId), eq(spaceId),
                        eq(contentId), EasyMock.anyLong(), eq(storeType),
                        eq(BitIntegrityResult.FAILURE), isNullOrEq(contentChecksum),
                        isNullOrEq(storeChecksum), isNullOrEq(auditLogChecksum),
                        isNullOrEq(contentIndexChecksum), EasyMock.isA(String.class)))
                .andReturn(EasyMock.createMock(BitLogItem.class));
    }

    /**
     * @param value
     * @return
     */
    private String isNullOrEq(String value) {
        return value == null ?
                isNull(String.class):
                    eq(value);
    }

    private void auditLogStoreMockInvalidChecksum() throws org.duracloud.error.NotFoundException  {
        auditLogStoreMockChecksum(badChecksum);
    }

    /**
     * @param auditChecksum
     * @throws NotFoundException
     */
    private void auditLogStoreMockChecksum(String auditChecksum)
            throws org.duracloud.error.NotFoundException {
        AuditLogItem item = createMock(AuditLogItem.class);
        EasyMock.expect(
                auditLogStore.getLatestLogItem(account, storeId, spaceId,
                        contentId)).andReturn(item);
    }

    private void auditLogStoreMockValidChecksum() throws org.duracloud.error.NotFoundException  {
        auditLogStoreMockChecksum(checksum);
    }

    private void contentIndexClientMockValidChecksum() {
        Map<String, String> props = createChecksumProps(checksum);
        ContentIndexItem item = new ContentIndexItem(account, storeId, spaceId,
                contentId);
        item.setProps(props);
        contentIndexMockGet(item);
    }
    
    private void contentIndexClientMockItemNotFound() {
        contentIndexMockGet(null);
    }

    /**
     * @param item
     */
    private void contentIndexMockGet(ContentIndexItem item) {
        EasyMock.expect(
                contentIndexClient.get(account, storeId, spaceId, contentId))
                .andReturn(item);
    }

    private void contentIndexClientMockInvalidChecksum() {
        Map<String, String> props = createChecksumProps(badChecksum);
        ContentIndexItem item = new ContentIndexItem(account, storeId, spaceId,
                contentId);
        item.setProps(props);
        contentIndexMockGet(item);
    }

    private void checksumUtilMockValid() {
        InputStream is = storeMockInputstream();
        EasyMock.expect(checksumUtil.generateChecksum(is)).andReturn(checksum);
    }

    private void checksumUtilMockInvalid() {
        InputStream is = storeMockInputstream();
        EasyMock.expect(checksumUtil.generateChecksum(is)).andReturn(
                badChecksum);
    }

    /**
     * @return
     */
    private InputStream storeMockInputstream() {
        InputStream is = EasyMock.createMock(InputStream.class);
        EasyMock.expect(store.getContent(spaceId, contentId)).andReturn(is);
        return is;
    }

    /**
     * @param storageProviderType
     * @return
     */
    private BitIntegrityCheckTaskProcessor createTaskProcessor(StorageProviderType storageProviderType) {
        return new BitIntegrityCheckTaskProcessor(task, store,
                storageProviderType, auditLogStore, bitLogStore,
                contentIndexClient, checksumUtil, bitErrorQueue, auditQueue);
    }

    @Test
    public void testSuccessWithContentCheckS3() throws Exception {
        testSuccess(StorageProviderType.AMAZON_S3, true);
    }

    @Test
    public void testSuccessWithContentCheckSDSC() throws Exception {
        testSuccess(StorageProviderType.SDSC, true);
    }

    @Test
    public void testSuccessWithOutContentCheckGlacier() throws Exception {
        testSuccess(StorageProviderType.AMAZON_GLACIER, false);
    }

    @Test
    public void testSuccessWithOutContentCheckChronStage() throws Exception {
        testSuccess(StorageProviderType.SNAPSHOT, false);
    }

    @Test
    public void testSuccessWithOutContentCheckIRODS() throws Exception {
        testSuccess(StorageProviderType.IRODS, false);
    }

    @Test
    public void testSuccessWithOutContentCheckRackSpace() throws Exception {
        testSuccess(StorageProviderType.RACKSPACE, false);
    }

    @Test
    public void testSourContent() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        checksumUtilMockInvalid();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        bitLogStoreMockInvalid(storeType, badChecksum, checksum, checksum,
                checksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testFailedStorageProviderChecksum() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockInvalidChecksum();
        checksumUtilMockValid();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        bitLogStoreMockInvalid(storeType, checksum, badChecksum, checksum,
                checksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testFailedStorageProviderChecksumWithNoContentChecksumFirstAttempt()
            throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_GLACIER;
        storeMockInvalidChecksum();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        taskProcessorExecutionExpectedFailure();
    }

    @Test
    public void testFailedStorageProviderChecksumWithNoContentChecksumPenultimateAttempt()
            throws Exception {
        task = createBitIntegrityCheckTask(2);
        StorageProviderType storeType = StorageProviderType.AMAZON_GLACIER;
        storeMockInvalidChecksum();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        this.taskProcessor = createTaskProcessor(storeType);
        long penultimateWait = 1000;
        this.taskProcessor.setPenultimateWaitMS(penultimateWait);

        replayAll();
        failAfterWait(penultimateWait);
    }

    @Test
    public void testFailedStorageProviderChecksumWithNoContentChecksumLastAttempt()
            throws Exception {
        task = createBitIntegrityCheckTask(3);
        StorageProviderType storeType = StorageProviderType.AMAZON_GLACIER;
        storeMockInvalidChecksum();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        bitLogStoreMockInvalid(storeType, null, badChecksum, checksum, checksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testContentIndexChecksumInvalid() throws Exception {
        testContentIndexChecksumFailed(badChecksum);
    }

    @Test
    public void testContentIndexChecksumNull() throws Exception {
        testContentIndexChecksumFailed(badChecksum);
    }

    /**
     * @param contentIndexChecksum
     * @throws NotFoundException
     * @throws Exception
     * @throws TaskExecutionFailedException
     */
    private void testContentIndexChecksumFailed(String contentIndexChecksum)
            throws NotFoundException, Exception, TaskExecutionFailedException {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        checksumUtilMockValid();
        contentIndexClientMockInvalidChecksum();
        auditLogStoreMockValidChecksum();
        contentIndexMockSave();
        auditLogStoreMockUpdateProperties();
        bitLogStoreMockInvalid(storeType, checksum, checksum, checksum,
                contentIndexChecksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testAuditLogChecksumFailed() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        checksumUtilMockValid();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockInvalidChecksum();
        bitLogStoreMockInvalid(storeType, checksum, checksum, badChecksum,
                checksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testAuditLogChecksumNull() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        checksumUtilMockValid();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockChecksum(null);
        bitLogStoreMockInvalid(storeType, checksum, checksum, null, checksum);
        mockAuditTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    
    @Test
    public void testContentNotFoundFirstAttempt()
            throws Exception {
        task = createBitIntegrityCheckTask(0);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockNotFound();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        taskProcessorExecutionExpectedFailure();
    }
    
    @Test
    public void testContentNotFoundPenultimateAttempt()
            throws Exception {
        task = createBitIntegrityCheckTask(2);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockNotFound();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        this.taskProcessor = createTaskProcessor(storeType);
        long penultimateWait = 1000;
        this.taskProcessor.setPenultimateWaitMS(penultimateWait);

        replayAll();
        
        failAfterWait(penultimateWait);
    }

    @Test
    public void testContentNotFoundLastAttempt()
            throws Exception {
        task = createBitIntegrityCheckTask(3);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockNotFound();
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        this.taskProcessor = createTaskProcessor(storeType);
        bitLogStoreMockInvalid(storeType, null, null, checksum, checksum);
        mockBitErrorTaskPut();
        replayAll();
        
        this.taskProcessor.execute();
        
    }

    
    @Test
    public void testAuditAndContentDoNotMatchStoreFirstAttmpt()
            throws Exception {
        task = createBitIntegrityCheckTask(0);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        checksumUtilMockValid();
        contentIndexClientMockInvalidChecksum();
        auditLogStoreMockInvalidChecksum();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        taskProcessorExecutionExpectedFailure();
    }
    
    @Test
    public void testAuditAndContentDoNotMatchStorePenultimateAttempt()
            throws Exception {
        task = createBitIntegrityCheckTask(2);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        checksumUtilMockValid();
        contentIndexClientMockInvalidChecksum();
        auditLogStoreMockInvalidChecksum();
        this.taskProcessor = createTaskProcessor(storeType);
        long penultimateWait = 1000;
        this.taskProcessor.setPenultimateWaitMS(penultimateWait);

        replayAll();
        
        failAfterWait(penultimateWait);
    }

    @Test
    public void testAuditAndContentDoNotMatchStoreLastAttempt()
            throws Exception {
        task = createBitIntegrityCheckTask(3);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        checksumUtilMockValid();
        contentIndexClientMockInvalidChecksum();
        auditLogStoreMockInvalidChecksum();
        this.taskProcessor = createTaskProcessor(storeType);
        bitLogStoreMockInvalid(storeType, checksum,checksum, badChecksum, badChecksum);
        mockAuditTaskPut();
        replayAll();
        
        this.taskProcessor.execute();
        
    }

    @Test
    public void testNoRecordOfItem()
            throws Exception {
        task = createBitIntegrityCheckTask(0);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockNotFound();
        contentIndexClientMockItemNotFound();
        auditLogStoreMockChecksum(null);
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        
        this.taskProcessor.execute();
        
    }

    /**
     * @param penultimateWait
     */
    private void failAfterWait(long penultimateWait) {
        long before = System.currentTimeMillis();
        taskProcessorExecutionExpectedFailure();

        long after = System.currentTimeMillis();

        Assert.assertTrue(after - before >= penultimateWait);
    }

    /**
     * 
     */
    private void taskProcessorExecutionExpectedFailure() {
        try {
            this.taskProcessor.execute();
            Assert.fail("above invocations should have failed.");
        } catch (TaskExecutionFailedException ex) {
        }
    }

    /**
     * @throws AuditLogWriteFailedException
     * 
     */
    private void auditLogStoreMockUpdateProperties()
            throws AuditLogWriteFailedException {
        this.auditLogStore.updateProperties(EasyMock.isA(AuditLogItem.class),
                EasyMock.isA(String.class));
        EasyMock.expectLastCall();
    }

    /**
     * @throws ContentIndexClientValidationException
     * 
     */
    private void contentIndexMockSave()
            throws ContentIndexClientValidationException {
        EasyMock.expect(
                this.contentIndexClient.save(EasyMock
                        .isA(ContentIndexItem.class))).andReturn("testId");
    }

    /**
     * 
     */
    private void mockBitErrorTaskPut() {
        this.bitErrorQueue.put(EasyMock.isA(Task.class));
        EasyMock.expectLastCall();
    }

    private void mockAuditTaskPut() {
        this.auditQueue.put(EasyMock.isA(Task.class));
        EasyMock.expectLastCall();
    }

    /**
     * @param storeType
     * @param checkContent
     * @throws NotFoundException
     * @throws Exception
     * @throws TaskExecutionFailedException
     */
    private void testSuccess(StorageProviderType storeType, boolean checkContent)
            throws NotFoundException, Exception, TaskExecutionFailedException {
        storeMockValidChecksum();
        if (checkContent) {
            checksumUtilMockValid();
        }
        contentIndexClientMockValidChecksum();
        auditLogStoreMockValidChecksum();
        bitLogStoreMockValid(storeType);
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }
}
