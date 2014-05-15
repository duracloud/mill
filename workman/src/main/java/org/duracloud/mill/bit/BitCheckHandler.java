/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.util.Map;

import org.duracloud.audit.AuditLogItem;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.audit.task.AuditTask.ActionType;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskWorker;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A base class for building specific bit check handlers used in the 
 * BitIntegrityCheckTaskProcessor.
 * @author Daniel Bernstein
 *	       Date: May 15, 2014
 */
abstract class BitCheckHandler {
    
    private static Logger log = LoggerFactory.getLogger(BitCheckHandler.class);
    
    public final boolean handle(BitCheckParameters parameters)
            throws TaskExecutionFailedException {
        HandlerResult result = handleImpl(parameters);
        if (result.isHandled()) {
            if (!result.getResult().equals(BitIntegrityResult.IGNORE)) {
                AuditLogItem auditLogItem = parameters.getAuditLogItem();
                writeResult(
                        result.getResult(),
                        auditLogItem == null ? null : auditLogItem
                                .getContentMd5(),
                        parameters.getContentIndexChecksum(),
                        parameters.getStoreChecksum(),
                        parameters.getContentChecksum(),
                        parameters.getBitLogStore(),
                        parameters.getStorageProviderType(),
                        parameters.getTask(), result.getMessage());
            }
            return true;
        }else{
            return false;
        }
    }

    protected String buildFailureMessage(String message,
            BitIntegrityCheckTask bitTask,
            StorageProviderType storageProviderType) {
        return BitIntegrityMessageUtil.buildFailureMessage(message, bitTask,
                storageProviderType);
    }

    /**
     * 
     * @param parameters
     * @return
     */
    abstract protected HandlerResult handleImpl(BitCheckParameters parameters);

    /**
     * @param bitTask
     * @param message
     */
    protected void addErrorTask(BitCheckParameters parameters, String message) {
        BitIntegrityCheckTask bitTask = parameters.getTask();
        TaskQueue queue = parameters.getBitErrorQueue();
        BitIntegrityErrorTask task = new BitIntegrityErrorTask();
        task.setAccount(bitTask.getAccount());
        task.setStoreId(bitTask.getStoreId());
        task.setSpaceId(bitTask.getSpaceId());
        task.setDescription(message);
        task.setAuditLogChecksum(parameters.getAuditLogChecksum());
        task.setContentChecksum(parameters.getContentChecksum());
        task.setContentIndexChecksum(parameters.getContentIndexChecksum());
        task.setStoreType(parameters.getStorageProviderType());
        task.setStoreChecksum(parameters.getStoreChecksum());
        queue.put(task.writeTask());
    }

    private void writeResult(final BitIntegrityResult result,
            final String auditLogChecksum,
            final String contentIndexChecksum,
            final String storeChecksum,
            final String contentChecksum,
            final BitLogStore bitLogStore,
            StorageProviderType storageProviderType,
            final BitIntegrityCheckTask bitTask,
            String details) throws TaskExecutionFailedException {
        if (result == BitIntegrityResult.SUCCESS) {
            writeSuccessResult(result, storeChecksum, bitTask, bitLogStore,
                    storageProviderType);
        } else {
            writeNonSuccessResult(result, auditLogChecksum,
                    contentIndexChecksum, storeChecksum, contentChecksum,
                    bitTask, bitLogStore, storageProviderType, details);
        }
    }

    private void writeSuccessResult(final BitIntegrityResult result,
            final String checksum,
            final BitIntegrityCheckTask bitTask,
            final BitLogStore bitLogStore,
            final StorageProviderType storageProviderType)
            throws TaskExecutionFailedException {
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Since the checksums match only log one of the checksum
                    // values
                    bitLogStore.write(bitTask.getAccount(),
                            bitTask.getStoreId(), bitTask.getSpaceId(),
                            bitTask.getContentId(), System.currentTimeMillis(),
                            storageProviderType, result, checksum, null, null,
                            null, null);
                    return "success";
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                    buildFailureMessage(
                            "Could not write successful result to BitLogStore",
                            bitTask, storageProviderType), e);
        }
        log.info(
                "Checksum result={} account={} storeId={} storeType={} space={}"
                        + " contentId={} checksum={}", result,
                bitTask.getAccount(), bitTask.getStoreId(),
                storageProviderType, bitTask.getSpaceId(),
                bitTask.getContentId(), checksum);
    }

    private void writeNonSuccessResult(final BitIntegrityResult result,
            final String auditLogChecksum,
            final String contentIndexChecksum,
            final String storeChecksum,
            final String contentChecksum,
            final BitIntegrityCheckTask bitTask,
            final BitLogStore bitLogStore,
            final StorageProviderType storageProviderType,
            final String details)

    throws TaskExecutionFailedException {
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Since the checksums match only log one of the checksum
                    // values
                    bitLogStore.write(bitTask.getAccount(),
                            bitTask.getStoreId(), bitTask.getSpaceId(),
                            bitTask.getContentId(), System.currentTimeMillis(),
                            storageProviderType, result, contentChecksum,
                            storeChecksum, auditLogChecksum,
                            contentIndexChecksum, details);
                    return "success";
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                    buildFailureMessage(
                            "Could not write non-successful result to BitLogStore",
                            bitTask, storageProviderType), e);
        }
        log
                .error("Checksum result={} account={} storeId={} storeType={} space={}"
                        + " contentId={} auditLogChecksum={} contentIndexChecksum={} storeChecksum={}"
                        + " contentChecksum={}", result, bitTask.getAccount(),
                        bitTask.getStoreId(), storageProviderType,
                        bitTask.getSpaceId(), bitTask.getContentId(),
                        auditLogChecksum, contentIndexChecksum, storeChecksum,
                        contentChecksum);
    }

    /**
     * @param parameters
     */
    protected boolean addAuditTask(BitCheckParameters parameters) {
        BitIntegrityCheckTask bitTask = parameters.getTask();

        Map<String, String> properties = parameters.getContentProperties();
        String mimeType = properties
                .get(StorageProvider.PROPERTIES_CONTENT_MIMETYPE);
        String size = properties.get(StorageProvider.PROPERTIES_CONTENT_SIZE);
        String user = properties
                .get(StorageProvider.PROPERTIES_CONTENT_CREATOR);

        try {
            AuditTask task = new AuditTask();
            task.setAccount(bitTask.getAccount());
            task.setStoreId(bitTask.getStoreId());
            task.setSpaceId(bitTask.getSpaceId());
            task.setContentId(bitTask.getContentId());
            task.setContentChecksum(parameters.getStoreChecksum());
            task.setContentMimetype(mimeType);
            task.setContentSize(size);
            task.setUserId(user);
            task.setContentProperties(parameters.getContentProperties());
            task.setAction(ActionType.ADD_CONTENT.name());
            task.setDateTime(String.valueOf(System.currentTimeMillis()));
            parameters.getAuditTaskQueue().put(task.writeTask());
            return true;
        } catch (Exception e) {
            log.error(
                    buildFailureMessage("failed to add task to audit queue",
                            bitTask, parameters.getStorageProviderType()), e);
            return false;
        }

    }

    /**
     * @param parameters
     * @return
     */
    protected boolean isContentChecksumOkay(StorageProviderType storageProvider,
            String contentChecksum,
            String otherChecksum) {
        if (BitIntegrityCheckTaskProcessor
                .isContentChecksumCalculated(storageProvider)
                && otherChecksum.equals(contentChecksum)) {
            return true;
        } else {
            return contentChecksum == null;
        }

    }

    /**
     * @param task
     * @return
     */
    protected boolean isLastAttempt(BitIntegrityCheckTask task) {
        return task.getAttempts() == TaskWorker.MAX_ATTEMPTS;
    }

    /**
     * @param task
     * @return
     */
    protected boolean isPenultimateAttempt(BitIntegrityCheckTask task) {
        return task.getAttempts() == TaskWorker.MAX_ATTEMPTS - 1;
    }

  
}