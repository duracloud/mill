/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.common.storageprovider;

import org.duracloud.common.queue.task.SpaceCentricTypedTask;
import org.duracloud.common.queue.task.Task;

/**
 * @author Daniel Bernstein
 * Date: Feb 29, 2016
 */
public class StorageStatsTask extends SpaceCentricTypedTask {
    @Override
    public Task writeTask() {
        Task task = super.writeTask();
        task.setType(Task.Type.STORAGE_STATS);
        return task;
    }
}
