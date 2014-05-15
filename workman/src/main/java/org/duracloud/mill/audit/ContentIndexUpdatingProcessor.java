/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.audit.task.AuditTask.ActionType;
import org.duracloud.common.util.TagUtil;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.contentindex.client.ContentIndexItem;
import org.duracloud.mill.contentindex.ContentIndexItemUtil;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * As a processor of audit tasks as the name suggests, this class updates the
 * content index.
 * 
 * @author Daniel Bernstein Date: Mar 20, 2014
 */
public class ContentIndexUpdatingProcessor implements TaskProcessor {
    private final Logger       log = LoggerFactory
                                           .getLogger(ContentIndexUpdatingProcessor.class);

    private AuditLogStore      auditLogStore;
    private ContentIndexClient contentIndexClient;
    private AuditTask          task;

    public ContentIndexUpdatingProcessor(AuditTask task,
            ContentIndexClient contentIndexClient){
        this.contentIndexClient = contentIndexClient;
        this.task = task;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.workman.TaskProcessor#execute()
     */
    @Override
    public void execute() throws TaskExecutionFailedException {
        try {
            String account = task.getAccount();
            String storeId = task.getStoreId();
            String spaceId = task.getSpaceId();
            String contentId = task.getContentId();
            String storeType = task.getStoreType();
            Map<String, String> props = task.getContentProperties();
            Date timestamp = new Date(Long.valueOf(task.getDateTime()));
            ActionType action  = ActionType.valueOf(task.getAction());

            ContentIndexItem indexItem = new ContentIndexItem(account, 
                                                              storeId,
                                                              spaceId, 
                                                              contentId);

            ContentIndexItemUtil.setProps(props, indexItem);

            
            indexItem.setVersion(timestamp.getTime());
            indexItem.setStoreType(storeType);

            if (action.equals(ActionType.DELETE_CONTENT)) {
                contentIndexClient.delete(indexItem);
            } else if (action.equals(ActionType.ADD_CONTENT)
                    || action.equals(ActionType.SET_CONTENT_PROPERTIES)) {
                contentIndexClient.save(indexItem);
            } else {
                log.debug("action type {} not being handled by this processor.", action);
            }

            log.debug("content index item saved: {}", indexItem);
            log.debug("task successfully processed: {}", task);
        } catch (Exception e) {
            String message = "Failed to execute " + task + ": "
                    + e.getMessage();
            log.debug(message, e);
            throw new TaskExecutionFailedException(message, e);
        }
    }



}
