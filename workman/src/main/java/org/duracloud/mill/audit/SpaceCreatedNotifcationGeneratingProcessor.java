/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.common.constant.Constants;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: Apr 11, 2014
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
            String storeId = task.getStoreId();
            String spaceId = task.getSpaceId();
            log.warn("New space notification: subdomain: {}, storeId: {}, spaceId: {}: ",
                     account, storeId, spaceId);

            newSpace(account,
                     task.getStoreId(),
                     task.getSpaceId(),
                     task.getDateTime(),
                     task.getUserId());
        } else {
            log.debug("This task {} is not a space creation task: it will be ignored.", task);
        }
    }

    private void newSpace(String subdomain,
                          String storeId,
                          String spaceId,
                          String datetime,
                          String username) {

        String domain = System.getProperty(ConfigConstants.DURACLOUD_SITE_DOMAIN);
        if (domain == null) {
            domain = Constants.DEFAULT_DOMAIN;
        }
        String host = subdomain + "." + domain;

        String subject = "New Space on " + host + ", provider " + storeId + ": " +
                         spaceId;
        StringBuilder body = new StringBuilder();

        body.append("A new space has been created!\n\n");
        body.append("Subdomain: https://" + host + "\n");
        body.append("Storage Provider Id: " + storeId + "\n");
        body.append("Space: " + spaceId + "\n");

        this.notificationManager.sendEmail(subject, body.toString());
    }

}
