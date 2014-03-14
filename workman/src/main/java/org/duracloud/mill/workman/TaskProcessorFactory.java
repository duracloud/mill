/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.common.queue.task.Task;

/**
 * The TaskProcessResolver resolves a <code>TaskProcessor</code> for a given
 * instance of a <code>Task</code>
 * 
 * @author Daniel Bernstein
 */
public interface TaskProcessorFactory {
    TaskProcessor create(Task task) throws TaskProcessorCreationFailedException;

}
