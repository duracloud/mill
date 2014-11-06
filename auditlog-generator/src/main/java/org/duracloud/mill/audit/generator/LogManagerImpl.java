/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
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

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Daniel Bernstein 
 *         Date: Sep 5, 2014
 */
@Component
public class LogManagerImpl implements LogManager {
    private static Logger log = LoggerFactory.getLogger(LogManagerImpl.class);
    private JpaAuditLogItemRepo repo;
    private Map<LogKey, SpaceLog> logMap = new HashMap<>();
    private File logsDirectory;
    private StorageProvider storageProvider;
    private String auditLogSpaceId;
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
            throw new RuntimeException("logsDirectory (" + logsDirectory
                    + ") does not exist.");
        }

    }

    @PostConstruct
    public void init() {
        log.info("initializing...");
        uploadLogs();
        log.info("initialization complete");
    }

    /**
     * @param item
     */
    @Transactional(MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
    public void write(JpaAuditLogItem item) {
        SpaceLog auditLog = getLog(item);

        try {
            auditLog.write(item);
            JpaAuditLogItem fresh = repo.getOne(item.getId());
            fresh.setWritten(true);
            repo.saveAndFlush(fresh);
        } catch (IOException ex) {
            log.error("failed to write to file. Database was not updated: "
                    + ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param item
     * @return
     */
    private SpaceLog getLog(JpaAuditLogItem item) {
        LogKey key = new LogKey(item.getAccount(),
                                item.getStoreId(),
                                item.getSpaceId());
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
                        try ( FileInputStream fis = new FileInputStream(file)){
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
                            
                        }catch(Exception ex){
                            throw new SpaceLogUploadException("failed to upload "
                                    + file.getAbsolutePath()
                                    + " to "
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

}
