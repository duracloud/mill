/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;

/**
 * A <code>TaskWorker</code> factory.
 * 
 * @author Daniel Bernstein Date: Dec 6, 2013
 */
public interface TaskWorkerFactory {
    /**
     * Creates a task worker for the specified task.
     * 
     * @param task
     * @param taskQueue
     * @return
     */
    TaskWorker create(Task task, TaskQueue taskQueue);

}
