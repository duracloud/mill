/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorCreationFailedException;
import org.duracloud.mill.workman.TaskProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: Apr 11, 2014
 */
public abstract class AuditTaskProcessorFactory implements TaskProcessorFactory {

    private final Logger log = LoggerFactory.getLogger(AuditTaskProcessorFactory.class);

    /*
     * (non-Javadoc)
     *
     * @see
     * org.duracloud.mill.workman.TaskProcessorFactory#create(org.duracloud.
     * common.queue.task.Task)
     */
    @Override
    public final TaskProcessor create(Task task)
        throws TaskProcessorCreationFailedException {
        if (isSupported(task)) {
            log.debug("creating task processor for " + task);
            AuditTask auditTask = new AuditTask();
            auditTask.readTask(task);
            return createImpl(auditTask);
        }

        throw new TaskProcessorCreationFailedException("Task is not an Audit task");

    }

    /**
     * @param auditTask
     * @return
     */
    protected abstract TaskProcessor createImpl(AuditTask auditTask);

    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessorFactory#isSupported(org.duracloud.common.queue.task.Task)
     */
    @Override
    public boolean isSupported(Task task) {
        return task.getType().equals(Task.Type.AUDIT);
    }
}
