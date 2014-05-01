/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import org.duracloud.common.queue.task.Task;
import org.duracloud.common.queue.task.TypedTask;

/**
 * @author Daniel Bernstein
 *	       Date: May 1, 2014
 */
public class BitIntegrityCheckReportTask extends TypedTask {

    @Override
    public Task writeTask() {
        Task task = super.writeTask();
        task.setType(Task.Type.BIT_REPORT);
        return task;
    }
 
}