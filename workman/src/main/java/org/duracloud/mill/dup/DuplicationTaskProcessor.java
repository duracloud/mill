/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.duracloud.mill.domain.DuplicationTask;
import org.duracloud.mill.util.Retriable;
import org.duracloud.mill.util.Retrier;
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

        // Retrieve properties for content items from both providers
        Map<String, String> sourceProperties =
            retryGetContentProperties(sourceStore, spaceId, contentId);
        Map<String, String> destProperties =
            retryGetContentProperties(destStore, spaceId, contentId);

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
                        duplicateContent(spaceId, contentId);
                    }
                } else {
                    // Source item properties has no checksum!
                    failTask("Source content item properties " +
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

    protected Map<String, String> retryGetContentProperties(final StorageProvider store,
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
        return null; // Should never actually be executed
    }

    /**
     * Determines if source and destination properties are consistent
     *
     * @param sourceProps
     * @param destProps
     * @return
     */
    protected boolean compareProperties(Map<String, String> sourceProps,
                                        Map<String, String> destProps) {
        // TODO implement
        return true;
    }

    /**
     * Copies the properties from the source item to the destination item.
     *
     * @param spaceId
     * @param contentId
     * @param sourceProperties
     */
    protected void duplicateProperties(String spaceId, String contentId,
                                       Map<String, String> sourceProperties) {
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
        throw new DuplicationTaskExecutionFailedException(
            buildFailureMessage(message));
    }

    protected void failTask(String message, Throwable cause)
        throws DuplicationTaskExecutionFailedException {
        throw new DuplicationTaskExecutionFailedException(
            buildFailureMessage(message), cause);
    }

    private String buildFailureMessage(String message) {
        StringBuilder builder = new StringBuilder();
        builder.append("Failure to duplicate content item due to:");
        builder.append(message);
        builder.append(" Account: ");
        builder.append(task.getAccount());
        builder.append(" Source StoreID: ");
        builder.append(task.getSourceStoreId());
        builder.append(" Destination StoreID: ");
        builder.append(task.getDestStoreId());
        builder.append(" SpaceID: ");
        builder.append(task.getSpaceId());
        builder.append(" ContentID: ");
        builder.append(task.getContentId());
        return builder.toString();
    }

}
