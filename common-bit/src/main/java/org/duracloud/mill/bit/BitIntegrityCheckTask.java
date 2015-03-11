/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.common.queue.task.Task;
import org.duracloud.common.queue.task.TypedTask;

/**
 * Provides the information necessary to complete a bit integrity check on a
 * content item.
 * 
 * @author Daniel Bernstein 
 *         Date: 04/22/2014
 */
public class BitIntegrityCheckTask extends TypedTask {

    @Override
    public Task writeTask() {
        Task task = super.writeTask();
        task.setType(Task.Type.BIT);
        return task;
    }
 
}
