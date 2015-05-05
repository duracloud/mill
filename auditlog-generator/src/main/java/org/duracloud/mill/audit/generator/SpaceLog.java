/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.audit.AuditLogItem;
import org.duracloud.audit.AuditLogUtil;
import org.duracloud.common.util.DateUtil.DateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein Date: Sep 5, 2014
 */
public class SpaceLog {
    private static final Comparator<File> LAST_MODIFIED_DATE_COMPARATOR = new LastModifiedDateFileComparator();
    private static Logger log = LoggerFactory.getLogger(SpaceLog.class);
    private static SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat(DateFormat.LONG_FORMAT
            .getPattern());

    public final static long MAX_FILE_SIZE = 10*1024*1024; 
    private LogKey key;
    private File logDir;
    private Writer writer;
    private File currentLogFile;

    /**
     * @param key
     * @param logsRootDir
     * @param contentStore
     */
    public SpaceLog(LogKey key,
                    File logsRootDir) {
        this.key = key;
        this.logDir = createSpaceLogsDirectory(logsRootDir);
    }

    private File createSpaceLogsDirectory(File rootDir){
        File directory = new File(rootDir.getAbsolutePath()
                                  + File.separator + key.getAccountId() + File.separator
                                  + key.getStoreId() + File.separator + key.getSpaceId());
        return directory;
    }
    /**
     * 
     */
    public void close() {
        this.currentLogFile = null;

        if(this.writer != null){

            try {
                this.writer.close();
                this.writer = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
            
    }

    /**
     * @param item
     */
    public void write(AuditLogItem item) throws IOException {
        makeDirIfNotExists();
        
        //open writer if not open
        if(writer == null){
            currentLogFile = getAvailableLogFile();
            writer = createWriter(currentLogFile);
            if(!currentLogFile.exists() || currentLogFile.length() == 0){
                writer.write(getHeader()+'\n');
            }
        }
        
        //write to log
        writer.write(formatRecord(item));
        writer.flush();
        //roll log if size exceeded.
        if(this.currentLogFile.length() > MAX_FILE_SIZE){
            close();
        }
    }

    /**
     * @return
     */
    private String getHeader() {
        return AuditLogUtil.getHeader();
    }

    /**
     * @return
     */
    private File getAvailableLogFile() {
        
        //get the most recently updated log file.
        File[] files = this.logDir.listFiles();
        //if null or full create new 
        if(files != null && files.length > 0) {
            List<File> fileList = Arrays.asList(files);
            Collections.sort(fileList, LAST_MODIFIED_DATE_COMPARATOR);
            
            File lastModified = fileList.get(0);
            
            if(lastModified.length() < MAX_FILE_SIZE){
                return lastModified;
            }
        }

        return  createNewLogFile();
    }

    /**
     * @param item
     * @return
     */
    private String formatRecord(AuditLogItem item) {
        return StringUtils.join(new String[]{
                item.getAccount(),
                item.getStoreId(),
                item.getSpaceId(),
                item.getContentId(),
                item.getContentMd5(),
                item.getContentSize(),
                item.getMimetype(),
                removeLineBreaksAndTabs(emptyStringIfNull(item.getContentProperties())),
                removeLineBreaksAndTabs(emptyStringIfNull(item.getSpaceAcls())),
                emptyStringIfNull(item.getSourceSpaceId()),
                emptyStringIfNull(item.getSourceContentId()),
                formatDate(new Date(item.getTimestamp())),
                item.getAction(),
                item.getUsername()
        }, "\t") + "\n";
    }

    /**
     * @param emptyStringIfNull
     * @return
     */
    private String removeLineBreaksAndTabs(String str) {
        return str.replaceAll("[\n\r\t]", "");
    }

    /**
     * @param string
     * @return
     */
    private String emptyStringIfNull(String string) {
        if(string == null){
            return "";
        }else{
            return string;
        }
    }

    /**
     * @return
     */
    protected File createNewLogFile() {
        SimpleDateFormat format = new SimpleDateFormat(DateFormat.PLAIN_FORMAT.getPattern());
        String date = format.format(new Date());
        File file = new File(this.logDir, key.getAccountId()
                + "_" + key.getStoreId() + "_" + key.getSpaceId() + "-" + date+ ".tsv");
        return file;
    }

    /**
     * @return
     */
    private Writer createWriter(File file) throws IOException{
        return new FileWriter(file, true);
    }

    private void makeDirIfNotExists() {
        if(!this.logDir.exists()){
            this.logDir.mkdirs();
        }
    }

    /**
     * @param timestamp
     * @return
     */
    protected String formatDate(Date timestamp) {
        return LOG_DATE_FORMAT.format(timestamp);
    }
}
