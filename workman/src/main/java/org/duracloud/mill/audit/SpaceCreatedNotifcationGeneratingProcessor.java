/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import java.util.HashSet;
import java.util.Set;

import org.duracloud.audit.task.AuditTask;
import static org.duracloud.audit.task.AuditTask.ActionType.*;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.task.DuplicationTask;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.aop.ContentMessage.ACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 11, 2014
 */
public class SpaceCreatedNotifcationGeneratingProcessor implements TaskProcessor {
    private static Logger log = LoggerFactory
            .getLogger(SpaceCreatedNotifcationGeneratingProcessor.class);

    private AuditTask task;
    private NotificationManager notificationManager;

    public SpaceCreatedNotifcationGeneratingProcessor(AuditTask task,
            NotificationManager notificationManager) {
        this.task = task;
        this.notificationManager = notificationManager;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessor#execute()
     */
    @Override
    public void execute() throws TaskExecutionFailedException {
        if (AuditTask.ActionType.CREATE_SPACE.name().equals(task.getAction())) {
            String account = task.getAccount();
            log.info("new space audit task received for account {} received: {}", account, task);
            this.notificationManager.newSpace(account,
                    task.getStoreId(),
                    task.getSpaceId(), 
                    task.getDateTime(),
                    task.getUserId());
        }else{
            log.debug("This task {} is not a space creation task: it will be ignored.", task);
        }
    }
    
   
}
