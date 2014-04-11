/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.mill.workman.TaskProcessor;

/**
 * @author Daniel Bernstein
 *	       Date: Mar 20, 2014
 */
public class ContentIndexUpdatingProcessorFactory extends AuditTaskProcessorFactory{

    private ContentIndexClient contentIndexClient;

    public ContentIndexUpdatingProcessorFactory(ContentIndexClient contentIndexClient) {
        super();
        this.contentIndexClient = contentIndexClient;
    }

    
    /* (non-Javadoc)
     * @see org.duracloud.mill.audit.AuditTaskProcessorFactory#createImpl(org.duracloud.audit.task.AuditTask)
     */
    @Override
    protected TaskProcessor createImpl(AuditTask auditTask) {
        return new ContentIndexUpdatingProcessor(auditTask, 
                contentIndexClient);
    }
}
