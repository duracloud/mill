/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.common.queue.task.SpaceCentricTypedTask;
import org.duracloud.common.queue.task.Task;

/**
 * @author Daniel Bernstein
 *	       Date: May 1, 2014
 */
public class BitIntegrityCheckReportTask extends SpaceCentricTypedTask {

    @Override
    public Task writeTask() {
        Task task = super.writeTask();
        task.setType(Task.Type.BIT_REPORT);
        return task;
    }
 
}
