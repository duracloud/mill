/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.duracloud.audit.AuditLogItem;
import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.AuditLogWriteFailedException;
import org.duracloud.audit.task.AuditTask.ActionType;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.contentindex.client.ContentIndexItem;
import org.duracloud.mill.audit.AuditLogStoreUtil;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.contentindex.ContentIndexItemUtil;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes bit integrity check tasks.  The logic for this block of code is quite intricate.
 * For a clearer understanding of what it is supposed to do please refer to the following document:
 * https://wiki.duraspace.org/display/DSPINT/Duracloud+Mill+Overview#DuracloudMillOverview-BitIntegrityCheckTaskProcessor
 * @author Erik Paulsson
 *         Date: 4/23/14
 */
public class BitIntegrityCheckTaskProcessor implements TaskProcessor {

    static final Logger log =
        LoggerFactory.getLogger(BitIntegrityCheckTaskProcessor.class);

    private BitIntegrityCheckTask bitTask;
    private StorageProvider store;
    private AuditLogStore auditLogStore;
    private BitLogStore bitLogStore;
    private ContentIndexClient contentIndexClient;
    private StorageProviderType storageProviderType;
    private ChecksumUtil checksumUtil;
    private TaskQueue bitErrorQueue;
    private TaskQueue auditTaskQueue;
    private static List<BitCheckHandler> HANDLERS = new LinkedList<>();
    
    private static long penultimateAttemptWaitMS = 5*60*1000;
    /**
     * Designates the list of storage providers on whose content we perform checksum verifications.
     * Some storage providers content is not available for immediate download, such as Glacier and Chronopolis.
     * IMPORTANT: As new storage providers come online, we'll need to add them to this set if we want the content 
     * to be checked. TODO We should probably make them configurable on the commandline.
     * 
     */
    private static final Set<StorageProviderType> CONTENT_CHECKSUM_CALCULATING_STORAGE_PROVIDERS = new HashSet<>();
    
    static {
        initializeHandlers();
        initializeContentChecksumCalculatingStorageProviders();

    }

    /**
     * 
     */
    private static void initializeContentChecksumCalculatingStorageProviders() {
        CONTENT_CHECKSUM_CALCULATING_STORAGE_PROVIDERS.add(StorageProviderType.AMAZON_S3);
        CONTENT_CHECKSUM_CALCULATING_STORAGE_PROVIDERS.add(StorageProviderType.SDSC);
    }
    
    public BitIntegrityCheckTaskProcessor(BitIntegrityCheckTask bitTask,
                                          StorageProvider store,
                                          StorageProviderType storageProviderType,
                                          AuditLogStore auditLogStore,
                                          BitLogStore bitLogStore,
                                          ContentIndexClient contentIndexClient,
                                          ChecksumUtil checksumUtil,
                                          TaskQueue bitErrorQueue,
                                          TaskQueue auditTaskQueue) {
        this.bitTask = bitTask;
        this.store = store;
        this.storageProviderType = storageProviderType;
        this.auditLogStore = auditLogStore;
        this.bitLogStore = bitLogStore;
        this.contentIndexClient = contentIndexClient;
        this.checksumUtil = checksumUtil;
        this.bitErrorQueue = bitErrorQueue;
        this.auditTaskQueue = auditTaskQueue;
    }
    
    /**
     * 
     */
    private static void initializeHandlers() {
        HANDLERS.add(new SuccessfulCheckHandler());
        HANDLERS.add(new SourContentHandler());
        HANDLERS.add(new FailedStorageProviderChecksumHandler());
        HANDLERS.add(new FailedStorageProviderChecksumWithNoContentChecksumHandler());
        HANDLERS.add(new FailedContentIndexChecksumHandler());
        HANDLERS.add(new FailedAuditLogChecksumHandler());
        HANDLERS.add(new ContentNotFoundHandler());
        HANDLERS.add(new AuditAndContentIndexDoNotMatchStoreHandler());
        HANDLERS.add(new NoRecordOfItemHandler());
    }
    
    /**
     * @param parameters
     * @return
     */
    static boolean isContentChecksumCalculated(StorageProviderType storageProviderType) {
        return CONTENT_CHECKSUM_CALCULATING_STORAGE_PROVIDERS.contains(storageProviderType);
    }
    
    protected static  void sleep() {
        try {
            Thread.sleep(penultimateAttemptWaitMS);
        } catch (InterruptedException e) {
        }
    }
    
    @Override
    public void execute() throws TaskExecutionFailedException {
        String contentChecksum = getContentChecksum();
        String contentIndexChecksum = getContentIndexChecksum();
        Map<String,String> contentProperties = getContentProperties();
        String storeChecksum = null;
        if(contentProperties != null){
            storeChecksum = contentProperties.get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
        }
        
        AuditLogItem auditLogItem = getLatestAuditLogItem();
        
        BitCheckParameters parameters = new BitCheckParameters(bitTask, 
                                                               storageProviderType, 
                                                               contentChecksum, 
                                                               storeChecksum, 
                                                               contentIndexChecksum, 
                                                               auditLogItem, 
                                                               contentProperties, 
                                                               bitLogStore,
                                                               bitErrorQueue,
                                                               contentIndexClient,
                                                               auditLogStore,
                                                               auditTaskQueue);
        
        boolean handled = false;
        for(BitCheckHandler handler : getHandlers()){
            if(handler.handle(parameters)){
                handled = true;
                break;
            }
        }
        
        if(!handled){
            throw new TaskExecutionFailedException("Nobody was able to handle the task this time around.");
        }
    }


    private static class SuccessfulCheckHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {

            String auditLogChecksum = parameters.getAuditLogChecksum();
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            String contentChecksum = parameters.getContentChecksum();
            StorageProviderType storageProviderType = parameters.getStorageProviderType();

            if (auditLogChecksum != null && contentIndexChecksum != null
                    && storeChecksum != null) {
                if (storeChecksum.equals(contentIndexChecksum)
                        && storeChecksum.equals(auditLogChecksum)
                        && isContentChecksumOkay(
                                storageProviderType,
                                contentChecksum, storeChecksum)) {
                    return new HandlerResult(BitIntegrityResult.SUCCESS,null);
                }
            }
            
            return new HandlerResult();
        }
    }


    private static class SourContentHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {

            String auditLogChecksum = parameters.getAuditLogChecksum();
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            String contentChecksum = parameters.getContentChecksum();
            StorageProviderType storageProviderType = parameters.getStorageProviderType();


            if (auditLogChecksum != null 
                    && contentIndexChecksum != null
                    && storeChecksum != null 
                    && isContentChecksumCalculated(storageProviderType)) {
                if (storeChecksum.equals(contentIndexChecksum)
                        && storeChecksum.equals(auditLogChecksum)
                        &&  !contentChecksum.equals(storeChecksum)) {
                    String message = "Content appears to have gone sour: content checksum does not match: Content checksum: "
                            + contentChecksum
                            + ",  audit, content index, and store checksums: "
                            + storeChecksum;
                    
                    addErrorTask(parameters,
                            message);
                    
                    return new HandlerResult(BitIntegrityResult.FAILURE,message);

                }
            }
            
            return new HandlerResult();
        }
    }

    private static class FailedStorageProviderChecksumHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {
            String auditLogChecksum = parameters.getAuditLogChecksum();
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            String contentChecksum = parameters.getContentChecksum();
            StorageProviderType storageProviderType = parameters.getStorageProviderType();

            if (auditLogChecksum != null && contentIndexChecksum != null
                    && storeChecksum != null && isContentChecksumCalculated(storageProviderType)) {
                if (auditLogChecksum.equals(contentIndexChecksum) 
                        && isContentChecksumOkay(storageProviderType, contentChecksum, contentIndexChecksum)
                        && !storeChecksum.equals(contentIndexChecksum)){
                    String message = "The storage provider's checksum did not match the others: " +
                    		"the storage provider's checksumming process appears to have failed.";
                    
                    addErrorTask(parameters,
                            message);
                    
                    return new HandlerResult(BitIntegrityResult.FAILURE, message);
                    
                }
            }

            return new HandlerResult();
        }
    }

    private static class FailedStorageProviderChecksumWithNoContentChecksumHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {
            String auditLogChecksum = parameters.getAuditLogChecksum();
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            StorageProviderType storageProviderType = parameters.getStorageProviderType();
            BitIntegrityCheckTask task = parameters.getTask();

            if (auditLogChecksum != null && contentIndexChecksum != null
                    && storeChecksum != null
                    && !isContentChecksumCalculated(storageProviderType)) {
                if (auditLogChecksum.equals(contentIndexChecksum)
                        && !storeChecksum.equals(contentIndexChecksum)) {

                    if(!isLastAttempt(task)){
                        if(isPenultimateAttempt(task)){
                            log.warn(buildFailureMessage("The storage checksum does not match audit and content index. " +
                            		"It is possible that " +
                                    "content update audit events have not propagated through the mill.  " +
                                    "Waiting a few minutes before making final attempt.", 
                                    task, 
                                    storageProviderType));
                            sleep();
                        }
                        return new HandlerResult();
                    }                        


                    String message = "The storage provider's checksum did not match the others.  "
                            + "The storage provider's checksumming process appears to have failed.";
                    
                    addErrorTask(
                            parameters,
                            message);
                    
                    return new HandlerResult(BitIntegrityResult.FAILURE,message);
                }
            }

            return new HandlerResult();
        }
    }

    
    private static class FailedContentIndexChecksumHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {
            AuditLogItem auditItem = parameters.getAuditLogItem();
            String auditLogChecksum = parameters.getAuditLogChecksum();
            
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            StorageProviderType storageProviderType = parameters.getStorageProviderType();
            if (auditLogChecksum != null && contentIndexChecksum != null
                    && storeChecksum != null) {
                if (auditLogChecksum.equals(storeChecksum) 
                        && !storeChecksum.equals(contentIndexChecksum)
                        && isContentChecksumOkay(storageProviderType, 
                                                 parameters.getContentChecksum(), 
                                                 storeChecksum)) {
                    String message = "The storage provider's checksum did not match the others.  " +
                            "The storage provider's checksumming process appears to have failed.";
                    addErrorTask(
                            parameters,
                            message);
                    
                    //update content index
                    saveToContentIndex(parameters);
                    
                    updatePropertiesOnAuditLogStore(
                            parameters.getAuditLogStore(), auditItem,
                            parameters.getContentProperties());
                    
                    return new HandlerResult(BitIntegrityResult.FAILURE,message);
                }
            }
            return new HandlerResult();
        }

        /**
         * @param parameters
         * @param storageProviderType
         * @param task
         */
        private void saveToContentIndex(BitCheckParameters parameters) {
            final ContentIndexClient client = parameters.getContentIndexClient();
            final ContentIndexItem item = new ContentIndexItem();
            BitIntegrityCheckTask task = parameters.getTask();
            StorageProviderType storageProviderType = parameters
                    .getStorageProviderType();
            item.setAccount(task.getAccount());
            item.setStoreId(task.getStoreId());
            item.setSpace(task.getSpaceId());
            item.setContentId(task.getContentId());
            item.setStoreType(storageProviderType.name());
            item.setVersion(System.currentTimeMillis());

            ContentIndexItemUtil.setProps(parameters.getContentProperties(), item);
            item.setAccount(task.getAccount());

            try {
                new Retrier().execute(new Retriable() {
                    /*
                     * (non-Javadoc)
                     * 
                     * @see org.duracloud.common.retry.Retriable#retry()
                     */
                    @Override
                    public Object retry() throws Exception {
                        String id = client.save(item);
                        log.info(
                                "updated content item {} : id = {}", item, id);
                        return id;
                    }
                });
            } catch (Exception e) {
                log.error(
                        "failed to save content item: " + e.getMessage(), e);
            }
        }
        /**
         * @param parameters
         * @param auditItem
         */
        protected void updatePropertiesOnAuditLogStore(AuditLogStore auditLogStore,
                AuditLogItem auditItem, Map<String,String> contentProperties) {
            if (auditItem.getContentProperties() == null
                    && contentProperties != null
                    && !contentProperties.isEmpty()) {
                try {
                    auditLogStore.updateProperties(auditItem,
                            AuditLogStoreUtil.serialize(contentProperties));
                } catch (AuditLogWriteFailedException e) {
                    log.error("Failed to update properties: " + e.getMessage()
                            + " -> " + auditItem, e);
                }
            }
        }
    }

    private static class FailedAuditLogChecksumHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {
            String auditLogChecksum = parameters.getAuditLogChecksum();
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            StorageProviderType storageProviderType = parameters.getStorageProviderType();
            
            //Handle non-null & non matching audit log checksum case
            if (auditLogChecksum != null && contentIndexChecksum != null
                    && storeChecksum != null) {
                if (!auditLogChecksum.equals(storeChecksum)
                        && storeChecksum.equals(contentIndexChecksum)
                        && isContentChecksumOkay(storageProviderType,
                                parameters.getContentChecksum(), storeChecksum)) {
                    String message  = "The audit log item was corrupted because an insert failed "
                            + "or the checksum itself was corrupted in the process of insertion into the auditLogStore.";
                    addErrorTask(parameters,
                                 message);

                    return new HandlerResult(BitIntegrityResult.FAILURE,message);

                }
            }else if (auditLogChecksum == null && contentIndexChecksum != null
                    && storeChecksum != null) {
                //Handle null audit log case

                if (storeChecksum.equals(contentIndexChecksum)
                        && isContentChecksumOkay(storageProviderType,
                                parameters.getContentChecksum(), storeChecksum)) {
                    
                    String message = "The audit log item is null while content index and store checksums match. " +
                            "Probable causes: an audit log insert failed silently under the covers" +
                            " or the item was manually deleted.";
                    
                    log.error(buildFailureMessage(message, parameters.getTask(), parameters.getStorageProviderType()));

                    //try to correct the problem.
                    if(!addAuditTask(parameters)){
                        addErrorTask(
                                parameters,
                                message + " Attempts to correct the problem by patching the audit log failed.");
                    } else {
                        message += " Corrective action has been taken adding a new audit task to repair missing info.";
                    }

                    return new HandlerResult(BitIntegrityResult.FAILURE,message);
                }
            }
            
            return new HandlerResult();
        }
    }        
       
    private static class ContentNotFoundHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {
            String auditLogChecksum = parameters.getAuditLogChecksum();
            String contentChecksum = parameters.getContentChecksum();
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            StorageProviderType storageProviderType = parameters
                    .getStorageProviderType();
            BitIntegrityCheckTask task = parameters.getTask();

            if (auditLogChecksum != null && contentIndexChecksum != null
                    && auditLogChecksum.equals(contentIndexChecksum)
                    && storeChecksum == null
                    && isContentChecksumCalculated(storageProviderType)
                    && contentChecksum == null) {
                String message = "The item is not in the Storage Provider, "
                        + "but in content index and audit storage. ";

                if (!isLastAttempt(task)) {
                    if (isPenultimateAttempt(task)) {
                        log.warn(buildFailureMessage(
                                message
                                        + "It is possible that "
                                        + "content added audit tasks have not propagated through the mill.  "
                                        + "Waiting a few minutes before making final attempt.",
                                task, storageProviderType));
                        sleep();
                    }
                } else {
                    addErrorTask(
                            parameters,
                            message += "  Is the audit queue overloaded? Perhaps a delete event got dropped?");
                    return new HandlerResult(BitIntegrityResult.FAILURE,message);
                }
            }
            return new HandlerResult();
        }
    }
    
    private static class AuditAndContentIndexDoNotMatchStoreHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {
            String auditLogChecksum = parameters.getAuditLogChecksum();
            String contentChecksum = parameters.getContentChecksum();
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            StorageProviderType storageProviderType = parameters.getStorageProviderType();
            BitIntegrityCheckTask task = parameters.getTask();

            if (storeChecksum != null
                    && isContentChecksumCalculated(storageProviderType)
                    && contentChecksum != null
                    && ((auditLogChecksum == null && contentIndexChecksum == null) || 
                        (auditLogChecksum != null && contentIndexChecksum != null))
                ) {

                if(!storeChecksum.equals(contentIndexChecksum) 
                        && !storeChecksum.equals(auditLogChecksum)
                        && storeChecksum.equals(contentChecksum)) {
                    
                    String message = "The content and storage provider checksums match, " +
                    		         "but the content index and audit checksums do match the store.";
                    
                    if(!isLastAttempt(task)){
                        if(isPenultimateAttempt(task)){
                            log.warn(buildFailureMessage(message +" It is possible that " +
                                    "content add or update audit tasks have not propagated through the mill.  " +
                                    "Waiting a few minutes before making final attempt.", 
                                    task, 
                                    storageProviderType));
                            sleep();
                        }
                    }else{                        

                        message +=" It is possible that " +
                                "content add or update audit tasks have not propagated through the mill.  " +
                                "Waiting a few minutes before making final attempt.";
                        
                        log.warn(buildFailureMessage(message, 
                                task, 
                                storageProviderType));

                        if(!addAuditTask(parameters)){
                            addErrorTask(
                                    parameters,
                                    message + " Attempts to correct the problem by patching the audit log failed.");
                        }

                        return new HandlerResult(BitIntegrityResult.FAILURE,message);
                    }
                }
            }
            return new HandlerResult();
        }
    }

    private static class NoRecordOfItemHandler extends BitCheckHandler {
        @Override
        protected HandlerResult handleImpl(BitCheckParameters parameters) {
            String auditLogChecksum = parameters.getAuditLogChecksum();
            String contentChecksum = parameters.getContentChecksum();
            String contentIndexChecksum = parameters.getContentIndexChecksum();
            String storeChecksum = parameters.getStoreChecksum();
            StorageProviderType storageProviderType = parameters
                    .getStorageProviderType();

            if (storeChecksum == null && contentChecksum == null
                    && auditLogChecksum == null && contentIndexChecksum == null) {

                String message = "No matching checksums for this content item could be found.  "
                        + "This bit integrity task is likely being processed after a deletion"
                        + " has been fully processed by duracloud. Ignoring...";
                log.warn(buildFailureMessage(message, parameters.getTask(),
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

 
    private AuditLogItem getLatestAuditLogItem() throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public AuditLogItem retry() throws Exception {
                    try{
                        AuditLogItem item =  auditLogStore.getLatestLogItem(
                                bitTask.getAccount(),
                                bitTask.getStoreId(),
                                bitTask.getSpaceId(),
                                 bitTask.getContentId());
                        
                        if(ActionType.DELETE_CONTENT.equals(item.getAction())){
                            return null;
                        }
                        
                        return item;
                    }catch(NotFoundException ex){
                        log.warn("No matching audit log item found for {}", bitTask);
                        return null;
                    }
                }
            });
        } catch (Exception e) {
            
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage("Could not find latest audit log item",bitTask, storageProviderType), e);
        }
    }

    private String getContentIndexChecksum() throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    ContentIndexItem item = contentIndexClient
                        .get(bitTask.getAccount(), bitTask.getStoreId(),
                             bitTask.getSpaceId(), bitTask.getContentId());
                    if(item != null){
                        return item.getProps().get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
                    }else{
                        log.debug("content index item {} is null.", item);
                        return null;
                    }
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage("Failed to talk to content item index", bitTask, storageProviderType),e);
        }
    }

    private Map<String,String> getContentProperties() throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public Map<String,String> retry() throws Exception {
                    return store.getContentProperties(bitTask.getSpaceId(),
                                                      bitTask.getContentId());
                }
            });
        } catch(NotFoundException e){
            return null;
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage(
                    "Could not retrieve checksum from storage provider",bitTask,storageProviderType), e);
        }
    }

    private String getContentChecksum() throws TaskExecutionFailedException {
        if(!isContentChecksumCalculated(storageProviderType)){
            return null;
        }
        

        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    try(InputStream inputStream = store.getContent(bitTask.getSpaceId(),
                                                                   bitTask.getContentId())) {
                        return checksumUtil.generateChecksum(inputStream);
                    }
                }
            });
        } catch(NotFoundException ex){
            return null;
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage(
                    "Could not compute checksum from content stream", bitTask, storageProviderType), e);
        }
    }

    private  String buildFailureMessage(String message,
            BitIntegrityCheckTask bitTask,
            StorageProviderType storageProviderType) {
        return BitIntegrityMessageUtil.buildFailureMessage(message, bitTask, storageProviderType);
    }
    
    /**
     * Sets the number of milliseconds that the processor should wait before abandoning the 
     * task.  Default is 5 minutes.
     * @param penultimateWaitMS
     */
    public static void setPenultimateWaitMS(long milliseconds) {
        penultimateAttemptWaitMS = milliseconds;
    }
}
