/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.mill.audit.AuditTaskProcessorFactory;
import org.duracloud.mill.workman.TaskProcessor;

/**
 * @author Daniel Bernstein
 *         Date: Sep 3, 2014
 */
public class ManifestWritingProcessorFactory extends
                                            AuditTaskProcessorFactory {

    private ManifestStore manifestStore;
    
    public ManifestWritingProcessorFactory(ManifestStore manifestStore){
        this.manifestStore = manifestStore;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.audit.AuditTaskProcessorFactory#createImpl(org.duracloud.audit.task.AuditTask)
     */
    @Override
    protected TaskProcessor createImpl(AuditTask auditTask) {
        return new ManifestWritingProcessor(auditTask, manifestStore);
    }
}
