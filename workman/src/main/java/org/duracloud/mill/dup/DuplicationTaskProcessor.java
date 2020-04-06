/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import static org.duracloud.common.util.ChecksumUtil.Algorithm.MD5;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.duracloud.common.retry.ExceptionHandler;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.mill.db.model.ManifestItem;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.task.DuplicationTask;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessorBase;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.error.StorageStateException;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class performs the Duplication Task
 *
 * @author Bill Branan
 */
public class DuplicationTaskProcessor extends TaskProcessorBase {

    private DuplicationTask dupTask;
    private StorageProvider sourceStore;
    private StorageProvider destStore;
    private File workDir;
    private ManifestStore manifestStore;

    private final Logger log =
        LoggerFactory.getLogger(DuplicationTaskProcessor.class);

    public DuplicationTaskProcessor(DuplicationTask dupTask,
                                    StorageProvider sourceStore,
                                    StorageProvider destStore,
                                    File workDir,
                                    ManifestStore manifestStore) {
        super(dupTask);
        this.dupTask = dupTask;
        this.sourceStore = sourceStore;
        this.destStore = destStore;
        this.workDir = workDir;
        this.manifestStore = manifestStore;
    }

    @Override
    protected void executeImpl() throws TaskExecutionFailedException {
        // Read task
        String spaceId = dupTask.getSpaceId();
        String contentId = dupTask.getContentId();

        // If space ID is missing, fail
        if (null == spaceId || spaceId.equals("")) {
            throw new DuplicationTaskExecutionFailedException(
                buildFailureMessage("SpaceId value is null or empty"));
        }

        // If content ID is missing, check on space
        if (null == contentId || contentId.equals("")) {
            if (spaceExists(sourceStore, spaceId)) {
                ensureDestSpaceExists(spaceId);
            } else { // Source space must have been deleted
                if (spaceExists(destStore, spaceId)) {
                    Iterator<String> contentItems =
                        getSpaceListing(destStore, spaceId);
                    if (!contentItems.hasNext()) { // List is empty
                        deleteDestSpace(spaceId);
                    } // If dest space is not empty, do not attempt a delete
                }
            }
            return; // With no content ID, nothing else can be done.
        }

        // Check destination space
        ensureDestSpaceExists(spaceId);

        // Retrieve properties for content items from both providers
        Map<String, String> sourceProperties =
            getContentProperties(sourceStore, spaceId, contentId);
        Map<String, String> destProperties =
            getContentProperties(destStore, spaceId, contentId);

        if (null != sourceProperties) { // Item exists in source provider
            String sourceChecksum = sourceProperties.get(
                StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
            cleanProperties(sourceProperties);

            if (null != destProperties) { // Item exists in dest provider
                String destChecksum = destProperties.get(
                    StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
                cleanProperties(destProperties);

                // Item exists in both providers, compare checksums
                if (null != sourceChecksum) {
                    if (sourceChecksum.equals(destChecksum)) {
                        // Source and destination checksums are equal
                        // Check to see if content properties are consistent
                        boolean propertiesEqual =
                            compareProperties(sourceProperties, destProperties);
                        if (!propertiesEqual) {
                            // Properties are not equal, duplicate the props
                            duplicateProperties(spaceId,
                                                contentId,
                                                sourceProperties);
                        } else {
                            // No updates needed
                            log.info("Duplication check complete, no updates " +
                                     "needed. Content id={} space={} account={}",
                                     contentId, spaceId, dupTask.getAccount());
                        }
                    } else {
                        // Source and destination content is not equal, duplicate
                        duplicateContent(spaceId,
                                         contentId,
                                         sourceChecksum,
                                         sourceProperties);
                    }
                } else {
                    // Source item properties has no checksum!
                    String msg = "Source content item properties " +
                                 "included no checksum!";
                    throw new DuplicationTaskExecutionFailedException(
                        buildFailureMessage(msg));
                }
            } else { // Item in source but not in destination, duplicate
                duplicateContent(spaceId,
                                 contentId,
                                 sourceChecksum,
                                 sourceProperties);
            }
        } else { // Item does not exist in source, it must have been deleted
            if (null != destProperties) { // Item does exist in dest
                // Perform delete on destination
                duplicateDeletion(spaceId, contentId);
            }
        }
    }

    /**
     * Determines if a space in the given store exists.
     *
     * @param store   the storage provider in which to check the space
     * @param spaceId space to check
     * @return true if space exists, false otherwise
     */
    private boolean spaceExists(final StorageProvider store,
                                final String spaceId)
        throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public Boolean retry() throws Exception {
                    // The actual method being executed
                    store.getSpaceProperties(spaceId);
                    return true;
                }
            });
        } catch (NotFoundException nfe) {
            return false;
        } catch (Exception e) {
            String msg = "Error attempting to check if space exists: " +
                         e.getMessage();
            throw new DuplicationTaskExecutionFailedException(
                buildFailureMessage(msg), e);
        }
    }

    /**
     * Ensures the destination space exists
     *
     * @param spaceId
     */
    private void ensureDestSpaceExists(final String spaceId) {
        try {
            destStore.createSpace(spaceId);
        } catch (Exception e) {
            // The space already exists
        }
    }

    /**
     * Retrieve the content listing for a space
     *
     * @param store   the storage provider in which the space exists
     * @param spaceId space from which to retrieve listing
     * @return
     */
    private Iterator<String> getSpaceListing(final StorageProvider store,
                                             final String spaceId)
        throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public Iterator<String> retry() throws Exception {
                    // The actual method being executed
                    return store.getSpaceContents(spaceId, null);
                }
            });
        } catch (Exception e) {
            String msg = "Error attempting to retrieve space listing: " +
                         e.getMessage();
            throw new DuplicationTaskExecutionFailedException(
                buildFailureMessage(msg), e);
        }
    }

    /**
     * Deletes a space from the destination store
     *
     * @param spaceId space to delete
     * @return
     */
    private void deleteDestSpace(final String spaceId)
        throws TaskExecutionFailedException {
        log.info("Deleting space " + spaceId +
                 " from dest provider in account " + dupTask.getAccount());
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // The actual method being executed
                    destStore.deleteSpace(spaceId);
                    return "success";
                }
            });
        } catch (Exception e) {
            String msg = "Error attempting to delete the destination space: " +
                         e.getMessage();
            throw new DuplicationTaskExecutionFailedException(
                buildFailureMessage(msg), e);
        }
        log.info("Successfully deleted space " + spaceId +
                 " from dest provider in account " + dupTask.getAccount());
    }

    /**
     * Retrieves the properties for the given content item
     *
     * @param store
     * @param spaceId
     * @param contentId
     * @return
     * @throws TaskExecutionFailedException
     */
    private Map<String, String> getContentProperties(final StorageProvider store,
                                                     final String spaceId,
                                                     final String contentId)
        throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public Map<String, String> retry() throws Exception {
                    // The actual method being executed
                    return store.getContentProperties(spaceId, contentId);
                }
            }, new ExceptionHandler() {
                @Override
                public void handle(Exception ex) {
                    if (!(ex instanceof NotFoundException)) {
                        log.debug(ex.getMessage(), ex);
                    } else {
                        log.debug("retry attempt failed but probably not an issue: {}", ex.getMessage());
                    }
                }
            });
        } catch (NotFoundException nfe) {
            return null;
        } catch (Exception e) {
            String msg = "Error attempting to retrieve content properties: " + e.getMessage();
            throw new DuplicationTaskExecutionFailedException(buildFailureMessage(msg), e);
        }
    }

    /**
     * Determines if source and destination properties are equal.
     *
     * @param sourceProps properties from the source content item
     * @param destProps   properties from the destination content item
     * @return true if all properties match
     */
    protected boolean compareProperties(Map<String, String> sourceProps,
                                        Map<String, String> destProps) {
        return sourceProps.equals(destProps);
    }

    /**
     * Copies the properties from the source item to the destination item.
     *
     * @param spaceId
     * @param contentId
     * @param sourceProperties
     */
    private void duplicateProperties(final String spaceId,
                                     final String contentId, final Map<String, String> sourceProperties)
        throws TaskExecutionFailedException {
        log.info("Duplicating properties for " + contentId + " in space "
                 + spaceId + " in account " + dupTask.getAccount());

        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Set properties
                    try {
                        destStore.setContentProperties(spaceId, contentId, sourceProperties);
                    } catch (StorageStateException ex) {
                        String message = "Unable to set content properties" +
                                         " on destination store ({0}) for " +
                                         "{1} (content) in {2} (space)";
                        log.warn(MessageFormat.format(message, destStore, contentId, spaceId));
                    }

                    return "success";

                }
            });

            log.info("Successfully duplicated properties for " + contentId
                     + " in space " + spaceId + " in account "
                     + dupTask.getAccount());

        } catch (Exception e) {
            String msg = "Error attempting to duplicate content properties: " + e.getMessage();
            throw new DuplicationTaskExecutionFailedException(
                buildFailureMessage(msg), e);
        }
    }

    /**
     * Pull out the system-generated properties, to allow the properties that
     * are added to the duplicated item to be only the user-defined properties.
     *
     * @param props
     */
    private void cleanProperties(Map<String, String> props) {
        if (props != null) {
            props.remove(StorageProvider.PROPERTIES_CONTENT_MD5);
            props.remove(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
            props.remove(StorageProvider.PROPERTIES_CONTENT_MODIFIED);
            props.remove(StorageProvider.PROPERTIES_CONTENT_SIZE);
            props.remove(HttpHeaders.CONTENT_LENGTH);
            props.remove(HttpHeaders.CONTENT_TYPE);
            props.remove(HttpHeaders.LAST_MODIFIED);
            props.remove(HttpHeaders.DATE);
            props.remove(HttpHeaders.ETAG);
            props.remove(HttpHeaders.CONTENT_LENGTH.toLowerCase());
            props.remove(HttpHeaders.CONTENT_TYPE.toLowerCase());
            props.remove(HttpHeaders.LAST_MODIFIED.toLowerCase());
            props.remove(HttpHeaders.DATE.toLowerCase());
            props.remove(HttpHeaders.ETAG.toLowerCase());
        }
    }

    /**
     * Copies a content item from the source store to the destination store
     *
     * @param spaceId
     * @param contentId
     */
    private void duplicateContent(final String spaceId,
                                  final String contentId,
                                  final String sourceChecksum,
                                  final Map<String, String> sourceProperties)
        throws TaskExecutionFailedException {
        log.info("Duplicating " + contentId + " in space " + spaceId +
                 " in account " + dupTask.getAccount());

        ChecksumUtil checksumUtil = new ChecksumUtil(MD5);
        boolean localChecksumMatch = false;
        int attempt = 0;

        File localFile = null;
        while (!localChecksumMatch && attempt < 3) {
            // Get content stream
            try (InputStream sourceStream = getSourceContent(spaceId, contentId)) {
                // Cache content locally
                localFile = cacheContent(sourceStream);
                // Check content
                String localChecksum = checksumUtil.generateChecksum(localFile);
                if (sourceChecksum.equals(localChecksum)) {
                    localChecksumMatch = true;
                } else {
                    cleanup(localFile);
                }
            } catch (Exception e) {
                log.warn("Error generating checksum for source content: " + e.getMessage(), e);
            }
            attempt++;
        }

        // Put content
        if (localChecksumMatch) {
            putDestinationContent(spaceId,
                                  contentId,
                                  sourceChecksum,
                                  sourceProperties,
                                  localFile);
            log.info(
                "Successfully duplicated id={} dup_size={} space={} account={}",
                contentId,
                localFile.length(),
                spaceId, dupTask.getAccount());
        } else {
            cleanup(localFile);
            String msg = "Unable to retrieve content which matches the" +
                         " expected source checksum of: " + sourceChecksum;
            throw new DuplicationTaskExecutionFailedException(buildFailureMessage(msg));
        }
        cleanup(localFile);
    }

    /*
     * Gets content item from source storage provider
     */
    private InputStream getSourceContent(final String spaceId,
                                         final String contentId)
        throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public InputStream retry() throws Exception {
                    // Retrieve from source
                    return sourceStore.getContent(spaceId, contentId).getContentStream();
                }
            });
        } catch (Exception e) {
            String msg = "Error attempting to get source content: " + e.getMessage();
            throw new DuplicationTaskExecutionFailedException(buildFailureMessage(msg), e);
        }
    }

    /*
     * Stores a stream as a file on the local file system
     */
    private File cacheContent(InputStream inStream)
        throws TaskExecutionFailedException {
        File localFile = null;
        try {
            localFile = File.createTempFile("content-item", ".tmp", workDir);
            try (OutputStream outStream = FileUtils.openOutputStream(localFile)) {
                IOUtils.copy(inStream, outStream);
            }
            inStream.close();
        } catch (IOException e) {

            if (localFile != null) {
                cleanup(localFile);
            }

            String msg = "Unable to cache content file due to: " + e.getMessage();
            throw new DuplicationTaskExecutionFailedException(buildFailureMessage(msg), e);
        }
        return localFile;
    }

    private void putDestinationContent(final String spaceId,
                                       final String contentId,
                                       final String sourceChecksum,
                                       final Map<String, String> sourceProperties,
                                       final File file)
        throws TaskExecutionFailedException {
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    String srcMimetype = sourceProperties.get(
                        StorageProvider.PROPERTIES_CONTENT_MIMETYPE);

                    // Push to destination
                    try (InputStream destStream = FileUtils.openInputStream(file)) {
                        String destChecksum =
                            destStore.addContent(spaceId,
                                                 contentId,
                                                 srcMimetype,
                                                 sourceProperties,
                                                 file.length(),
                                                 sourceChecksum,
                                                 destStream);
                        if (sourceChecksum.equals(destChecksum)) {
                            return "success";
                        } else {
                            throw new RuntimeException("Checksum in dest " +
                                                       "does not match source");
                        }
                    }
                }
            });
        } catch (Exception e) {
            cleanup(file);
            String msg = "Error attempting to add destination content: " + e.getMessage();
            throw new DuplicationTaskExecutionFailedException(buildFailureMessage(msg), e);
        }
    }

    private void cleanup(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            log.info("Unable to delete temp file: " + file.getAbsolutePath() +
                     " due to: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a content item in the destination space, but only if it does not exists in the
     * source manifest.
     *
     * @param spaceId
     * @param contentId
     */
    private void duplicateDeletion(final String spaceId,
                                   final String contentId)
        throws TaskExecutionFailedException {

        if (existsInSourceManifest(spaceId, contentId)) {
            throw new TaskExecutionFailedException(
                MessageFormat.format("item exists in source manifest and thus appears to be " +
                                     "missing content.  account={0}, storeId={1}, spaceId={2}, contentId={3}",
                                     this.dupTask.getAccount(),
                                     this.dupTask.getSourceStoreId(),
                                     spaceId,
                                     contentId));
        }

        log.info("Duplicating deletion of " + contentId + " in dest space " +
                 spaceId + " in account " + dupTask.getAccount());
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Delete content
                    destStore.deleteContent(spaceId, contentId);
                    return "success";
                }
            });
        } catch (Exception e) {
            String msg = "Error attempting to delete content : " + e.getMessage();
            throw new DuplicationTaskExecutionFailedException(buildFailureMessage(msg), e);
        }

        log.info("Successfully deleted content item (content_id=" + contentId +
                 ") in dest space (space_id=" + spaceId + ") where account_id=" +
                 dupTask.getAccount());
    }

    /**
     * @param spaceId
     * @param contentId
     */
    private boolean existsInSourceManifest(String spaceId, String contentId) {
        // if the source store's manifest contains an entry for this item
        // it indicates that the content item is missing from the provider
        // and thus the copy should not be deleted and an error should be raised.
        // If there is a temporary backup in the audit log, then this issue should
        // be resolved by the automatic retry mechanism.
        String sourceStoreId = this.dupTask.getSourceStoreId();
        String account = this.dupTask.getAccount();

        try {
            ManifestItem item = this.manifestStore.getItem(account, sourceStoreId, spaceId, contentId);
            if (!item.isDeleted()) {
                return true;
            }
        } catch (org.duracloud.common.db.error.NotFoundException e1) {
            // Indicates that the item does not exist, so fall through to returning false
        }

        return false;

    }

    private String buildFailureMessage(String message) {
        StringBuilder builder = new StringBuilder();
        builder.append("Failure to duplicate content item due to:");
        builder.append(message);
        builder.append(" Account: ");
        builder.append(dupTask.getAccount());
        builder.append(" Source StoreID: ");
        builder.append(dupTask.getStoreId());
        builder.append(" Destination StoreID: ");
        builder.append(dupTask.getDestStoreId());
        builder.append(" SpaceID: ");
        builder.append(dupTask.getSpaceId());
        builder.append(" ContentID: ");
        builder.append(dupTask.getContentId());
        return builder.toString();
    }
}