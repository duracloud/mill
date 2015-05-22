/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.common.util.DateUtil;
import org.duracloud.common.util.DateUtil.DateFormat;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.db.model.ManifestItem;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorBase;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.hibernate.type.descriptor.sql.BitTypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes bit integrity check tasks. The logic for this block of
 * code is specified here:
 * https://wiki.duraspace.org/display/DSPINT/Bit+Integrity+Check+Logic+Version+2
 * 
 * @author Daniel Bernstein 
           Date: 10/15/2014
 */
public class BitIntegrityCheckTaskProcessor extends
                                           TaskProcessorBase {

    private static final Logger log = LoggerFactory
            .getLogger(BitIntegrityCheckTaskProcessor.class);

    private BitIntegrityCheckTask bitTask;
    private StorageProvider store;
    private ManifestStore manifestStore;
    private BitLogStore bitLogStore;
    private StorageProviderType storageProviderType;
    private TaskQueue bitErrorQueue;
    private TaskQueue auditTaskQueue;
    private static List<BitCheckHandler> HANDLERS = new LinkedList<>();
    private ContentChecksumHelper checksumHelper;

    private static long penultimateAttemptWaitMS = 5 * 60 * 1000;

    static {
        initializeHandlers();
    }

    public BitIntegrityCheckTaskProcessor(BitIntegrityCheckTask bitTask,
                                          StorageProvider store,
                                          ManifestStore manifestStore,
                                          StorageProviderType storageProviderType,
                                          BitLogStore bitLogStore,
                                          TaskQueue bitErrorQueue,
                                          TaskQueue auditTaskQueue,
                                          ContentChecksumHelper checksumHelper) {
        super(bitTask);
        this.bitTask = bitTask;
        this.store = store;
        this.storageProviderType = storageProviderType;
        this.bitLogStore = bitLogStore;
        this.bitErrorQueue = bitErrorQueue;
        this.auditTaskQueue = auditTaskQueue;
        this.manifestStore = manifestStore;
        this.checksumHelper = checksumHelper;
    }

    /**
     * 
     */
    private static void initializeHandlers() {
        HANDLERS.add(new SuccessfulCheckHandler());
        HANDLERS.add(new SourContentHandler());
        HANDLERS.add(new FailedStorageProviderChecksumHandler());
        HANDLERS.add(new FailedManifestAndNoMatchesChecksumHandler());
        HANDLERS.add(new ContentNotFoundHandler());
        HANDLERS.add(new NoRecordOfItemHandler());
    }

    protected static void sleep() {
        try {
            Thread.sleep(penultimateAttemptWaitMS);
        } catch (InterruptedException e) {
        }
    }

    @Override
    protected void executeImpl() throws TaskExecutionFailedException {

        BitCheckExecutionState state = new BitCheckExecutionState(bitTask,
                                                                  storageProviderType,
                                                                  bitLogStore,
                                                                  bitErrorQueue,
                                                                  auditTaskQueue,
                                                                  checksumHelper,
                                                                  manifestStore);

        Map<String, String> contentProperties = getContentProperties();
        state.setContentProperties(contentProperties);
        String storeChecksum = null;
        if (contentProperties != null) {
            storeChecksum = contentProperties
                    .get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
        }

        state.setStoreChecksum(storeChecksum);

        String manifestChecksum = getManifestChecksum();
        state.setManifestChecksum(manifestChecksum);
        boolean handled = false;
        for (BitCheckHandler handler : getHandlers()) {
            if (handler.handle(state)) {
                handled = true;
                break;
            }
        }

        if (!handled) {
            throw new TaskExecutionFailedException("Nobody was able to handle the task this time around.");
        }
    }

    /**
     * @return
     */
    private String getManifestChecksum() {

        try {
            ManifestItem item = this.manifestStore.getItem(this.bitTask
                    .getAccount(), this.bitTask.getStoreId(), this.bitTask
                    .getSpaceId(), this.bitTask.getContentId());
            return item.getContentChecksum();
        } catch (org.duracloud.error.NotFoundException e) {
            return null;
        }

    }

    private static class SuccessfulCheckHandler extends BitCheckHandler {
        @Override
        protected HandlerResult
                handleImpl(BitCheckExecutionState state) throws TaskExecutionFailedException {
            String manifestChecksum = state.getManifestChecksum();
            String storeChecksum = state.getStoreChecksum();

            if (storeChecksum != null && storeChecksum.equals(manifestChecksum)) {

                if (isContentChecksumOkay(state, manifestChecksum)) {
                    return new HandlerResult(BitIntegrityResult.SUCCESS, null);
                }
            }

            return new HandlerResult();
        }
    }

    private static class SourContentHandler extends BitCheckHandler {
        @Override
        protected HandlerResult
                handleImpl(BitCheckExecutionState state) throws TaskExecutionFailedException {

            String manifestChecksum = state.getManifestChecksum();
            String storeChecksum = state.getStoreChecksum();
            StorageProviderType storageProviderType = state
                    .getStorageProviderType();

            if (manifestChecksum != null
                    && manifestChecksum.equals(storeChecksum)
                    && isContentChecksumCalculated(storageProviderType)) {
                String contentChecksum = state.getContentChecksumHelper()
                        .getContentChecksum(manifestChecksum);
                if (!manifestChecksum.equals(contentChecksum)) {
                    String message = "Content appears to have gone sour: "
                            + "content checksum does not match the manifest and store";
                    addErrorTask(state, message);

                    return new HandlerResult(BitIntegrityResult.FAILURE,
                                             message);

                }
            }

            return new HandlerResult();
        }
    }

    private static class FailedStorageProviderChecksumHandler extends
            BitCheckHandler {
        @Override
        protected HandlerResult
                handleImpl(BitCheckExecutionState state) throws TaskExecutionFailedException {
            String manifestChecksum = state.getManifestChecksum();
            String storeChecksum = state.getStoreChecksum();
            StorageProviderType storageProviderType = state
                    .getStorageProviderType();

            if (manifestChecksum != null && storeChecksum != null
                    && !manifestChecksum.equals(storeChecksum)
                    && isContentChecksumCalculated(storageProviderType)) {
                String contentChecksum = state.getContentChecksumHelper()
                        .getContentChecksum(manifestChecksum);

                if (manifestChecksum.equals(contentChecksum)) {

                    if (isLastAttempt(state.getTask())) {
                        String message = "The storage provider's checksum did not match the others: "
                                + "the storage provider's checksumming process appears to have failed.";

                        addErrorTask(state, message);

                        return new HandlerResult(BitIntegrityResult.FAILURE,
                                                 message);
                    } else if (isPenultimateAttempt(state.getTask())) {
                        sleep();
                    }

                }
            }

            return new HandlerResult();
        }
    }

    private static class FailedManifestAndNoMatchesChecksumHandler extends BitCheckHandler {
        @Override
        protected HandlerResult
                handleImpl(BitCheckExecutionState state) throws TaskExecutionFailedException {
            String manifestChecksum = state.getManifestChecksum();
            String storeChecksum = state.getStoreChecksum();
            BitIntegrityCheckTask task = state.getTask();
            if (storeChecksum != null
                    && !storeChecksum.equals(manifestChecksum)) {
                
                    if (isLastAttempt(task)) {
                        //if content item is less than a day old, then assume mill hasn't processed and ignore.
                        Map<String,String> props = state.getContentProperties();
                        
                        String modified = props.get(StorageProvider.PROPERTIES_CONTENT_MODIFIED);
                        try {
                            Date modifiedDate = DateUtil.convertToDate(modified, DateFormat.DEFAULT_FORMAT);
                            Calendar c = Calendar.getInstance();
                            c.add(Calendar.DATE, -1);
                            Date oneDayAgo = c.getTime();
                            if(modifiedDate.after(oneDayAgo)){
                                String message = "The manifest entry's checksum ("+manifestChecksum+") did not match the others: " + 
                                        "The content item is less than 1 day old. It is most likely that " +
                                        "the item has not yet been processed by the mill. Therefore we are " + 
                                        "ignorning this content item for now.";
                                return new HandlerResult(BitIntegrityResult.IGNORE,
                                                         message);
                            }
                            
                        } catch (ParseException e) {
                            throw new BitIntegrityCheckTaskExecutionFailedException("failed to parse date using DateFormat.DEFAULT_FORMAT: "
                                                                                        + modified,
                                                                                e);
                        }
                        
                        String message = "The manifest entry's checksum("+manifestChecksum+") did not match the others: " + 
                                "the last update to the manifest must have failed or has " +
                                "not yet been processed by the audit system in the last 24 hours " + 
                                "due to major downtime for the mill or a fantastically backed-up audit queue.";

                        if(!isContentChecksumOkay(state,storeChecksum)){
                                message = MessageFormat
                                        .format("Neither the content checksum ({0}) nor the manifest checksum " +
                                    		"({1})  match the store checksum ({2}).",
                                                state.getContentChecksumHelper()
                                                     .getContentChecksum(storeChecksum),
                                                manifestChecksum,
                                                storeChecksum);
                                log.error(message + ": storeId = {}, spaceId = {}, contentId = {}",
                                          task.getStoreId(),
                                          task.getSpaceId(),
                                          task.getContentId());
                        }else{
                            log.error(message + "{}", task);
                        }

                        updateManifestChecksum(state, storeChecksum);
                        addErrorTask(state, message);
                        return new HandlerResult(BitIntegrityResult.FAILURE,
                                                 message);
                    } else if (isPenultimateAttempt(state.getTask())) {
                        sleep();
                    }
            }

            return new HandlerResult();
        }


        /**
         * @param state
         * @param checksum
         * @throws BitIntegrityCheckTaskExecutionFailedException 
         */
        private void updateManifestChecksum(BitCheckExecutionState state,
                                            String checksum) throws BitIntegrityCheckTaskExecutionFailedException {

            BitIntegrityCheckTask task = state.getTask();
            ManifestStore manifestStore = state.getManifestStore();
            String account = task.getAccount();
            String storeId = task.getStoreId();
            String spaceId = task.getSpaceId();
            String contentId = task.getContentId();
            try {
                
                
                ManifestItem item;

                //if the item doesn't exist, we must get the content mimetype and size 
                //from the storage provider
                String contentMimetype;
                String contentSize;

                try{ 
                    item = manifestStore.getItem(account,
                                                 storeId,
                                                 spaceId,
                                                 contentId);

                    contentMimetype = item.getContentMimetype();;
                    contentSize = item.getContentSize();

                }catch(org.duracloud.error.NotFoundException ex ){
                    Map<String, String> props = state.getContentProperties();
                    contentMimetype = props
                            .get(StorageProvider.PROPERTIES_CONTENT_MIMETYPE);
                    contentSize = props
                            .get(StorageProvider.PROPERTIES_CONTENT_SIZE);
                }

                manifestStore.addUpdate(account,
                                        storeId,
                                        spaceId,
                                        contentId,
                                        checksum,
                                        contentMimetype,
                                        contentSize,
                                        new Date());
                
            } catch (Exception ex) {
                throw new BitIntegrityCheckTaskExecutionFailedException(buildFailureMessage("failed to update manifest: "
                        + ex.getMessage(), task, state.getStorageProviderType()));
                
            }
        }
    }

    private static class ContentNotFoundHandler extends BitCheckHandler {
        @Override
        protected HandlerResult
                handleImpl(BitCheckExecutionState state) throws TaskExecutionFailedException {
            String storeChecksum = state.getStoreChecksum();
            String manifestChecksum = state.getManifestChecksum();

            StorageProviderType storageProviderType = state
                    .getStorageProviderType();
            BitIntegrityCheckTask task = state.getTask();

            if (storeChecksum == null && manifestChecksum != null) {
                String message = "The item is not in the Storage Provider, "
                        + "but is in the manifest. ";

                if (!isLastAttempt(task)) {
                    if (isPenultimateAttempt(task)) {
                        log.warn(buildFailureMessage(message
                                                             + "It is possible that "
                                                             + "content added audit tasks have not propagated through the mill.  "
                                                             + "Waiting a few minutes before making final attempt.",
                                                     task,
                                                     storageProviderType));
                        sleep();
                    }
                } else {
                    addErrorTask(state,
                                 message += "  Is the audit queue overloaded? Perhaps a delete event got dropped?");
                    return new HandlerResult(BitIntegrityResult.FAILURE,
                                             message);
                }
            }
            return new HandlerResult();
        }
    }

    private static class NoRecordOfItemHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckExecutionState state) {
            String storeChecksum = state.getStoreChecksum();
            String manifestChecksum = state.getManifestChecksum();

            StorageProviderType storageProviderType = state
                    .getStorageProviderType();

            if (storeChecksum == null && manifestChecksum == null) {

                String message = "No matching checksums for this content item could be found.  "
                        + "This bit integrity task is likely being processed after a deletion"
                        + " has been fully processed by duracloud. Ignoring...";
                log.warn(buildFailureMessage(message,
                                             state.getTask(),
                                             storageProviderType));
                return new HandlerResult(BitIntegrityResult.IGNORE, null);
            }
            return new HandlerResult();
        }
    }

    /**
     * @return
     */
    private List<BitCheckHandler> getHandlers() {
        return HANDLERS;
    }

    private Map<String, String>
            getContentProperties() throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public Map<String, String> retry() throws Exception {
                    return store.getContentProperties(bitTask.getSpaceId(),
                                                      bitTask.getContentId());
                }
            });
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(buildFailureMessage("Could not retrieve checksum from storage provider",
                                                                                        bitTask,
                                                                                        storageProviderType),
                                                                    e);
        }
    }

    private String buildFailureMessage(String message,
                                       BitIntegrityCheckTask bitTask,
                                       StorageProviderType storageProviderType) {
        return BitIntegrityHelper.buildFailureMessage(message,
                                                           bitTask,
                                                           storageProviderType);
    }

    /**
     * Sets the number of milliseconds that the processor should wait before
     * abandoning the task. Default is 5 minutes.
     * 
     * @param penultimateWaitMS
     */
    public static void setPenultimateWaitMS(long milliseconds) {
        penultimateAttemptWaitMS = milliseconds;
    }
}
