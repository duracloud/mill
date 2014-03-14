/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.storage.aop.ContentMessage;


/**
 * This class is responsible for handling space deletion messages.
 * 
 * @author Daniel Bernstein 
 *         Date: Jan 7, 2013
 */
public class SpaceDeleteMessageListener extends MessageListenerBase {

    public SpaceDeleteMessageListener(TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager, String subdomain) {
        super(duplicationTaskQueue, duplicationPolicyManager, subdomain);
    }    
    
    /**
     * Receives message from jms listener.
     * @param message
     */
    @Override
    protected void onMessageImpl(ContentMessage message) {
        applyDuplicationPolicy(message);
    }
}
