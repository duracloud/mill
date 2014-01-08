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

        if (policy == null) {
            policy = duplicationPolicyManager
                    .getDuplicationPolicy(subdomain);
            if (policy == null) {
                log.warn(
                        "no policy found for subdomain \"{}\": this message will be ignored. " +
                        "The most likely cause is that the subdomain " +
                        "was removed from the duplication policy account " +
                        "list after the durastoretaskproducer was started. " +
                        "To make this message go away, restart the " +
                        "durastore task producer.",
                        subdomain);
                return;
            }
        }
        
        String storeId = contentMessage.getStoreId();
        String spaceId = contentMessage.getSpaceId();
        String contentId = contentMessage.getContentId();
        if(contentId == null){
            contentId = "";
        }
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
}
