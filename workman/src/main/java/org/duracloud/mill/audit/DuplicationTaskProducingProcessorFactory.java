/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.workman.TaskProcessor;

/**
 * @author Daniel Bernstein
 *	       Date: Mar 20, 2014
 */
public class DuplicationTaskProducingProcessorFactory extends AuditTaskProcessorFactory{

    private TaskQueue duplicationTaskQueue;
    private DuplicationPolicyManager duplicationPolicyManager;

    /**
     * @param duplicationTaskQueue
     * @param duplicationPolicyManager
     */
    public DuplicationTaskProducingProcessorFactory(TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager) {
        super();
        this.duplicationTaskQueue = duplicationTaskQueue;
        this.duplicationPolicyManager = duplicationPolicyManager;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.audit.AuditTaskProcessorFactory#createImpl(org.duracloud.audit.task.AuditTask)
     */
    @Override
    protected TaskProcessor createImpl(AuditTask auditTask) {
        return new DuplicationTaskProducingProcessor(auditTask,
                duplicationTaskQueue, 
                duplicationPolicyManager);
    }
}
