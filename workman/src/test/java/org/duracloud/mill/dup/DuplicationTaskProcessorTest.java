/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.common.util.IOUtil;
import org.duracloud.mill.task.DuplicationTask;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.error.StorageStateException;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Bill Branan
 *         Date: 10/25/13
 */
public class DuplicationTaskProcessorTest {

    private static final String account = "account-id";
    private static final String srcStoreId = "src-store-id";
    private static final String destStoreId = "dest-store-id";
    private static final String spaceId = "space-id";
    private static final String contentId = "content-id";

    private StorageProvider srcStore;
    private StorageProvider destStore;
    private DuplicationTaskProcessor taskProcessor;
    private File workDir;

    @Before
    public void setup() throws IOException {
        srcStore = EasyMock.createMock(StorageProvider.class);
        destStore = EasyMock.createMock(StorageProvider.class);

        DuplicationTask dupTask = new DuplicationTask();
        dupTask.setAccount(account);
        dupTask.setSourceStoreId(srcStoreId);
        dupTask.setDestStoreId(destStoreId);
        dupTask.setSpaceId(spaceId);
        dupTask.setContentId(contentId);

        workDir = new File("target", "dup-task-processor-test");
        workDir.mkdirs();
        workDir.deleteOnExit();

        taskProcessor = new DuplicationTaskProcessor(dupTask.writeTask(),
                                                     srcStore,
                                                     destStore,
                                                     workDir);
    }

    private void replayMocks() {
        EasyMock.replay(srcStore, destStore);
    }

    @After
    public void teardown() {
        EasyMock.verify(srcStore, destStore);
        workDir.delete();
    }

    @Test
    public void testCompareProperties() {
        Map<String, String> srcProps = new HashMap<>();
        Map<String, String> destProps = new HashMap<>();

        assertEquals("Empty props should be true (equal)",
                     true,
                     taskProcessor.compareProperties(srcProps, destProps));

        String oneKey = "one";
        String oneVal = "blue";
        srcProps.put(oneKey, oneVal);
        assertEquals("Prop in src, not in dest should be false (not equal)",
                     false,
                     taskProcessor.compareProperties(srcProps, destProps));

        destProps.put(oneKey, oneVal);
        assertEquals("Same prop in both should be true (equal)",
                     true,
                     taskProcessor.compareProperties(srcProps, destProps));

        String twoKey = "two";
        srcProps.put(twoKey, null);
        assertEquals("Prop in src (with null value), not in dest, " +
                      "should be false (not equal)",
                     false,
                     taskProcessor.compareProperties(srcProps, destProps));

        destProps.put(twoKey, null);
        assertEquals("Same props in both should be true (equal)",
                     true,
                     taskProcessor.compareProperties(srcProps, destProps));

        String threeKey = "three";
        String threeValA = "orange";
        srcProps.put(threeKey, threeValA);
        String threeValB = "purple";
        destProps.put(threeKey, threeValB);
        assertEquals("Different prop values should be false (not equal)", false,
                     taskProcessor.compareProperties(srcProps, destProps));

        destProps.put(threeKey, threeValA);
        assertEquals("Same props in both should be true (equal)",
                     true,
                     taskProcessor.compareProperties(srcProps, destProps));

        String fourKey = "four";
        String fourVal = "yellow";
        destProps.put(fourKey, fourVal);
        assertEquals("Extra props in destination should be false (not equal)",
                     false,
                     taskProcessor.compareProperties(srcProps, destProps));

        srcProps.put(fourKey, fourVal);
        assertEquals("Same props in both should be true (equal)",
                     true,
                     taskProcessor.compareProperties(srcProps, destProps));

        replayMocks();
    }

    /**
     * Verifies the flow of actions that occur when a content item is already
     * available in both the source and destination stores. No updates are
     * needed.
     *
     * @throws Exception on error
     */
    @Test
    public void testExecuteAvailableInBoth() throws Exception {
        // Check space
        destStore.createSpace(spaceId);
        EasyMock.expectLastCall();

        // Equal content properties
        final String checksum = "checksum";
        Map<String, String> srcProps = new HashMap<>();
        srcProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        EasyMock.expect(srcStore.getContentProperties(spaceId, contentId))
                .andReturn(srcProps);

        Map<String, String> destProps = new HashMap<>();
        destProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        EasyMock.expect(destStore.getContentProperties(spaceId, contentId))
                .andReturn(destProps);

        replayMocks();

        taskProcessor.execute();
    }

    /**
     * Verifies the flow of actions that occur when a content item exists in
     * neither the source nor destination stores. No updates are needed.
     *
     * @throws Exception on error
     */
    @Test
    public void testExecuteMissingInBoth() throws Exception {
        // Check space
        destStore.createSpace(spaceId);
        EasyMock.expectLastCall();

        // Missing content
        EasyMock.expect(srcStore.getContentProperties(spaceId, contentId))
                .andThrow(new NotFoundException("")).anyTimes();

        EasyMock.expect(destStore.getContentProperties(spaceId, contentId))
                .andThrow(new NotFoundException("")).anyTimes();

        replayMocks();

        taskProcessor.execute();
    }

    /**
     * Verifies the flow of actions that occur when the source space does not
     * exist. If the destination space exists, and is empty, it should be
     * removed
     *
     * @throws Exception on error
     */
    @Test
    public void testExecuteMissingSrcSpace() throws Exception {
        // Check source space
        EasyMock.expect(srcStore.getSpaceProperties(spaceId))
                .andThrow(new NotFoundException("")).anyTimes();

        // Check dest space
        EasyMock.expect(destStore.getSpaceProperties(spaceId))
                .andThrow(new NotFoundException(""))
                .andReturn(new HashMap<String, String>());

        // Check dest space, indicate that space exists but is empty
        EasyMock.expect(destStore.getSpaceContents(spaceId, null))
                .andReturn(new ArrayList<String>().iterator());

        // Delete space
        destStore.deleteSpace(spaceId);
        EasyMock.expectLastCall();

        replayMocks();

        // Change task to provide an empty contentId value
        DuplicationTask dupTask = new DuplicationTask();
        dupTask.setAccount(account);
        dupTask.setSourceStoreId(srcStoreId);
        dupTask.setDestStoreId(destStoreId);
        dupTask.setSpaceId(spaceId);
        dupTask.setContentId(null);

        taskProcessor = new DuplicationTaskProcessor(dupTask.writeTask(),
                                                     srcStore,
                                                     destStore,
                                                     workDir);

        taskProcessor.execute();
    }

    /**
     * Verifies the flow of actions that occur when an error occurs. In this
     * case, the error is prompted by the contentId and spaceId values being
     * null.
     *
     * @throws Exception on error
     */
    @Test
    public void testExecuteErrorInvalidInput() throws Exception {
        replayMocks();

        // Change task to provide an empty contentId value
        DuplicationTask dupTask = new DuplicationTask();
        dupTask.setAccount(account);
        dupTask.setSourceStoreId(srcStoreId);
        dupTask.setDestStoreId(destStoreId);
        dupTask.setSpaceId(null);
        dupTask.setContentId(null);

        taskProcessor = new DuplicationTaskProcessor(dupTask.writeTask(),
                                                     srcStore,
                                                     destStore,
                                                     workDir);

        try {
            taskProcessor.execute();
        } catch(DuplicationTaskExecutionFailedException e) {
            String msg = e.getMessage();
            assertTrue("Error message should contain account id",
                       msg.contains(account));
            assertTrue("Error message should contain account source store id",
                       msg.contains(srcStoreId));
            assertTrue("Error message should contain account dest store id",
                       msg.contains(destStoreId));
        }
    }

    /**
     * Verifies the flow of actions that occur when an error occurs. In this
     * case, the error is prompted by content properties being available but
     * with no checksum value. A task failure exception should be thrown.
     *
     * @throws Exception on error
     */
    @Test
    public void testExecuteErrorCase() throws Exception {
        // Check space
        destStore.createSpace(spaceId);
        EasyMock.expectLastCall();

        // Empty content properties
        Map<String, String> srcProps = new HashMap<>();
        EasyMock.expect(srcStore.getContentProperties(spaceId, contentId))
                .andReturn(srcProps);

        Map<String, String> destProps = new HashMap<>();
        EasyMock.expect(destStore.getContentProperties(spaceId, contentId))
                .andReturn(destProps);

        replayMocks();

        // Error thrown
        try {
            taskProcessor.execute();
        } catch(DuplicationTaskExecutionFailedException e) {
            String msg = e.getMessage();
            assertTrue("Error message should contain account id",
                       msg.contains(account));
            assertTrue("Error message should contain account source store id",
                       msg.contains(srcStoreId));
            assertTrue("Error message should contain account dest store id",
                       msg.contains(destStoreId));
            assertTrue("Error message should contain account space id",
                       msg.contains(spaceId));
            assertTrue("Error message should contain account content id",
                       msg.contains(contentId));
        }
    }

    /**
     * Verifies the flow of actions that occur when a content item is already
     * available in both the source and destination stores, but the properties
     * of the items do not match. The properties should be duplicated from
     * source to destination.
     *
     * @throws Exception on error
     */
    @Test
    public void testExecutePropertiesMismatch() throws Exception {
        // Check space
        destStore.createSpace(spaceId);
        EasyMock.expectLastCall();

        // Mismatched content properties
        final String checksum = "checksum";
        final String customPropKey = "important-information";
        final String customPropVal = "is-stored-here";
        Map<String, String> srcProps = new HashMap<>();
        srcProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        srcProps.put(customPropKey, customPropVal);
        EasyMock.expect(srcStore.getContentProperties(spaceId, contentId))
                .andReturn(srcProps);

        Map<String, String> destProps = new HashMap<>();
        destProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        EasyMock.expect(destStore.getContentProperties(spaceId, contentId))
                .andReturn(destProps);

        // Duplicate properties
        destStore.setContentProperties(spaceId, contentId, srcProps);
        EasyMock.expectLastCall();

        replayMocks();

        taskProcessor.execute();
    }

    /**
     * Tests that  property dates that cannot be made due to the store client throwing
     * a StorageStateException are handled gracefully.
     * @throws Exception
     */
    @Test
    public void testExecutePropertiesUpdateNotAllowed() throws Exception {
        // Check space
        destStore.createSpace(spaceId);
        EasyMock.expectLastCall();

        // Mismatched content properties
        final String checksum = "checksum";
        final String customPropKey = "important-information";
        final String customPropVal = "is-stored-here";
        Map<String, String> srcProps = new HashMap<>();
        srcProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        srcProps.put(customPropKey, customPropVal);
        EasyMock.expect(srcStore.getContentProperties(spaceId, contentId))
                .andReturn(srcProps);

        Map<String, String> destProps = new HashMap<>();
        destProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        EasyMock.expect(destStore.getContentProperties(spaceId, contentId))
                .andReturn(destProps);

        // Duplicate properties
        destStore.setContentProperties(spaceId, contentId, srcProps);
        EasyMock.expectLastCall().andThrow(new StorageStateException("test", null));

        replayMocks();

        taskProcessor.execute();
    }
    /**
     * Verifies the flow of actions that occur when a content item is not
     * available in the source store, but is available in the destination store.
     * The content should be removed from the destination store.
     *
     * @throws Exception on error
     */
    @Test
    public void testExecuteMissingInSource() throws Exception {
        // Check space
        destStore.createSpace(spaceId);
        EasyMock.expectLastCall();

        // Missing source content
        EasyMock.expect(srcStore.getContentProperties(spaceId, contentId))
                .andThrow(new NotFoundException("")).anyTimes();

        final String checksum = "checksum";
        Map<String, String> destProps = new HashMap<>();
        destProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        EasyMock.expect(destStore.getContentProperties(spaceId, contentId))
                .andReturn(destProps);

        // Delete content
        destStore.deleteContent(spaceId, contentId);
        EasyMock.expectLastCall();

        replayMocks();

        taskProcessor.execute();
    }

    /**
     * Verifies the flow of actions that occur when a content item is in the
     * source provider, but not the destination. The source content should be
     * duplicated to the destination.
     *
     * @throws Exception on error
     */
    @Test
    public void testExecuteMissingInDest() throws Exception {
        // Check space
        destStore.createSpace(EasyMock.eq(spaceId));
        EasyMock.expectLastCall();

        // Prepare source content
        String content = "source-content";
        ChecksumUtil checksumUtil = new ChecksumUtil(ChecksumUtil.Algorithm.MD5);
        final String checksum = checksumUtil.generateChecksum(content);

        // Source properties
        Map<String, String> srcProps = new HashMap<>();
        srcProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, checksum);
        final String mimetype = "text/plain";
        srcProps.put(StorageProvider.PROPERTIES_CONTENT_MIMETYPE, mimetype);
        final String customPropKey = "important-information";
        final String customPropVal = "is-stored-here";
        srcProps.put(customPropKey, customPropVal);
        EasyMock.expect(srcStore.getContentProperties(EasyMock.eq(spaceId),
                                                      EasyMock.eq(contentId)))
                .andReturn(srcProps);

        // Missing dest content
        EasyMock.expect(destStore.getContentProperties(EasyMock.eq(spaceId),
                                                       EasyMock.eq(contentId)))
                .andThrow(new NotFoundException("")).anyTimes();

        // Get source content
        InputStream contentStream = IOUtil.writeStringToStream(content);
        EasyMock.expect(srcStore.getContent(EasyMock.eq(spaceId),
                                            EasyMock.eq(contentId)))
                .andReturn(contentStream);

        // Add dest content
        EasyMock.expect(destStore.addContent(EasyMock.eq(spaceId),
                                             EasyMock.eq(contentId),
                                             EasyMock.eq(mimetype),
                                             EasyMock.eq(srcProps),
                                             EasyMock.eq((long)content.length()),
                                             EasyMock.eq(checksum),
                                             EasyMock.<InputStream>anyObject()))
                .andReturn(checksum);

        replayMocks();

        taskProcessor.execute();
    }

    /**
     * Verifies the flow of actions that occur when a content item in the source
     * provider has a checksum which does not match the checksum of the content
     * in the dest provider. The source content should be duplicated to the
     * destination.
     *
     * @throws Exception on error
     */
    @Test
    public void testExecuteChecksumMismatch() throws Exception {
        // Check space
        destStore.createSpace(EasyMock.eq(spaceId));
        EasyMock.expectLastCall();

        // Prepare source content
        String content = "source-content";
        ChecksumUtil checksumUtil = new ChecksumUtil(ChecksumUtil.Algorithm.MD5);
        final String srcChecksum = checksumUtil.generateChecksum(content);
        final String destChecksum = "checksum";
        final String mimetype = "text/plain";

        // Source properties
        Map<String, String> srcProps = new HashMap<>();
        srcProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, srcChecksum);
        srcProps.put(StorageProvider.PROPERTIES_CONTENT_MIMETYPE, mimetype);
        EasyMock.expect(srcStore.getContentProperties(EasyMock.eq(spaceId),
                                                      EasyMock.eq(contentId)))
                .andReturn(srcProps);

        // Dest properties (note different checksum value)
        Map<String, String> destProps = new HashMap<>();
        destProps.put(StorageProvider.PROPERTIES_CONTENT_CHECKSUM, destChecksum);
        destProps.put(StorageProvider.PROPERTIES_CONTENT_MIMETYPE, mimetype);
        EasyMock.expect(destStore.getContentProperties(EasyMock.eq(spaceId),
                                                       EasyMock.eq(contentId)))
                .andReturn(destProps);

        // Get source content
        InputStream contentStream = IOUtil.writeStringToStream(content);
        EasyMock.expect(srcStore.getContent(EasyMock.eq(spaceId),
                                            EasyMock.eq(contentId)))
                .andReturn(contentStream);

        // Add dest content
        EasyMock.expect(destStore.addContent(EasyMock.eq(spaceId),
                                             EasyMock.eq(contentId),
                                             EasyMock.eq(mimetype),
                                             EasyMock.eq(srcProps),
                                             EasyMock.eq((long)content.length()),
                                             EasyMock.eq(srcChecksum),
                                             EasyMock.<InputStream>anyObject()))
                .andReturn(srcChecksum);

        replayMocks();

        taskProcessor.execute();
    }

}
