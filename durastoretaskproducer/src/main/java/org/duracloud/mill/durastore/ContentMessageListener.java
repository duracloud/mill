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
import org.duracloud.storage.aop.ContentMessage.ACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for delegating <code>ContentMessage</code> objects
 * received from DuraStore to the appropriate <code>TaskQueue</code>s.
 * 
 * @author Daniel Bernstein 
 *         Date: Oct 30, 2013
 */
public class ContentMessageListener extends MessageListenerBase {
    private static Logger log = LoggerFactory
            .getLogger(ContentMessageListener.class);

    
    public ContentMessageListener(TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager, String subdomain) {
        super(duplicationTaskQueue, duplicationPolicyManager, subdomain);
    }    
    
    /**
     * Receives message from jms listener.
     * @param message
     */
    @Override
    protected void onMessageImpl(ContentMessage message) {
        // if action is ingest
        if (isDuplicatable(message)) {
            applyDuplicationPolicy(message);
        }else{
            log.warn("This message {} is not duplicable: it will be ignored." );
        }
    }


    /**
     * @param contentMessage
     * @return
     */
    private boolean isDuplicatable(ContentMessage contentMessage) {
        if(contentMessage.getAction() == null){
            return false;
        }
        
        try {
            ACTION action = ACTION.valueOf(contentMessage.getAction());
            return (action.equals(ACTION.INGEST) || 
                    action.equals(ACTION.UPDATE) ||
                    action.equals(ACTION.COPY) ||
                    action.equals(ACTION.DELETE) 
            );
        } catch (IllegalArgumentException e) {
            log.warn(
                    "failed to get enum value of " + contentMessage.getAction()
                            + ": " + e.getMessage());
            return false;
        }
    }
}
