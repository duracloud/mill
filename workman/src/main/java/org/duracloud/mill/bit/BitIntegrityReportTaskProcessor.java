/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.common.util.ChecksumUtil.Algorithm;
import org.duracloud.common.util.DateUtil;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.db.model.BitIntegrityReportResult;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.spring.WorkmanConfigurationManager;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein Date: May 7, 2014
 */
public class BitIntegrityReportTaskProcessor implements
                                            TaskProcessor {
    private static Logger log = LoggerFactory
            .getLogger(BitIntegrityReportTaskProcessor.class);
    private BitIntegrityCheckReportTask task;
    private BitLogStore bitLogStore;
    private StorageProvider store;
    private WorkmanConfigurationManager config;
    private DateFormat dateFormat = new SimpleDateFormat(DateUtil.DateFormat.DEFAULT_FORMAT
            .getPattern());

    /**
     * @param bitTask
     * @param bitLogStore
     */
    public BitIntegrityReportTaskProcessor(BitIntegrityCheckReportTask task,
                                           BitLogStore bitLogStore,
                                          
                                           StorageProvider store,
                                           WorkmanConfigurationManager config) {
        this.task = task;
        this.bitLogStore = bitLogStore;
        this.store = store;
        this.config = config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.workman.TaskProcessor#execute()
     */
    @Override
    public void execute() throws TaskExecutionFailedException {
        final String account = task.getAccount();
        final String storeId = task.getStoreId();
        final String spaceId = task.getSpaceId();

        // open text file
        final File bitLogDir = new File(config.getWorkDirectoryPath()
                + File.separator + "bit-integrity-reports");
        if (!bitLogDir.exists()) {
            bitLogDir.mkdirs();
        }

        final File bitLog = createNewLogFile(account,
                                             storeId,
                                             spaceId,
                                             bitLogDir);

        try {
            writeLog(bitLog, account, storeId, spaceId);

            ChecksumUtil util = new ChecksumUtil(Algorithm.MD5);
            final String checksum = util.generateChecksum(bitLog);
            // upload to duracloud
            final String bitlogSpaceId = "x-duracloud-admin";
            final String contentId = "bit-integrity/" + spaceId + "/" + storeId + "/"
                    + bitLog.getName();

            new Retrier().execute(new Retriable() {

                @Override
                public Object retry() throws Exception {
                    return store.addContent(bitlogSpaceId,
                                            contentId,
                                            "text/tsv",
                                            null,
                                            bitLog.length(),
                                            checksum,
                                            new FileInputStream(bitLog));
                }
            });

            BitIntegrityReportResult result = BitIntegrityReportResult.FAILURE;
            
            if(bitLogStore.isCompletelySuccessful(account, storeId, spaceId)){
                result = BitIntegrityReportResult.SUCCESS;
            }
            
            String reportContentId = bitlogSpaceId + "/"+contentId;
            bitLogStore.addReport(account, storeId, spaceId, reportContentId, result , new Date());
            
            // delete all bit integrity log items for space.
            bitLogStore.delete(account, storeId, spaceId);
            bitLog.delete();
        } catch (Exception ex) {
            throw new TaskExecutionFailedException("task processing failed: "
                    + ex.getMessage(), ex);
        }

    }

    private void writeLog(File bitLog,
                          String account,
                          String storeId,
                          String spaceId) throws Exception {
        try (FileWriter writer = new FileWriter(bitLog)) {
            // for each bit integrity item for space write to file
            writer.write(getHeader());
            Iterator<BitLogItem> it = this.bitLogStore.getBitLogItems(account,
                                                                      storeId,
                                                                      spaceId);
            while (it.hasNext()) {
                writer.write(formatLogLine(it.next()));
                if (it.hasNext()) {
                    writer.write("\n");
                }
            }
        }
    }

    private File createNewLogFile(String account,
                                  String storeId,
                                  String spaceId,
                                  File bitLogDir) {
        DateFormat fileDateFormat = new SimpleDateFormat(DateUtil.DateFormat.PLAIN_FORMAT
                .getPattern());

        File bitLog = new File(bitLogDir, "bit-integrity_" + account
                + "_" + storeId + "_" + spaceId + "_"
                + fileDateFormat.format(new Date()) + ".txt");
        return bitLog;
    }

    /**
     * @param next
     * @return
     */
    private String formatLogLine(BitLogItem item) {
        String[] values = {
                dateFormat.format(item.getModified()),
                item.getAccount(), 
                item.getStoreId(),
                item.getStoreType().name(), 
                item.getSpaceId(),
                item.getContentId(), 
                item.getResult().name(),
                item.getContentChecksum(), 
                item.getStorageProviderChecksum(),
                item.getManifestChecksum(),
                item.getDetails()};

        return StringUtils.join(values, "\t");

    }

    /**
     * @return
     */
    private String getHeader() {
        String[] values = {
                "DATE_CHECKED",
                "ACCOUNT", 
                "STORE_ID",
                "STORE_TYPE", 
                "SPACE_ID",
                "CONTENT_ID", 
                "RESULT",
                "CONTENT_CHECKSUM", 
                "PROVIDER_CHECKSUM",
                "MANIFEST_CHECKSUM",
                "DETAILS"};

        return StringUtils.join(values, "\t")+"\n";

    }

}
