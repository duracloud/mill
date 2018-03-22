/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.util.Map;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.storage.domain.StorageProviderType;

/**
 * The execution state that visits BitCheckHandlers.
 *
 * @author Daniel Bernstein
 * Date: 10/15/2014
 */
class BitCheckExecutionState {
    private BitIntegrityCheckTask task;
    private StorageProviderType storageProviderType;
    private String manifestChecksum;
    private String storeChecksum;
    private Map<String, String> contentProperties;
    private BitLogStore bitLogStore;
    private TaskQueue bitErrorQueue;
    private TaskQueue auditTaskQueue;
    private String details;
    private ContentChecksumHelper helper;
    private ManifestStore manifestStore;

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
    public BitCheckExecutionState(BitIntegrityCheckTask task,
                                  StorageProviderType storageProviderType,
                                  BitLogStore bitLogStore,
                                  TaskQueue bitErrorQueue,
                                  TaskQueue auditTaskQueue,
                                  ContentChecksumHelper helper,
                                  ManifestStore manifestStore) {
        this.task = task;
        this.storageProviderType = storageProviderType;
        this.bitLogStore = bitLogStore;
        this.bitErrorQueue = bitErrorQueue;
        this.auditTaskQueue = auditTaskQueue;
        this.helper = helper;
        this.manifestStore = manifestStore;
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
     * @return the contentProperties
     */
    public Map<String, String> getContentProperties() {
        return contentProperties;
    }

    /**
     * @return the storeChecksum
     */
    public String getStoreChecksum() {
        return storeChecksum;
    }

    /**
     * @return the bitLogStore
     */
    public BitLogStore getBitLogStore() {
        return bitLogStore;
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

    public String getManifestChecksum() {
        return manifestChecksum;
    }

    public void setManifestChecksum(String manifestChecksum) {
        this.manifestChecksum = manifestChecksum;
    }

    public void setStoreChecksum(String storeChecksum) {
        this.storeChecksum = storeChecksum;
    }

    public void setContentProperties(Map<String, String> contentProperties) {
        this.contentProperties = contentProperties;
    }

    /**
     * @return
     */
    public ContentChecksumHelper getContentChecksumHelper() {
        return helper;
    }

    /**
     *
     */
    public ManifestStore getManifestStore() {
        return this.manifestStore;
    }

}
