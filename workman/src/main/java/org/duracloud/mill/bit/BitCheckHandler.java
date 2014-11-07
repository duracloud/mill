/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
 * 
 * @author Daniel Bernstein Date: May 15, 2014
 */
abstract class BitCheckHandler {

    private static Logger log = LoggerFactory.getLogger(BitCheckHandler.class);

    private static long penultimateAttemptWaitMS = 5 * 60 * 1000;
    /**
     * Designates the list of storage providers on whose content we perform
     * checksum verifications. Some storage providers content is not available
     * for immediate download, such as Glacier and Chronopolis. IMPORTANT: As
     * new storage providers come online, we'll need to add them to this set if
     * we want the content to be checked. TODO We should probably make them
     * configurable on the commandline.
     * 
     */
    private static final Set<StorageProviderType> CONTENT_CHECKSUM_CALCULATING_STORAGE_PROVIDERS = new HashSet<>();

    static {
        initializeContentChecksumCalculatingStorageProviders();
    }

    /**
     * 
     */
    private static void initializeContentChecksumCalculatingStorageProviders() {
        CONTENT_CHECKSUM_CALCULATING_STORAGE_PROVIDERS
                .add(StorageProviderType.AMAZON_S3);
        CONTENT_CHECKSUM_CALCULATING_STORAGE_PROVIDERS
                .add(StorageProviderType.SDSC);

    }

    public final boolean
            handle(BitCheckExecutionState bitCheckState) throws TaskExecutionFailedException {
        HandlerResult result = handleImpl(bitCheckState);
        if (result.isHandled()) {
            if (!result.getResult().equals(BitIntegrityResult.IGNORE)) {

                String contentChecksum = null;
                String storeChecksum = bitCheckState.getStoreChecksum();
                
                if (storeChecksum != null && isContentChecksumCalculated(bitCheckState
                        .getStorageProviderType())) {
                    contentChecksum = bitCheckState
                            .getContentChecksumHelper()
                            .getContentChecksum(bitCheckState.getManifestChecksum());
                }

                writeResult(result.getResult(),
                            bitCheckState.getManifestChecksum(),
                            bitCheckState.getStoreChecksum(),
                            contentChecksum,
                            bitCheckState.getBitLogStore(),
                            bitCheckState.getStorageProviderType(),
                            bitCheckState.getTask(),
                            result.getMessage());
            }
            return true;
        } else {
            return false;
        }
    }

    protected String
            buildFailureMessage(String message,
                                BitIntegrityCheckTask bitTask,
                                StorageProviderType storageProviderType) {
        return BitIntegrityHelper.buildFailureMessage(message,
                                                           bitTask,
                                                           storageProviderType);
    }

    protected boolean
            isContentChecksumOkay(BitCheckExecutionState state,
                                  String otherChecksum) throws TaskExecutionFailedException {
        if (isContentChecksumCalculated(state.getStorageProviderType())) {
            return otherChecksum.equals(state.getContentChecksumHelper()
                    .getContentChecksum(otherChecksum));
        } else {
            return true;
        }
    }

    /**
     * @param parameters
     * @return
     */
    protected boolean
            isContentChecksumCalculated(StorageProviderType storageProviderType) {
        return CONTENT_CHECKSUM_CALCULATING_STORAGE_PROVIDERS
                .contains(storageProviderType);
    }

    /**
     * 
     * @param parameters
     * @return
     * @throws TaskExecutionFailedException
     */
    abstract protected HandlerResult
            handleImpl(BitCheckExecutionState parameters) throws TaskExecutionFailedException;

    /**
     * @param bitTask
     * @param message
     */
    protected void
            addErrorTask(BitCheckExecutionState state, String message) throws TaskExecutionFailedException {
        String manifestChecksum = state.getManifestChecksum();
        BitIntegrityCheckTask bitTask = state.getTask();
        TaskQueue queue = state.getBitErrorQueue();
        String storeChecksum = state.getStoreChecksum();
        BitIntegrityErrorTask task = new BitIntegrityErrorTask();
        task.setAccount(bitTask.getAccount());
        task.setStoreId(bitTask.getStoreId());
        task.setSpaceId(bitTask.getSpaceId());
        task.setDescription(message);
        if (storeChecksum != null && isContentChecksumCalculated(state.getStorageProviderType())) {
            task.setContentChecksum(state.getContentChecksumHelper()
                    .getContentChecksum(manifestChecksum));
        }
        task.setStoreType(state.getStorageProviderType());
        task.setStoreChecksum(storeChecksum);
        task.setManifestChecksum(manifestChecksum);
        queue.put(task.writeTask());
    }

    private void
            writeResult(final BitIntegrityResult result,
                        final String manifestChecksum,
                        final String storeChecksum,
                        final String contentChecksum,
                        final BitLogStore bitLogStore,
                        StorageProviderType storageProviderType,
                        final BitIntegrityCheckTask bitTask,
                        String details) throws TaskExecutionFailedException {
        if (result == BitIntegrityResult.SUCCESS) {
            writeSuccessResult(result,
                               storeChecksum,
                               bitTask,
                               bitLogStore,
                               storageProviderType);
        } else {
            writeNonSuccessResult(result,
                                  manifestChecksum,
                                  storeChecksum,
                                  contentChecksum,
                                  bitTask,
                                  bitLogStore,
                                  storageProviderType,
                                  details);
        }
    }

    private void
            writeSuccessResult(final BitIntegrityResult result,
                               final String checksum,
                               final BitIntegrityCheckTask bitTask,
                               final BitLogStore bitLogStore,
                               final StorageProviderType storageProviderType) throws TaskExecutionFailedException {
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Since the checksums match only log one of the checksum
                    // values
                    bitLogStore.write(bitTask.getAccount(),
                                      bitTask.getStoreId(),
                                      bitTask.getSpaceId(),
                                      bitTask.getContentId(),
                                      new Date(System.currentTimeMillis()),
                                      storageProviderType,
                                      result,
                                      checksum,
                                      null,
                                      null,
                                      null);
                    return "success";
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(buildFailureMessage("Could not write successful result to BitLogStore",
                                                                                        bitTask,
                                                                                        storageProviderType),
                                                                    e);
        }
        log.info("Checksum result={} account={} storeId={} storeType={} space={}"
                         + " contentId={} checksum={}",
                 result,
                 bitTask.getAccount(),
                 bitTask.getStoreId(),
                 storageProviderType,
                 bitTask.getSpaceId(),
                 bitTask.getContentId(),
                 checksum);
    }

    private void
            writeNonSuccessResult(final BitIntegrityResult result,
                                  final String manifestChecksum,
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
                                      bitTask.getStoreId(),
                                      bitTask.getSpaceId(),
                                      bitTask.getContentId(),
                                      new Date(System.currentTimeMillis()),
                                      storageProviderType,
                                      result,
                                      contentChecksum,
                                      storeChecksum,
                                      manifestChecksum,
                                      details);
                    return "success";
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(buildFailureMessage("Could not write non-successful result to BitLogStore",
                                                                                        bitTask,
                                                                                        storageProviderType),
                                                                    e);
        }
        log.error("Checksum result={} account={} storeId={} storeType={} space={}"
                          + " contentId={} manifestChecksum={} storeChecksum={}"
                          + " contentChecksum={}",
                  result,
                  bitTask.getAccount(),
                  bitTask.getStoreId(),
                  storageProviderType,
                  bitTask.getSpaceId(),
                  bitTask.getContentId(),
                  manifestChecksum,
                  storeChecksum,
                  contentChecksum);
    }

    /**
     * @param state
     */
    protected boolean addAuditTask(BitCheckExecutionState state) {
        BitIntegrityCheckTask bitTask = state.getTask();

        Map<String, String> properties = state.getContentProperties();
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
            task.setContentChecksum(state.getStoreChecksum());
            task.setContentMimetype(mimeType);
            task.setContentSize(size);
            task.setUserId(user);
            task.setContentProperties(state.getContentProperties());
            task.setAction(ActionType.ADD_CONTENT.name());
            task.setDateTime(String.valueOf(System.currentTimeMillis()));
            state.getAuditTaskQueue().put(task.writeTask());
            return true;
        } catch (Exception e) {
            log.error(buildFailureMessage("failed to add task to audit queue",
                                          bitTask,
                                          state.getStorageProviderType()),
                      e);
            return false;
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