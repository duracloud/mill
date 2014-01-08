/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.storage.aop.ContentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for handling space creation messages from durastore.
 * 
 * @author Daniel Bernstein 
 *         Date: Oct 30, 2013
 */
public class SpaceCreateMessageListener  implements MessageListener{
    private static Logger log = LoggerFactory
            .getLogger(SpaceCreateMessageListener.class);
    
    private String subdomain;
    private NotificationManager notificationManager;
    /**
     * 
     * @param duplicationTaskQueue
     * @param duplicationPolicyManager
     * @param subdomain
     */
    public SpaceCreateMessageListener(String subdomain, 
            NotificationManager notificationManager) {
       this.subdomain = subdomain;
       this.notificationManager = notificationManager;
    }

    /**
     * Receives message from jms listener.
     * @param message
     */
    public void onMessage(ContentMessage message) {
        log.debug("listener for {} received {}", subdomain, message);
        this.notificationManager.newSpace(this.subdomain, 
                message.getStoreId(),
                message.getSpaceId(), 
                message.getDatetime(),
                message.getUsername());
    }
}
