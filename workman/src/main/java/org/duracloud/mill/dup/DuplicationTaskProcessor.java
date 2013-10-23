/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.duracloud.mill.domain.DuplicationTask;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;

import java.util.Map;

/**
 * This class performs the Duplication Task
 * 
 * @author Bill Branan
 * 
 */
public class DuplicationTaskProcessor implements TaskProcessor {

    private DuplicationTask task;
    private StorageProvider sourceStore;
    private StorageProvider destStore;

    public DuplicationTaskProcessor(DuplicationTask task,
                                    StorageProvider sourceStore,
                                    StorageProvider destStore) {
        this.task = task;
        this.sourceStore = sourceStore;
        this.destStore = destStore;
    }

    @Override
    public void execute() throws TaskExecutionFailedException {
        // Read task
        String spaceId = task.getSpaceId();
        String contentId = task.getContentId();

        // Retrieve metadata for content item from both providers
        Map<String, String> sourceMetadata = null;
        try { // TODO: Add retry
            sourceMetadata =
                sourceStore.getContentProperties(spaceId, contentId);
        } catch(NotFoundException nfe) {
            sourceMetadata = null;
        }

        Map<String, String> destMetadata = null;
        try { // TODO: Add retry
            destMetadata =
                destStore.getContentProperties(spaceId, contentId);
        } catch(NotFoundException nfe) {
            destMetadata = null;
        }

        if(null != sourceMetadata) { // Item exists in source provider
            if(null != destMetadata) { // Item exists in dest provider
                // Item exists in both providers, compare checksums
                String srcChecksum = sourceMetadata.get(
                    StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
                String destChecksum = destMetadata.get(
                    StorageProvider.PROPERTIES_CONTENT_CHECKSUM);

                if(null != srcChecksum) {
                    if(srcChecksum.equals(destChecksum)) {
                        // Source and destination are equal, verify metadata
                        boolean metadataEqual =
                            compareMetadata(sourceMetadata, destMetadata);
                        if(!metadataEqual) {
                            // Metadata is not equal, duplicate the metadata
                            duplicateMetadata(spaceId,
                                              contentId,
                                              sourceMetadata);
                        }
                    } else {
                        // Source and destination content is not equal, duplicate
                        duplicateContent(spaceId, contentId);
                    }
                } else {
                    // Source item metadata has no checksum!
                    failTask("Source content item metadata " +
                             "included no checksum!");
                }
            } else { // Item in source but not in destination, duplicate
                duplicateContent(spaceId, contentId);
            }
        } else { // Item does not exist, it must have been deleted
            // Perform delete on destination
            duplicateDeletion(spaceId, contentId);
        }
    }

    /**
     * Determines if source and destination metadata are consistent
     *
     * @param sourceMetadata
     * @param destMetadata
     * @return
     */
    protected boolean compareMetadata(Map<String, String> sourceMetadata,
                                      Map<String, String> destMetadata) {
        // TODO implement
        return true;
    }

    /**
     * Copies the metadata from the source item to the destination item.
     *
     * @param spaceId
     * @param contentId
     * @param sourceMetadata
     */
    protected void duplicateMetadata(String spaceId,
                                     String contentId,
                                     Map<String, String> sourceMetadata) {
        // TODO implement
    }

    /**
     * Copies a content item from the source store to the destination store
     *
     * @param spaceId
     * @param contentId
     */
    protected void duplicateContent(String spaceId, String contentId) {
        // TODO implement
    }

    /**
     * Deletes a content item in the destination space
     *
     * @param spaceId
     * @param contentId
     */
    protected void duplicateDeletion(String spaceId, String contentId) {
        // TODO implement
    }

    protected void failTask(String message)
        throws DuplicationTaskExecutionFailedException {
        throw new DuplicationTaskExecutionFailedException(buildMessage(message));
    }

    protected void failTask(String message, Throwable cause)
        throws DuplicationTaskExecutionFailedException {
        throw new DuplicationTaskExecutionFailedException(buildMessage(message),
                                                          cause);
    }

    private String buildMessage(String message) {
        // TODO build message using task
        return message;
    }

}
