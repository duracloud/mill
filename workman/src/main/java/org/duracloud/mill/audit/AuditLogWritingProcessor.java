/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import java.util.Date;
import java.util.Map;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessorBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionSystemException;

/**
 * As a processor of audit tasks as the name suggests, this class creates an
 * audit log entry and subsequently updates the content index.
 * 
 * @author Daniel Bernstein 
 *         Date: Mar 20, 2014
 */
public class AuditLogWritingProcessor extends TaskProcessorBase {
    private final Logger       log = LoggerFactory
                                           .getLogger(AuditLogWritingProcessor.class);

    private AuditLogStore      auditLogStore;
    private AuditTask          task;

    public AuditLogWritingProcessor(AuditTask task,
            AuditLogStore auditLogStore) {
        super(task);
        this.auditLogStore = auditLogStore;
        this.task = task;
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
            Map<String, String> props = task.getContentProperties();
            String acls = task.getSpaceACLs();
            Date timestamp = new Date(Long.valueOf(task.getDateTime()));
            
            auditLogStore.write(account, 
                                storeId, 
                                spaceId, 
                                contentId,
                                task.getContentChecksum(), 
                                task.getContentMimetype(),
                                task.getContentSize(), 
                                task.getUserId(), 
                                action,
                                props != null ? AuditLogStoreUtil.serialize(props) : null, 
                                acls,
                                task.getSourceSpaceId(), 
                                task.getSourceContentId(),
                                timestamp);

            log.debug("audit task successfully processed: {}", task);
        } catch(TransactionSystemException e){
            log.error("failed to write item  ( account={} storeId={} spaceId={} contentId={} timestamp={} ) " +
            	     "to the database due to a transactional error. Likely cause: duplicate entry. Details: {}. Ignoring...",
                     task.getAccount(),
                     task.getStoreId(),
                     task.getSpaceId(),
                     task.getContentId(),
                     new Date(Long.valueOf(task.getDateTime())),
                     e.getMessage());
        } catch (Exception e) {
            String message = "Failed to execute " + task + ": "
                    + e.getMessage();
            log.debug(message, e);
            throw new TaskExecutionFailedException(message, e);
        }
    }

}
