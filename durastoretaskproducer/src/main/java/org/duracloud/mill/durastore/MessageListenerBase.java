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

import org.duracloud.mill.task.DuplicationTask;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.storage.aop.ContentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A base class for delegating <code>ContentMessage</code> objects
 * received from DuraStore to the appropriate <code>TaskQueue</code>s.
 * 
 * @author Daniel Bernstein 
 *         Date: Jan 7, 2014
 */
public abstract class MessageListenerBase implements MessageListener {
    private static Logger log = LoggerFactory
            .getLogger(MessageListenerBase.class);
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
    public MessageListenerBase(TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager, String subdomain) {
        this.duplicationTaskQueue = duplicationTaskQueue;
        this.duplicationPolicyManager = duplicationPolicyManager;
        this.subdomain = subdomain;
    }
    
    /**
     * @return the duplicationPolicyManager
     */
    protected DuplicationPolicyManager getDuplicationPolicyManager() {
        return duplicationPolicyManager;
    }
    
    /**
     * @return the subdomain
     */
    protected String getSubdomain() {
        return subdomain;
    }
    
    /**
     * @return the duplicationTaskQueue
     */
    protected TaskQueue getDuplicationTaskQueue() {
        return duplicationTaskQueue;
    }
    
    /**
     * @return the policy
     */
    public DuplicationPolicy getPolicy() {
        return policy;
    }

    /**
     * Receives message from jms listener.
     * @param message
     */
    
    public final void onMessage(ContentMessage message) {
        log.debug("listener for {} received {}", subdomain, message);
        onMessageImpl(message);
    }

    /**
     * @param message
     */
    protected abstract void onMessageImpl(ContentMessage message);

    /**
     * @param contentMessage
     */
    protected void applyDuplicationPolicy(ContentMessage contentMessage) {

        policy = duplicationPolicyManager
                .getDuplicationPolicy(subdomain);
        if (policy == null) {
            log.warn(
                    "no policy found for subdomain \"{}\": {} will be ignored. ",
                    subdomain, contentMessage);
            return;
        } else{
            log.debug(
                    "applying duplication policies to {} for {}", contentMessage, subdomain);
        }
        
        String storeId = contentMessage.getStoreId();
        String spaceId = contentMessage.getSpaceId();
        String contentId = contentMessage.getContentId();
        if(contentId == null){
            contentId = "";
        }
        Set<DuplicationStorePolicy> dupStorePolicies = 
                policy.getDuplicationStorePolicies(spaceId);
        
        if(dupStorePolicies != null && !dupStorePolicies.isEmpty()) {
            
            Set<Task> tasks = null;
            
            for (DuplicationStorePolicy dupStorePolicy : dupStorePolicies) {
                if (dupStorePolicy.getSrcStoreId().equals(storeId)) {
                    log.debug(
                            "policy's sourceStoreId matches " +
                            "messageStoreId: policy={}; messageStoreId={}",
                            dupStorePolicy, storeId);
                    DuplicationTask dupTask = new DuplicationTask();
                    dupTask.setAccount(subdomain);
                    dupTask.setDestStoreId(dupStorePolicy.getDestStoreId());
                    dupTask.setSpaceId(spaceId);
                    dupTask.setContentId(contentId);
                    dupTask.setSourceStoreId(storeId);
                    
                    log.info("adding duplication task to the task queue: {}", dupTask);
                    
                    if(tasks == null){
                        tasks = new HashSet<>();
                    }
                    
                    tasks.add(dupTask.writeTask());
                }else{
                    log.debug(
                            "policy's sourceStoreId does not match " +
                            "messageStoreId: policy={}; messageStoreId={}",
                            dupStorePolicy, storeId);
                }
            }
            
            if(tasks != null){
                duplicationTaskQueue.put(tasks);
            }

        }else{
            log.info("no duplication policies for {} on subdomain {}", spaceId, subdomain);
        }
    }
}
