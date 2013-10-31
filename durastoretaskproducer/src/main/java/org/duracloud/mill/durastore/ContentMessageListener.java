/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import java.util.HashSet;
import java.util.Set;

import org.duracloud.mill.domain.DuplicationTask;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.storage.aop.ContentCopyMessage;
import org.duracloud.storage.aop.ContentMessage;
import org.duracloud.storage.aop.ContentMessage.ACTION;
import org.duracloud.storage.aop.IngestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for delegating <code>ContentMessage</code> objects
 * received from DuraStore to the appropriate <code>TaskQueue</code>s.
 * 
 * @author Daniel Bernstein 
 *         Date: Oct 30, 2013
 */
public class ContentMessageListener {
    private static Logger log = LoggerFactory
            .getLogger(ContentMessageListener.class);
    private TaskQueue duplicationTaskQueue;
    private DuplicationPolicyManager duplicationPolicyManager;
    private String subdomain;
    private DuplicationPolicy policy;

    /**
     * 
     * @param duplicationTaskQueue
     * @param duplicationPolicyManager
     * @param subdomain
     */
    public ContentMessageListener(TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager, String subdomain) {
        this.duplicationTaskQueue = duplicationTaskQueue;
        this.duplicationPolicyManager = duplicationPolicyManager;
        this.subdomain = subdomain;
    }

    /**
     * Receives message from jms listener.
     * @param message
     */
    public void onMessage(ContentMessage message) {
        log.debug("listener for {} received {}", subdomain, message);
        // if action is ingest
        if (isDuplicatable(message)) {

            if (policy == null) {
                policy = duplicationPolicyManager
                        .getDuplicationPolicy(subdomain);
                if (policy == null) {
                    throw new RuntimeException("no policy found for subdomain "
                            + subdomain);
                }
            }
            applyDuplicationPolicy(message);
        }
    }

    /**
     * @param contentMessage
     */
    private void applyDuplicationPolicy(ContentMessage contentMessage) {

        String storeId = contentMessage.getStoreId();
        String spaceId = contentMessage.getSpaceId();
        String contentId = contentMessage.getContentId();

        Set<DuplicationStorePolicy> dupStorePolicies = 
                policy.getDuplicationStorePolicies(spaceId);
        
        if(dupStorePolicies != null) {
            
            Set<Task> tasks = null;
            
            for (DuplicationStorePolicy dupStorePolicy : dupStorePolicies) {
                if (dupStorePolicy.getSrcStoreId().equals(storeId)) {
                    DuplicationTask dupTask = new DuplicationTask();
                    dupTask.setAccount(subdomain);
                    dupTask.setDestStoreId(dupStorePolicy.getDestStoreId());
                    dupTask.setSpaceId(spaceId);
                    dupTask.setContentId(contentId);
                    dupTask.setSourceStoreId(storeId);
                    
                    log.debug("adding duplication task to the task queue: {}", dupTask);
                    
                    if(tasks == null){
                        tasks = new HashSet<>();
                    }
                    
                    tasks.add(dupTask.writeTask());
                }
            }
            
            if(tasks != null){
                duplicationTaskQueue.put(tasks);
            }

        }else{
            log.debug("no duplication policies for {} on subdomain {}", spaceId, subdomain);
        }
    }

    /**
     * @param contentMessage
     * @return
     */
    private boolean isDuplicatable(ContentMessage contentMessage) {
        try {
            ACTION action = ACTION.valueOf(contentMessage.getAction());
            return (
                action != null && 
                ( 
                    action.equals(ACTION.INGEST) || 
                    action.equals(ACTION.UPDATE) ||
                    action.equals(ACTION.COPY) ||
                    action.equals(ACTION.DELETE) 
                )
            );
        } catch (IllegalArgumentException e) {
            log.warn(
                    "failed to get enum value of " + contentMessage.getAction()
                            + ": " + e.getMessage(), e);
            return false;
        }
    }
}
