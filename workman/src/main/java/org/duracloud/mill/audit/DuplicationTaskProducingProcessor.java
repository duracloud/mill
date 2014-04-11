/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import static org.duracloud.audit.task.AuditTask.ActionType.ADD_CONTENT;
import static org.duracloud.audit.task.AuditTask.ActionType.COPY_CONTENT;
import static org.duracloud.audit.task.AuditTask.ActionType.DELETE_CONTENT;
import static org.duracloud.audit.task.AuditTask.ActionType.DELETE_SPACE;
import static org.duracloud.audit.task.AuditTask.ActionType.SET_CONTENT_PROPERTIES;

import java.util.HashSet;
import java.util.Set;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.task.DuplicationTask;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 11, 2014
 */
public class DuplicationTaskProducingProcessor implements TaskProcessor {
    private static Logger log = LoggerFactory
            .getLogger(DuplicationTaskProducingProcessor.class);

    private AuditTask task;
    private TaskQueue duplicationTaskQueue;
    private DuplicationPolicyManager duplicationPolicyManager;

    /**
     * @param at
     * @param duplicationTaskQueue
     * @param duplicationPolicyManager
     * @param notificationManager
     */
    public DuplicationTaskProducingProcessor(AuditTask task,
            TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager) {
        this.task = task;
        this.duplicationTaskQueue = duplicationTaskQueue;
        this.duplicationPolicyManager = duplicationPolicyManager;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessor#execute()
     */
    @Override
    public void execute() throws TaskExecutionFailedException {
        if (isDuplicatable(task)) {
            applyDuplicationPolicy(task);
        }else{
            log.warn("This message {} is not duplicable: it will be ignored." );
        }
    }
    
   
    /**
     * @param contentMessage
     * @return
     */
    private boolean isDuplicatable(AuditTask task) {
        if(task.getAction() == null){
            return false;
        }
        
        try {
            AuditTask.ActionType action = AuditTask.ActionType.valueOf(task.getAction());
            return (action.equals(ADD_CONTENT) || 
                    action.equals(SET_CONTENT_PROPERTIES) ||
                    action.equals(COPY_CONTENT) ||
                    action.equals(DELETE_CONTENT) ||
                    action.equals(DELETE_SPACE));
        } catch (IllegalArgumentException e) {
            log.warn(
                    "failed to get enum value of " + task.getAction()
                            + ": " + e.getMessage());
            return false;
        }
    }

    protected void applyDuplicationPolicy(AuditTask task) {

        String account = task.getAccount();
        DuplicationPolicy policy = duplicationPolicyManager
                .getDuplicationPolicy(account);
        if (policy == null) {
            log.warn(
                    "no policy found for account \"{}\": {} will be ignored. ",
                    account, task);
            return;
        } else{
            log.debug(
                    "applying duplication policies to {} for {}", task, account);
        }
        
        String storeId = task.getStoreId();
        String spaceId = task.getSpaceId();
        String contentId = task.getContentId();
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
                    dupTask.setAccount(account);
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
            log.info("no duplication policies for {} on account {}", spaceId, account);
        }
    }

}
