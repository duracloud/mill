/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.util.List;

import org.duracloud.mill.db.model.JpaAuditLogItem;
import org.duracloud.mill.db.repo.JpaAuditLogItemRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    
    @Autowired
    public AuditLogGenerator(JpaAuditLogItemRepo auditLogItemRepo, LogManager logManager) {
        this.auditLogItemRepo = auditLogItemRepo;
        this.logManager = logManager;
    }

    public void execute() {
        log.info("executing generator...");
        long itemsWritten = 0;
        long totalItemsWritten = 0;
        try {
            // Reads the Audit table, writes each item it finds to a
            // space-specific file
            int maxItemsPerRequest = Integer.parseInt(System.getProperty("max-audit-items-per-request", "1000"));
            Pageable pageRequest = new PageRequest(0, maxItemsPerRequest);

            while (true) {
                List<JpaAuditLogItem> items = auditLogItemRepo.findByWrittenFalseOrderByTimestampAsc(pageRequest);
                if (CollectionUtils.isEmpty(items)) {
                    log.info("No audit items found for processing: nowhere to go, nothing to do.", items.size());
                    break;
                }

                log.info("{} audit items read from the the jpa repo.", items.size());

                for (JpaAuditLogItem item : items) {
                    write(item);
                }
                
                itemsWritten += items.size();
                totalItemsWritten +=itemsWritten;
                log.info("{} audit items written.", items.size());
                
                if(itemsWritten >= 100000){
                    log.info("{} items written since last flush.  Flushing logs...", itemsWritten);
                    logManager.flushLogs();
                    itemsWritten = 0;
                    log.info("Purge expired...");
                    logManager.purgeExpired();
                }
            }
            
            log.info("{} total audit items written in this run.", totalItemsWritten);
            log.info("perform final purge of expired items.");
            this.logManager.purgeExpired();
            log.info("purge complete.");
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        } finally {
            // close all logs
            try {
                logManager.flushLogs();
                log.info("audit log run completed.");
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
        logManager.write(item);
    }




}
