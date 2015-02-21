/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest;

import java.util.Date;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.audit.task.AuditTask.ActionType;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessorBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *         Date: Sep 3, 2014
 */
public class ManifestWritingProcessor extends
                                     TaskProcessorBase {
    private static Logger log = LoggerFactory.getLogger(ManifestWritingProcessor.class);
    private AuditTask task;
    private ManifestStore manifestStore;
    /**
     * @param task
     * @param manifestStore 
     */
    public ManifestWritingProcessor(AuditTask task, ManifestStore manifestStore) {
        super(task);
        this.task = task;
        this.manifestStore = manifestStore;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessorBase#executeImpl()
     */
    @Override
    protected void executeImpl() throws TaskExecutionFailedException {
        try {
            String account = task.getAccount();
            String storeId = task.getStoreId();
            String spaceId = task.getSpaceId();
            String contentId = task.getContentId();
            String action = task.getAction();
            Date timeStamp = new Date(Long.parseLong(task.getDateTime()));
            
            if(ActionType.ADD_CONTENT.name().equals(action) || 
                    ActionType.COPY_CONTENT.name().equals(action) ){
                String mimetype = task.getContentMimetype();
                String size = task.getContentSize();

                this.manifestStore.addUpdate(account, 
                                    storeId, 
                                    spaceId, 
                                    contentId,
                                    task.getContentChecksum(),
                                    mimetype,
                                    size,
                                    timeStamp);
                
            }else if(ActionType.DELETE_CONTENT.name().equals(action)){
                this.manifestStore.flagAsDeleted(account,
                                                 storeId,
                                                 spaceId,
                                                 contentId,
                                                 timeStamp);
            }else{
                log.debug("action {} not handled by this processor: task={}", action,task);
            }
            log.info("audit task successfully processed: {}", task);
        } catch (Exception e) {
            String message = "Failed to execute " + task + ": "
                    + e.getMessage();
            log.debug(message, e);
            throw new TaskExecutionFailedException(message, e);
        }
    }        

}
