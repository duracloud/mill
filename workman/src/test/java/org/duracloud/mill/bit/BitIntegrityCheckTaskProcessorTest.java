/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.common.util.DateUtil;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.db.model.ManifestItem;
import org.duracloud.mill.manifest.ManifestItemWriteException;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskWorker;
import org.duracloud.storage.domain.RetrievedContent;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 * Date: 10/15/2014
 */
@RunWith(EasyMockRunner.class)
public class BitIntegrityCheckTaskProcessorTest extends EasyMockSupport {

    private static final String account = "account-id";
    private static final String storeId = "store-id";
    private static final String spaceId = "space-id";
    private static final String contentId = "content-id";
    private static final String user = "user";
    private static final String mimetype = "text/plain";
    private static final String contentSize = "10240";

    private static final String checksum = "checksum";
    private static final String badChecksum = "bad-checksum";

    private BitIntegrityCheckTask task;
    @Mock
    private StorageProvider store;
    @Mock
    private ManifestStore manifestStore;
    @Mock
    private ManifestItem manifestItem;

    @Mock
    private BitLogStore bitLogStore;
    @Mock
    private ContentChecksumHelper contentChecksumHelper;
    @Mock
    private TaskQueue bitErrorQueue;
    @Mock
    private TaskQueue auditQueue;
    @Mock
    private RetrievedContent retrievedContent;

    private BitIntegrityCheckTaskProcessor taskProcessor;

    private long penultimateWait = 1000;

    @Before
    public void setup() throws IOException {
        BitIntegrityCheckTaskProcessor.setPenultimateWaitMS(penultimateWait);
        task = createBitIntegrityCheckTask(1);
    }

    /**
     * @return
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
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        return createChecksumProps(checksum, c.getTime());
    }

    private Map<String, String> createChecksumProps(String checksum, Date modifiedDate) {
        Map<String, String> props = new HashMap<>();
        props.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        props.put(StorageProvider.PROPERTIES_CONTENT_SIZE, contentSize);
        props.put(StorageProvider.PROPERTIES_CONTENT_MIMETYPE, mimetype);
        props.put(StorageProvider.PROPERTIES_CONTENT_CREATOR, user);
        props.put(StorageProvider.PROPERTIES_CONTENT_MODIFIED,
                  DateUtil.convertToString(modifiedDate.getTime()));

        return props;
    }

    private void storeMockValidChecksum() {
        Map<String, String> props = createChecksumProps(checksum);
        storeMockGetProperties(props);
    }

    private void storeMockValidChecksum(Date modifiedDate) {
        Map<String, String> props = createChecksumProps(checksum, modifiedDate);
        storeMockGetProperties(props);
    }

    private void storeMockInvalidChecksum() {
        Map<String, String> props = createChecksumProps(badChecksum);
        storeMockGetProperties(props);
    }

    private void storeMockNotFound() {
        EasyMock.expect(store.getContentProperties(spaceId, contentId))
                .andThrow(new NotFoundException("test")).times(4);

    }

    /**
     * @param props
     */
    private void storeMockGetProperties(Map<String, String> props) {
        expect(store.getContentProperties(spaceId, contentId)).andReturn(props);
    }

    private void bitLogStoreMockInvalid(StorageProviderType storeType,
                                        String contentChecksum,
                                        String storeChecksum,
                                        String manifestChecksum) throws Exception {
        bitLogStoreMock(storeType,
                        contentChecksum,
                        storeChecksum,
                        manifestChecksum,
                        BitIntegrityResult.FAILURE);
    }

    private void bitLogStoreMock(StorageProviderType storeType,
                                 String contentChecksum,
                                 String storeChecksum,
                                 String manifestChecksum,
                                 BitIntegrityResult result) throws Exception {

        EasyMock.expect(bitLogStore.write(eq(account),
                                          eq(storeId),
                                          eq(spaceId),
                                          eq(contentId),
                                          isA(Date.class),
                                          eq(storeType),
                                          eq(result),
                                          isNullOrEq(contentChecksum),
                                          isNullOrEq(storeChecksum),
                                          isNullOrEq(manifestChecksum),
                                          isA(String.class)))
                .andReturn(EasyMock.createMock(BitLogItem.class));
    }

    /**
     * @param value
     * @return
     */
    private String isNullOrEq(String value) {
        return value == null ? isNull(String.class) : eq(value);
    }

    /**
     * @return
     */
    private InputStream storeMockInputstream() {
        InputStream is = EasyMock.createMock(InputStream.class);
        expect(retrievedContent.getContentStream()).andReturn(is);
        EasyMock.expect(store.getContent(spaceId, contentId)).andReturn(retrievedContent);
        return is;
    }

    //

    /**
     * @param storageProviderType
     * @return
     */
    private BitIntegrityCheckTaskProcessor createTaskProcessor(StorageProviderType storageProviderType) {
        return new BitIntegrityCheckTaskProcessor(task,
                                                  store,
                                                  manifestStore,
                                                  storageProviderType,
                                                  bitLogStore,
                                                  bitErrorQueue,
                                                  auditQueue,
                                                  contentChecksumHelper);
    }

    @Test
    public void testSuccessWithContentCheckS3() throws Exception {
        testSuccess(StorageProviderType.AMAZON_S3, true);
    }

    @Test
    public void testSuccessWithOutContentCheckGlacier() throws Exception {
        testSuccess(StorageProviderType.AMAZON_GLACIER, false);
    }

    @Test
    public void testSuccessWithOutContentCheckDpn() throws Exception {
        testSuccess(StorageProviderType.DPN, false);
    }

    @Test
    public void testSuccessWithOutContentCheckChronopolis() throws Exception {
        testSuccess(StorageProviderType.CHRONOPOLIS, false);
    }

    @Test
    public void testSuccessWithOutContentCheckIRODS() throws Exception {
        testSuccess(StorageProviderType.IRODS, false);
    }

    @Test
    public void testSourContent() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        mockManifestValidChecksum();
        mockGetContentChecksum(badChecksum);
        bitLogStoreMockInvalid(storeType, badChecksum, checksum, checksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    private void mockGetContentChecksum(String outputChecksum) throws TaskExecutionFailedException {
        expect(contentChecksumHelper.getContentChecksum(isA(String.class)))
            .andReturn(outputChecksum).atLeastOnce();
    }

    @Test
    public void testFailedStorageProviderChecksumFinalAttempt() throws Exception {
        this.task = createBitIntegrityCheckTask(TaskWorker.MAX_ATTEMPTS);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockInvalidChecksum();
        mockManifestValidChecksum();
        mockGetContentChecksum(checksum);

        bitLogStoreMockInvalid(storeType, checksum, badChecksum, checksum);
        mockBitErrorTaskPut();

        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testFailedManifestChecksumFinalAttempt() throws Exception {
        this.task = createBitIntegrityCheckTask(TaskWorker.MAX_ATTEMPTS);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        mockManifestInvalidChecksum();
        mockGetContentChecksum(checksum);

        expect(manifestStore.getItem(eq(account), eq(storeId), eq(spaceId), eq(contentId)))
            .andReturn(manifestItem);
        String mimetype = "plain/text";
        String size = "1000";
        expect(manifestItem.getContentMimetype()).andReturn(mimetype);
        expect(manifestItem.getContentSize()).andReturn(size);
        expect(manifestStore.addUpdate(eq(account), eq(storeId), eq(spaceId), eq(contentId), eq(checksum), eq(mimetype),
                                       eq(size), isA(Date.class))).andReturn(true);

        bitLogStoreMockInvalid(storeType, checksum, checksum, badChecksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testFailedManifestChecksumFirstAttempt() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        mockManifestInvalidChecksum();
        mockGetContentChecksum(checksum);
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        taskProcessorExecutionExpectedFailure();
    }

    @Test
    public void testFailedManifestChecksumPenultimateAttempt() throws Exception {
        this.task = createBitIntegrityCheckTask(TaskWorker.MAX_ATTEMPTS - 1);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        mockManifestInvalidChecksum();
        mockGetContentChecksum(checksum);
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        failAfterWait(penultimateWait);
    }

    @Test
    public void testFailedManifestChecksumDueToNullChecksumUltimateAttempt() throws Exception {
        this.task = createBitIntegrityCheckTask(TaskWorker.MAX_ATTEMPTS);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum();
        mockGetContentChecksum(checksum);
        mockManifestChecksumNull();
        mockManifestChecksumNull();
        mockManifestStoreUpdate(checksum);
        mockBitErrorTaskPut();
        bitLogStoreMockInvalid(storeType, checksum, checksum, null);
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testIgnoreFailedManifestChecksumUltimateAttempt() throws Exception {
        this.task = createBitIntegrityCheckTask(TaskWorker.MAX_ATTEMPTS);
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        storeMockValidChecksum(new Date());
        mockManifestChecksumNull();

        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testAllChecksumsMismatchedUltimateAttempt() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        int attempt = TaskWorker.MAX_ATTEMPTS;
        this.task = createBitIntegrityCheckTask(attempt);
        storeMockInvalidChecksum();
        mockManifestValidChecksum();
        String otherChecksum = "another-bad-checksum";
        mockGetContentChecksum(otherChecksum);
        bitLogStoreMockInvalid(storeType, otherChecksum, badChecksum, checksum);
        mockManifestUpdateNotNullManifest(badChecksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testAllChecksumsMismatchedUltimateAttemptGlacier() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_GLACIER;
        int attempt = TaskWorker.MAX_ATTEMPTS;
        this.task = createBitIntegrityCheckTask(attempt);
        storeMockInvalidChecksum();
        mockManifestValidChecksum();
        mockManifestUpdateNotNullManifest(badChecksum);

        bitLogStoreMockInvalid(storeType, null, badChecksum, checksum);
        mockBitErrorTaskPut();
        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    @Test
    public void testContentNotFound() throws Exception {
        StorageProviderType storeType = StorageProviderType.AMAZON_S3;
        int attempt = TaskWorker.MAX_ATTEMPTS;
        this.task = createBitIntegrityCheckTask(attempt);
        storeMockNotFound();
        mockManifestValidChecksum();
        bitLogStoreMockInvalid(storeType, null, null, checksum);
        mockBitErrorTaskPut();
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

        assertTrue(after - before >= penultimateWait);
    }

    /**
     *
     */
    private void taskProcessorExecutionExpectedFailure() {
        try {
            this.taskProcessor.execute();
            fail("above invocations should have failed.");
        } catch (TaskExecutionFailedException ex) {
            // Expected exception
        }
    }

    /**
     *
     */
    private void mockBitErrorTaskPut() {
        this.bitErrorQueue.put(isA(Task.class));
        EasyMock.expectLastCall().once();
    }

    /**
     * @param storeType
     * @param checkContent
     * @throws NotFoundException
     * @throws Exception
     * @throws TaskExecutionFailedException
     */
    private void testSuccess(StorageProviderType storeType, boolean checkContent) throws NotFoundException,
        Exception,
        TaskExecutionFailedException {
        mockManifestValidChecksum();

        storeMockValidChecksum();
        if (checkContent) {
            mockGetValidContentChecksum();
        }

        bitLogStoreMockValid(storeType, checkContent ? checksum : null, checksum);

        this.taskProcessor = createTaskProcessor(storeType);
        replayAll();
        this.taskProcessor.execute();
    }

    /**
     * @param storeType
     */
    private void bitLogStoreMockValid(StorageProviderType storeType,
                                      String contentChecksum,
                                      String storeChecksum) throws Exception {
        bitLogStoreMock(storeType,
                        contentChecksum,
                        storeChecksum,
                        storeChecksum,
                        BitIntegrityResult.SUCCESS);
    }

    private void mockGetValidContentChecksum() throws TaskExecutionFailedException {
        mockGetContentChecksum(checksum);
    }

    private void mockManifestValidChecksum() throws org.duracloud.common.db.error.NotFoundException {
        mockManifestChecksum(checksum);
    }

    private void mockManifestInvalidChecksum() throws org.duracloud.common.db.error.NotFoundException {
        mockManifestChecksum(badChecksum);
    }

    private void mockManifestChecksum(String checksum) throws org.duracloud.common.db.error.NotFoundException {
        expect(manifestStore.getItem(eq(account),
                                     eq(storeId),
                                     eq(spaceId),
                                     eq(contentId))).andReturn(manifestItem);
        expect(manifestItem.getContentChecksum()).andReturn(checksum);

    }

    private void mockManifestChecksumNull() throws org.duracloud.common.db.error.NotFoundException {
        expect(manifestStore.getItem(eq(account),
                                     eq(storeId),
                                     eq(spaceId),
                                     eq(contentId)))
            .andThrow(new org.duracloud.common.db.error.NotFoundException("test"));
    }

    private void mockManifestUpdateNotNullManifest(String checksum)
        throws org.duracloud.common.db.error.NotFoundException, ManifestItemWriteException {
        expect(manifestStore.getItem(eq(account),
                                     eq(storeId),
                                     eq(spaceId),
                                     eq(contentId))).andReturn(manifestItem);

        expect(manifestItem.getContentMimetype()).andReturn(mimetype);
        expect(manifestItem.getContentSize()).andReturn(contentSize);
        mockManifestStoreUpdate(checksum);
    }

    private void mockManifestStoreUpdate(String checksum) throws ManifestItemWriteException {
        expect(manifestStore.addUpdate(eq(account),
                                       eq(storeId),
                                       eq(spaceId),
                                       eq(contentId),
                                       eq(checksum),
                                       eq(mimetype),
                                       eq(contentSize),
                                       isA(Date.class))).andReturn(true);
    }
}