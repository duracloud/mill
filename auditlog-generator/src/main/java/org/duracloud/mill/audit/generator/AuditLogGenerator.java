/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.mill.db.model.JpaAuditLogItem;
import org.duracloud.mill.db.repo.JpaAuditLogItemRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author Daniel Bernstein Date: Sep 5, 2014
 */
@Component
public class AuditLogGenerator {

    private static Logger log = LoggerFactory
            .getLogger(AuditLogGenerator.class);
    private JpaAuditLogItemRepo auditLogItemRepo;
    private LogManager logManager;
    
    private List<JpaAuditLogItem> recentWrites = new LinkedList<>();
    
    @Autowired
    public AuditLogGenerator(JpaAuditLogItemRepo auditLogItemRepo, LogManager logManager) {
        this.auditLogItemRepo = auditLogItemRepo;
        this.logManager = logManager;
    }

    public void execute() {
        log.info("executing generator...");
        try {
            // Reads the Audit table, writes each item it finds to a
            // space-specific file
            while (true) {

                List<JpaAuditLogItem> items = auditLogItemRepo.findByWrittenFalseOrderByTimestampAsc();
                if (CollectionUtils.isEmpty(items)) {
                    log.info("No audit items found for processing: nowhere to go, nothing to do.", items.size());
                    break;
                }

                log.info("{} audit items read from the the jpa repo.", items.size());

                for (JpaAuditLogItem item : items) {
                    write(item);
                }
                
                log.info("{} audit items written.", items.size());
                
            }
            
            this.logManager.purgeExpired();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        } finally {
            // close all logs
            try {
                logManager.flushLogs();
                log.info("audit log run completed successfully.");
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * @param item
     */
    private void write(JpaAuditLogItem item) {
        if(!isEqualToRecentlyWritten(item)){
            this.logManager.write(item);
            this.recentWrites.add(0, item);
            if(this.recentWrites.size() > 10){
                this.recentWrites.remove(this.recentWrites.size()-1);
            }
        }else{
            log.info("We detected log item that matches another item that just written, " +
            		"differing only in timestamp: {}. This item will not be written....", item);
        }
        
    }

    /**
     * @param item
     * @return
     */
    private boolean isEqualToRecentlyWritten(JpaAuditLogItem current) {
        for(JpaAuditLogItem old : this.recentWrites){
            if(equals(old.getAccount(),current.getAccount()) &&
               equals(old.getAction(),current.getAction()) &&
               equals(old.getContentId(),current.getContentId()) &&
               equals(old.getContentMd5(),current.getContentMd5()) && 
               equals(old.getContentProperties(), current.getContentProperties()) && 
               equals(old.getContentSize(),current.getContentSize()) &&
               equals(old.getStoreId(),current.getStoreId()) &&
               equals(old.getSpaceId(),current.getSpaceId()) &&
               equals(old.getSpaceAcls(),current.getSpaceAcls()) &&
               equals(old.getMimetype(),current.getMimetype()) &&
               equals(old.getSourceContentId(),current.getSourceContentId()) &&
               equals(old.getSourceSpaceId(),current.getSourceSpaceId()) &&
               equals(old.getUsername(),current.getUsername())) {
                return true;
            }
               
        }
        
        return false;

    }

    /**
     * @param string1
     * @param string2
     * @return
     */
    private boolean equals(String string1, String string2) {
       return StringUtils.equals(string1, string2);
    }

}
