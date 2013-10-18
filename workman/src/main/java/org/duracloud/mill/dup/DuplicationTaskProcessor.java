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

        // If item exists in both providers compare checksums

        // If item exists in source but not destination (or exists in both but
        // checksums do not match), perform copy

        // If item exists in dest but not in source, perform delete
    }
}
