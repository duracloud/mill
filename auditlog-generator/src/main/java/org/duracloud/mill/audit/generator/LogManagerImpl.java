/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.common.util.ContentIdUtil;
import org.duracloud.mill.db.model.JpaAuditLogItem;
import org.duracloud.mill.db.repo.JpaAuditLogItemRepo;
import org.duracloud.mill.db.repo.MillJpaRepoConfig;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Daniel Bernstein
 * Date: Sep 5, 2014
 */
@Component
public class LogManagerImpl implements LogManager {
    private static Logger log = LoggerFactory.getLogger(LogManagerImpl.class);
    private JpaAuditLogItemRepo repo;
    private Map<LogKey, SpaceLog> logMap = new HashMap<>();
    private File logsDirectory;
    private StorageProvider storageProvider;
    private String auditLogSpaceId;
    private int ageInDaysOfPurgeableWrittenLogEntries = 30;
    private List<JpaAuditLogItem> recentWrites = new LinkedList<>();

    @Autowired
    public LogManagerImpl(StorageProvider storageProvider,
                          String logsDirectory,
                          JpaAuditLogItemRepo repo,
                          @Qualifier("auditLogSpaceId") String auditLogSpaceId) {
        this.storageProvider = storageProvider;
        this.logsDirectory = new File(logsDirectory);
        this.repo = repo;
        this.auditLogSpaceId = auditLogSpaceId;
        if (!this.logsDirectory.exists()) {
            throw new RuntimeException("logsDirectory (" + logsDirectory + ") does not exist.");
        }

    }

    /**
     * @param item
     */
    @Transactional(MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
    public void write(JpaAuditLogItem item) {
        SpaceLog auditLog = getLog(item);

        try {
            if (!isEqualToRecentlyWritten(item)) {
                auditLog.write(item);
                this.recentWrites.add(0, item);
                if (this.recentWrites.size() > 10) {
                    this.recentWrites.remove(this.recentWrites.size() - 1);
                }
            } else {
                log.info("We detected log item that matches another item that just written, " +
                         "differing only in timestamp: {}. This item will not be written....", item);
            }
            JpaAuditLogItem fresh = repo.getOne(item.getId());
            fresh.setWritten(true);
            repo.saveAndFlush(fresh);
        } catch (IOException ex) {
            log.error("failed to write to file. Database was not updated: " + ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param item
     * @return
     */
    private SpaceLog getLog(JpaAuditLogItem item) {
        LogKey key = new LogKey(item.getAccount(), item.getStoreId(), item.getSpaceId());
        SpaceLog auditLog = this.logMap.get(key);
        if (auditLog == null) {
            auditLog = createSpaceLog(key);
            logMap.put(key, auditLog);
        }

        return auditLog;
    }

    protected SpaceLog createSpaceLog(LogKey key) {
        return new SpaceLog(key, logsDirectory);
    }

    /**
     *
     */
    private void uploadLogs() {
        log.info("closing audit logs...");
        for (Map.Entry<LogKey, SpaceLog> entry : this.logMap.entrySet()) {
            entry.getValue().close();
            log.debug("closed audit log {}", entry.getKey());
        }
        this.logMap.clear();
        log.info("audit logs closed.");

        try {
            for (final File file : getLogFiles(this.logsDirectory)) {
                new Retrier(3).execute(new Retriable() {
                    /*
                     * (non-Javadoc)
                     *
                     * @see org.duracloud.common.retry.Retriable#retry()
                     */
                    @Override
                    public Object retry() throws SpaceLogUploadException {
                        log.info("Uploading log file {}", file.getAbsolutePath());
                        try (FileInputStream fis = new FileInputStream(file)) {
                            ChecksumUtil checksumUtil =
                                new ChecksumUtil(ChecksumUtil.Algorithm.MD5);
                            String md5 = checksumUtil.generateChecksum(file);
                            String contentId =
                                ContentIdUtil.getContentId(file, logsDirectory, null);
                            storageProvider.addContent(auditLogSpaceId,
                                                       contentId,
                                                       "text/tsv",
                                                       null,
                                                       file.length(),
                                                       md5,
                                                       fis);
                            fis.close();
                            log.info("successfully uploaded log {}  to durastore.",
                                     file.getAbsoluteFile());

                            if (fileIsFull(file)) {
                                file.delete();
                                log.info("log file {} deleted from local storage.",
                                         file.getAbsolutePath());
                            }
                            return "success";

                        } catch (Exception ex) {
                            throw new SpaceLogUploadException("failed to upload " + file.getAbsolutePath() + " to "
                                                              + storageProvider + ":" + ex.getMessage(), ex);

                        }
                    }
                });
            }

            log.info("All logs successfully uploaded.");

        } catch (Exception ex) {
            log.error("Upload failed: not all files uploaded successfully:  " +
                      ex.getMessage(), ex);
        }
    }

    private boolean fileIsFull(File file) {
        return file.length() >= SpaceLog.MAX_FILE_SIZE;
    }

    /**
     * @return
     */
    protected Collection<File> getLogFiles(File directory) {
        return FileUtils.listFiles(directory,
                                   FileFilterUtils.trueFileFilter(),
                                   FileFilterUtils.trueFileFilter());
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.audit.generator.LogManager#flushLogs()
     */
    @Override
    public void flushLogs() {
        uploadLogs();
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.audit.generator.LogManager#purgeExpired()
     */
    @Override
    @Transactional(MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
    public void purgeExpired() {
        log.info("flushing all written log entries over {} days old.", ageInDaysOfPurgeableWrittenLogEntries);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, ageInDaysOfPurgeableWrittenLogEntries * -1);
        Date date = c.getTime();
        long deleted = this.repo.deleteByWrittenTrueAndTimestampLessThan(date.getTime());
        log.info("successfully deleted {} audit log entries that had been written and were timestamped before {}",
                 deleted, date);

    }

    /**
     * @param item
     * @return
     */
    private boolean isEqualToRecentlyWritten(JpaAuditLogItem current) {
        for (JpaAuditLogItem old : this.recentWrites) {
            if (equals(old.getAccount(), current.getAccount()) &&
                equals(old.getAction(), current.getAction()) &&
                equals(old.getContentId(), current.getContentId()) &&
                equals(old.getContentMd5(), current.getContentMd5()) &&
                equals(old.getContentProperties(), current.getContentProperties()) &&
                equals(old.getContentSize(), current.getContentSize()) &&
                equals(old.getStoreId(), current.getStoreId()) &&
                equals(old.getSpaceId(), current.getSpaceId()) &&
                equals(old.getSpaceAcls(), current.getSpaceAcls()) &&
                equals(old.getMimetype(), current.getMimetype()) &&
                equals(old.getSourceContentId(), current.getSourceContentId()) &&
                equals(old.getSourceSpaceId(), current.getSourceSpaceId()) &&
                equals(old.getUsername(), current.getUsername())) {
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
