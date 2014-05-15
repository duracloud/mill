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
import org.duracloud.audit.AuditLogStore;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.storage.domain.StorageProviderType;
/**
 * The set of parameters used by BitCheckHandlers.
 * @author Daniel Bernstein
 *	       Date: May 13, 2014
 */
class BitCheckParameters {
    private BitIntegrityCheckTask task;
    private StorageProviderType storageProviderType;
    private String contentChecksum;
    private String storeChecksum;
    private String contentIndexChecksum;
    private AuditLogItem auditLogItem;
    private Map<String, String> contentProperties;
    private BitLogStore bitLogStore;
    private TaskQueue bitErrorQueue;
    private ContentIndexClient contentIndexClient;
    private AuditLogStore auditLogStore;
    private TaskQueue auditTaskQueue;
    private String details;
    /**
     * @param task
     * @param storageProviderType
     * @param contentChecksum
     * @param storeChecksum
     * @param contentIndexChecksum
     * @param auditLogItem
     * @param contentProperties
     * @param bitLogStore
     */
    public BitCheckParameters(BitIntegrityCheckTask task,
            StorageProviderType storageProviderType,
            String contentChecksum,
            String storeChecksum,
            String contentIndexChecksum,
            AuditLogItem auditLogItem,
            Map<String, String> contentProperties,
            BitLogStore bitLogStore,
            TaskQueue bitErrorQueue,
            ContentIndexClient contentIndexClient,
            AuditLogStore auditLogStore,
            TaskQueue auditTaskQueue){
        this.task = task;
        this.storageProviderType = storageProviderType;
        this.contentChecksum = contentChecksum;
        this.storeChecksum = storeChecksum;
        this.contentIndexChecksum = contentIndexChecksum;
        this.auditLogItem = auditLogItem;
        this.contentProperties = contentProperties;
        this.bitLogStore = bitLogStore;
        this.bitErrorQueue = bitErrorQueue;
        this.contentIndexClient = contentIndexClient;
        this.auditLogStore = auditLogStore;
        this.auditTaskQueue = auditTaskQueue;
    }
    
    /**
     * @return
     */
    public String getAuditLogChecksum() {
        if(this.auditLogItem == null){
            return null;
        }else{
            return auditLogItem.getContentMd5();
        }
    }
    
    /**
     * @return the bitErrorQueue
     */
    public TaskQueue getBitErrorQueue() {
        return bitErrorQueue;
    }

    /**
     * @return the task
     */
    public BitIntegrityCheckTask getTask() {
        return task;
    }
    
    /**
     * @return the storageProviderType
     */
    public StorageProviderType getStorageProviderType() {
        return storageProviderType;
    }
    
    /**
     * @return the auditLogItem
     */
    public AuditLogItem getAuditLogItem() {
        return auditLogItem;
    }
    
    /**
     * @return the contentProperties
     */
    public Map<String, String> getContentProperties() {
        return contentProperties;
    }
    
    /**
     * @return the contentChecksum
     */
    public String getContentChecksum() {
        return contentChecksum;
    }
    
    /**
     * @return the storeChecksum
     */
    public String getStoreChecksum() {
        return storeChecksum;
    }
    
    /**
     * @return the contentIndexChecksum
     */
    public String getContentIndexChecksum() {
        return contentIndexChecksum;
    }
    
    /**
     * @return the bitLogStore
     */
    public BitLogStore getBitLogStore() {
        return bitLogStore;
    }

    /**
     * @return
     */
    public ContentIndexClient getContentIndexClient() {
        return contentIndexClient;
    }
    
    /**
     * @return the auditLogStore
     */
    public AuditLogStore getAuditLogStore() {
        return auditLogStore;
    }
    
    /**
     * @return the auditTaskQueue
     */
    public TaskQueue getAuditTaskQueue() {
        return auditTaskQueue;
    }

    /**
     * @return
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * @param details the details to set
     */
    public void setDetails(String details) {
        this.details = details;
    }
}
