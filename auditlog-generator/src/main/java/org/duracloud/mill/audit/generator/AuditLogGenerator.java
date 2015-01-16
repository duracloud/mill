/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private int ageInDaysOfDeletableWrittenLogEntries = 30;

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
            
            log.info("flushing all written log entries over {} days old.", ageInDaysOfDeletableWrittenLogEntries);
            
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, ageInDaysOfDeletableWrittenLogEntries*-1);
            Date date = c.getTime();
            long deleted = this.auditLogItemRepo.deleteByWrittenTrueAndTimestampLessThan(date.getTime());
            log.info("successfully deleted {} audit log entries that had been written and were timestamped before {}", deleted, date);
            
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
        this.logManager.write(item);
    }

}
