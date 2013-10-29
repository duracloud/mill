/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.mill.domain.DuplicationTask;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.util.Retriable;
import org.duracloud.mill.util.Retrier;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.s3storage.S3StorageProvider;
import org.duracloud.sdscstorage.SDSCStorageProvider;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static org.duracloud.common.util.ChecksumUtil.Algorithm.MD5;

/**
 * This class performs the Duplication Task
 * 
 * @author Bill Branan
 * 
 */
public class DuplicationTaskProcessor implements TaskProcessor {

    private DuplicationTask dupTask;
    private StorageProvider sourceStore;
    private StorageProvider destStore;

    private final Logger log =
        LoggerFactory.getLogger(DuplicationTaskProcessor.class);

    public DuplicationTaskProcessor(Task task,
                                    StorageProvider sourceStore,
                                    StorageProvider destStore) {
        this.dupTask = new DuplicationTask();
        this.dupTask.readTask(task);
        this.sourceStore = sourceStore;
        this.destStore = destStore;
    }

    protected DuplicationTaskProcessor(DuplicationTask dupTask,
                                       StorageProvider sourceStore,
                                       StorageProvider destStore) {
        this.dupTask = dupTask;
        this.sourceStore = sourceStore;
        this.destStore = destStore;
    }

    @Override
    public void execute() throws TaskExecutionFailedException {
        // Read task
        String spaceId = dupTask.getSpaceId();
        String contentId = dupTask.getContentId();

        // Check destination space
        checkSpace(spaceId);

        // Retrieve properties for content items from both providers
        Map<String, String> sourceProperties =
            getContentProperties(sourceStore, spaceId, contentId);
        Map<String, String> destProperties =
            getContentProperties(destStore, spaceId, contentId);

        if(null != sourceProperties) { // Item exists in source provider
            if(null != destProperties) { // Item exists in dest provider
                // Item exists in both providers, compare checksums
                String srcChecksum = sourceProperties.get(
                    StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
                String destChecksum = destProperties.get(
                    StorageProvider.PROPERTIES_CONTENT_CHECKSUM);

                if(null != srcChecksum) {
                    if(srcChecksum.equals(destChecksum)) {
                        // Source and destination checksums are equal
                        // Check to see if content properties are consistent
                        boolean propertiesEqual =
                            compareProperties(sourceProperties, destProperties);
                        if(!propertiesEqual) {
                            // Properties are not equal, duplicate the props
                            duplicateProperties(spaceId, contentId,
                                                sourceProperties);
                        }
                    } else {
                        // Source and destination content is not equal, duplicate
                        duplicateContent(spaceId, contentId, sourceProperties);
                    }
                } else {
                    // Source item properties has no checksum!
                    failTask("Source content item properties " +
                             "included no checksum!");
                }
            } else { // Item in source but not in destination, duplicate
                duplicateContent(spaceId, contentId, sourceProperties);
            }
        } else { // Item does not exist in source, it must have been deleted
            if(null != destProperties) { // Item does exist in dest
                // Perform delete on destination
                duplicateDeletion(spaceId, contentId);
            }
        }
    }

    /**
     * Ensures the destination space exists
     *
     * @param spaceId
     */
    private void checkSpace(final String spaceId) {
        try {
            destStore.createSpace(spaceId);
        } catch(Exception e) {
            // The space already exists
        }
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
            });
        } catch(NotFoundException nfe) {
            return null;
        } catch(Exception e) {
            failTask("Error attempting to retrieve content properties: " +
                     e.getMessage(), e);
        }

        log.error("Reached unexpected code path in getContentProperties()!");
        return null; // Should never actually be executed
    }

    /**
     * Determines if source and destination properties are consistent.
     * This consistentcy check just ensures that all properties stored in the
     * source are also stored in the destination. Additional properties in
     * the destination will not cause this check to fail.
     *
     * @param sourceProps properties from the source content item
     * @param destProps properties from the destination content item
     * @return true if all properties from source are available in dest
     */
    protected boolean compareProperties(Map<String, String> sourceProps,
                                        Map<String, String> destProps) {
        for(String sourceKey : sourceProps.keySet()) { // Check all source keys
            if(destProps.containsKey(sourceKey)) {
                String sourceVal = sourceProps.get(sourceKey);
                String destVal = destProps.get(sourceKey);
                if(null == sourceVal) {
                    if(null != destVal) {
                        return false; // Source value is empty and dest is not
                    }
                } else if(!sourceVal.equals(destVal)) {
                    return false; // Source and dest values are not equal
                }
            } else {
                return false; // Dest does not include key
            }
        }
        return true; // All items in source match in destination
    }

    /**
     * Copies the properties from the source item to the destination item.
     *
     * @param spaceId
     * @param contentId
     * @param sourceProperties
     */
    private void duplicateProperties(final String spaceId,
                                     final String contentId,
                                     final Map<String, String> sourceProperties)
        throws TaskExecutionFailedException {
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Set properties
                    destStore.setContentProperties(spaceId,
                                                   contentId,
                                                   sourceProperties);
                    return "success";
                }
            });
        } catch(Exception e) {
            failTask("Error attempting to duplicate content properties : " +
                     e.getMessage(), e);
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
                                  final Map<String, String> sourceProperties)
        throws TaskExecutionFailedException {

        String srcChecksum = sourceProperties.get(
            StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
        ChecksumUtil checksumUtil = new ChecksumUtil(MD5);
        boolean localChecksumMatch = false;
        int attempt = 0;

        File localFile = null;
        while (!localChecksumMatch && attempt < 3) {
            // Get content stream
            InputStream sourceStream = getSourceContent(spaceId, contentId);

            // Cache content locally
            localFile = cacheContent(sourceStream);

            // Check content
            try {
                String localChecksum = checksumUtil.generateChecksum(localFile);
                if(srcChecksum.equals(localChecksum)) {
                    localChecksumMatch = true;
                } else {
                    cleanup(localFile);
                }
            } catch(IOException e) {
                log.warn("Error generating checksum for source content: " +
                         e.getMessage(), e);
            }
            attempt++;
        }

        // Put content
        if(localChecksumMatch) {
            putDestinationContent(spaceId,
                                  contentId,
                                  srcChecksum,
                                  sourceProperties,
                                  localFile);
        } else {
            cleanup(localFile);
            failTask("Unable to retrieve content which matches the expected " +
                     "source checksum of: " + srcChecksum);
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
                    return sourceStore.getContent(spaceId, contentId);
                }
            });
        } catch(Exception e) {
            failTask("Error attempting to duplicate content properties : " +
                     e.getMessage(), e);
        }

        log.error("Reached unexpected code path in getSourceContent()!");
        return null; // Should never actually be executed
    }

    /*
     * Stores a stream as a file on the local file system
     */
    private File cacheContent(InputStream inStream)
        throws TaskExecutionFailedException {
        File localFile = null;
        try {
            // TODO: Allow for temp files to be stored in a preferred location
            localFile = File.createTempFile("content-item", ".tmp");
            try(OutputStream outStream = FileUtils.openOutputStream(localFile)) {
                IOUtils.copy(inStream, outStream);
            }
            inStream.close();
        } catch(IOException e) {
            failTask("Unable to cache content file due to: " +
                     e.getMessage(), e);
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
                    try(InputStream destStream = FileUtils.openInputStream(file)) {
                        String destChecksum =
                            destStore.addContent(spaceId,
                                                 contentId,
                                                 srcMimetype,
                                                 sourceProperties,
                                                 file.length(),
                                                 sourceChecksum,
                                                 destStream);
                        if(sourceChecksum.equals(destChecksum)) {
                            return "success";
                        } else {
                            throw new RuntimeException("Checksum in dest does not " +
                                                       "match source");
                        }
                    }
                }
            });
        } catch(Exception e) {
            cleanup(file);
            failTask("Error attempting to duplicate content properties : " +
                     e.getMessage(), e);
        }
    }

    private void cleanup(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch(IOException e) {
            log.info("Unable to delete temp file: " + file.getAbsolutePath() +
                     " due to: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a content item in the destination space
     *
     * @param spaceId
     * @param contentId
     */
    private void duplicateDeletion(final String spaceId,
                                   final String contentId)
        throws TaskExecutionFailedException {
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Delete content
                    destStore.deleteContent(spaceId, contentId);
                    return "success";
                }
            });
        } catch(Exception e) {
            failTask("Error attempting to delete content : " +
                     e.getMessage(), e);
        }
    }

    private void failTask(String message)
        throws DuplicationTaskExecutionFailedException {
        throw new DuplicationTaskExecutionFailedException(
            buildFailureMessage(message));
    }

    private void failTask(String message, Throwable cause)
        throws DuplicationTaskExecutionFailedException {
        throw new DuplicationTaskExecutionFailedException(
            buildFailureMessage(message), cause);
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

    /**
     * Allows for some simple testing of this class
     */
    public static void main(String[] args) throws Exception {
        if(args.length != 6) {
            throw new RuntimeException("6 arguments expected:\n " +
                "spaceId\n contentId\n sourceProviderUsername\n " +
                "sourceProviderPassword\n destProviderUsername\n " +
                "destProviderPassword");
        }

        String spaceId = args[0];
        String contentId = args[1];
        String srcProvCredUser = args[2];
        String srcProvCredPass = args[3];
        String destProvCredUser = args[4];
        String destProvCredPass = args[5];

        System.out.println("Performing duplication check for content item " +
                           contentId + " in space " + spaceId);

        DuplicationTask task = new DuplicationTask();
        task.setAccount("Dup Testing");
        task.setSpaceId(spaceId);
        task.setContentId(contentId);

        StorageProvider srcProvider =
            new S3StorageProvider(srcProvCredUser, srcProvCredPass);
        // Making an assumption here that the secondary provider is SDSC
        StorageProvider destProvider =
            new SDSCStorageProvider(destProvCredUser, destProvCredPass);

        DuplicationTaskProcessor dupProcessor =
            new DuplicationTaskProcessor(task, srcProvider, destProvider);
        dupProcessor.execute();

        System.out.println("Duplication check completed successfully!");
    }

}
