/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.workman.TaskProcessor;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 11, 2014
 */
public class SpaceCreatedNotifcationGeneratingProcessorFactory extends AuditTaskProcessorFactory {

    private NotificationManager notificationManager;

    public SpaceCreatedNotifcationGeneratingProcessorFactory(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.audit.AuditTaskProcessorFactory#createImpl(org.duracloud.audit.task.AuditTask)
     */
    @Override
    public TaskProcessor createImpl(AuditTask auditTask) {
        return new SpaceCreatedNotifcationGeneratingProcessor(auditTask,
                notificationManager);
 
    }
}
