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
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.common.util.ChecksumUtil.Algorithm;
import org.duracloud.common.util.DateUtil;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.db.model.BitIntegrityReport;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessorBase;
import org.duracloud.reportdata.bitintegrity.BitIntegrityReportResult;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: May 7, 2014
 */
public class BitIntegrityReportTaskProcessor extends TaskProcessorBase {
    private static Logger log = LoggerFactory.getLogger(BitIntegrityReportTaskProcessor.class);
    private BitIntegrityCheckReportTask task;
    private BitLogStore bitLogStore;
    private StorageProvider store;
    private TaskProducerConfigurationManager config;

    private NotificationManager notificationManager;

    /**
     *
     */
    public BitIntegrityReportTaskProcessor(BitIntegrityCheckReportTask task,
                                           BitLogStore bitLogStore,
                                           StorageProvider store,
                                           TaskProducerConfigurationManager config,
                                           NotificationManager notificationManager) {
        super(task);
        this.task = task;
        this.bitLogStore = bitLogStore;
        this.store = store;
        this.config = config;
        this.notificationManager = notificationManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.duracloud.mill.workman.TaskProcessorBase#executeImpl()
     */
    @Override
    protected void executeImpl() throws TaskExecutionFailedException {
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
            List<BitLogItem> errors = writeLog(bitLog, account, storeId, spaceId);

            ChecksumUtil util = new ChecksumUtil(Algorithm.MD5);
            final String checksum = util.generateChecksum(bitLog);
            // upload to duracloud
            final String reportSpaceId = "x-duracloud-admin";
            final String reportContentId = "bit-integrity/" + spaceId + "/" + storeId + "/"
                                           + bitLog.getName();

            new Retrier().execute(new Retriable() {
                @Override
                public Object retry() throws Exception {
                    Iterator<String> spaces = store.getSpaces();
                    while (spaces.hasNext()) {
                        if (spaces.next().equals(reportSpaceId)) {
                            return null;
                        }
                    }

                    store.createSpace(reportSpaceId);
                    return null;
                }
            });

            new Retrier().execute(new Retriable() {

                @Override
                public Object retry() throws Exception {
                    return store.addContent(reportSpaceId,
                                            reportContentId,
                                            "text/tsv",
                                            null,
                                            bitLog.length(),
                                            checksum,
                                            new FileInputStream(bitLog));
                }
            });

            BitIntegrityReportResult result = BitIntegrityReportResult.SUCCESS;

            if (errors.size() > 0) {
                result = BitIntegrityReportResult.FAILURE;
            }

            BitIntegrityReport report = bitLogStore.addReport(account,
                                                              storeId,
                                                              spaceId,
                                                              reportSpaceId,
                                                              reportContentId,
                                                              result,
                                                              new Date());

            if (errors.size() > 0) {
                log.warn(
                    "Bit integirty errors: subdomain: {}, storeId: {}, spaceId: {}: ",
                    account, storeId, spaceId);

                notifyManagerOfBitIntegrityErrors(report, errors);
            }

            try {
                // delete all bit integrity log items for space.
                bitLogStore.delete(account, storeId, spaceId);
                log.info("Deleted Bit Log Items for account: {}, storeId: {}, spaceId: {}: ",
                         account, storeId, spaceId);

            } catch (Exception ex) {
                log.warn(MessageFormat.format(
                    "failed to delete bit log items where account={0}, store_id = {1}, space_id = {2} due to: {3}",
                    account, storeId, spaceId, ex.getMessage()), ex);
            }

        } catch (Exception ex) {
            throw new TaskExecutionFailedException("task processing failed: " + ex.getMessage(), ex);
        } finally {
            try {
                bitLog.delete();
            } catch (Exception ex) {
                log.warn("failed to delete bit log: " + bitLog.getAbsolutePath(), ex);
            }
        }

    }

    /**
     * @param report
     * @param errors
     */
    private void notifyManagerOfBitIntegrityErrors(BitIntegrityReport report,
                                                   List<BitLogItem> errors) {

        String account = report.getAccount();
        String storeId = report.getStoreId();
        String spaceId = report.getSpaceId();

        String host = account + ".duracloud.org";

        String subject = "Bit Integrity Report #" + report.getId() + ": errors (count = " +
                         errors.size() + ")  detected on " + host + ", providerId=" + storeId +
                         ", spaceId=" + spaceId;

        StringBuilder body = new StringBuilder();

        body.append(BitIntegrityHelper.getHeader());
        for (BitLogItem error : errors) {
            body.append(BitIntegrityHelper.formatLogLine(error) + "\n");
        }

        this.notificationManager.sendEmail(subject, body.toString());
    }

    private List<BitLogItem> writeLog(File bitLog,
                                      String account,
                                      String storeId,
                                      String spaceId) throws Exception {
        List<BitLogItem> errors = new LinkedList<>();
        try (FileWriter writer = new FileWriter(bitLog)) {
            // for each bit integrity item for space write to file
            writer.write(BitIntegrityHelper.getHeader());
            Iterator<BitLogItem> it = this.bitLogStore.getBitLogItems(account,
                                                                      storeId,
                                                                      spaceId);
            while (it.hasNext()) {
                BitLogItem item = it.next();
                if (isError(item)) {
                    errors.add(item);
                }
                writer.write(BitIntegrityHelper.formatLogLine(item));
                if (it.hasNext()) {
                    writer.write("\n");
                }
            }
        }

        return errors;
    }

    /**
     * @param item
     * @return
     */
    private boolean isError(BitLogItem item) {
        BitIntegrityResult result = item.getResult();
        return (result.equals(BitIntegrityResult.ERROR) ||
                result.equals(BitIntegrityResult.FAILURE));
    }

    private File createNewLogFile(String account,
                                  String storeId,
                                  String spaceId,
                                  File bitLogDir) {
        DateFormat fileDateFormat = new SimpleDateFormat(DateUtil.DateFormat.PLAIN_FORMAT.getPattern());

        File bitLog = new File(bitLogDir, "bit-integrity_" + account
                                          + "_" + storeId + "_" + spaceId + "_"
                                          + fileDateFormat.format(new Date()) + ".tsv");
        return bitLog;
    }

}
