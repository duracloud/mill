/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.client.contentindex.ContentIndexClient;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorCreationFailedException;
import org.duracloud.mill.workman.TaskProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: Mar 20, 2014
 */
public class AuditTaskProcessorFactory implements TaskProcessorFactory{

    private final Logger log =
            LoggerFactory.getLogger(AuditTaskProcessorFactory.class);

    private AuditLogStore      auditLogStore;
    private ContentIndexClient contentIndexClient;
 
    public AuditTaskProcessorFactory(ContentIndexClient contentIndexClient,
                                     AuditLogStore auditLogStore) {
        this.contentIndexClient = contentIndexClient;
        this.auditLogStore = auditLogStore;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessorFactory#create(org.duracloud.common.queue.task.Task)
     */
    @Override
    public TaskProcessor create(Task task)
            throws TaskProcessorCreationFailedException {
        if(task.getType().equals(Task.Type.AUDIT)){
            log.debug("creating audit task processor for " + task);
            AuditTask auditTask = new AuditTask();
            auditTask.readTask(task);
            return new AuditTaskProcessor(  auditTask, 
                                            contentIndexClient,
                                            auditLogStore);
        }
        
        throw new TaskProcessorCreationFailedException("Task is not an Audit task");
    }
}
