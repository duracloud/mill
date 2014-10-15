/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.util.Map;

import org.duracloud.common.queue.task.Task;
import org.duracloud.common.queue.task.TypedTask;
import org.duracloud.storage.domain.StorageProviderType;

/**
 * Provides the information necessary to describe a bit integrity error.
 * 
 * @author Daniel Bernstein 
 *         Date: 04/22/2014
 */
public class BitIntegrityErrorTask extends TypedTask {

    private static final String DESCRIPTION_KEY = "description";
    private static final String STORE_TYPE_KEY = "storeType";
    private static final String CONTENT_CHECKSUM_KEY = "contentChecksum";
    private static final String MANIFEST_CHECKSUM_CHECKSUM_KEY = "manifestChecksum";
    private static final String STORE_CHECKSUM_KEY = "storeChecksum";
    
    private String description;
    private StorageProviderType storeType;
    private String contentChecksum,
                   storeChecksum,
                   manifestChecksum;
                   
                   
    @Override
    public Task writeTask() {
        Task task = super.writeTask();
        task.setType(Task.Type.BIT_ERROR);
        Map<String,String> p = task.getProperties();
        p.put(DESCRIPTION_KEY, description);
        p.put(STORE_TYPE_KEY, storeType.name());
        p.put(CONTENT_CHECKSUM_KEY, contentChecksum);
        p.put(MANIFEST_CHECKSUM_CHECKSUM_KEY, manifestChecksum);
        p.put(STORE_CHECKSUM_KEY, storeChecksum);
        return task;
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.common.queue.task.TypedTask#readTask(org.duracloud.common.queue.task.Task)
     */
    @Override
    public void readTask(Task task) {
        super.readTask(task);
        this.description = task.getProperty(DESCRIPTION_KEY);
        this.contentChecksum = task.getProperty(CONTENT_CHECKSUM_KEY);
        this.storeChecksum = task.getProperty(STORE_CHECKSUM_KEY);
        this.storeType = StorageProviderType.valueOf(task.getProperty(STORE_TYPE_KEY));
        this.manifestChecksum = task.getProperty(MANIFEST_CHECKSUM_CHECKSUM_KEY);
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the storeType
     */
    public StorageProviderType getStoreType() {
        return storeType;
    }

    /**
     * @param storeType the storeType to set
     */
    public void setStoreType(StorageProviderType storeType) {
        this.storeType = storeType;
    }

    /**
     * @return the contentChecksum
     */
    public String getContentChecksum() {
        return contentChecksum;
    }

    /**
     * @param contentChecksum the contentChecksum to set
     */
    public void setContentChecksum(String contentChecksum) {
        this.contentChecksum = contentChecksum;
    }

    /**
     * @return the storeChecksum
     */
    public String getStoreChecksum() {
        return storeChecksum;
    }

    /**
     * @param storeChecksum the storeChecksum to set
     */
    public void setStoreChecksum(String storeChecksum) {
        this.storeChecksum = storeChecksum;
    }


    /**
     * @return the manifestChecksum
     */
    public String getManifestChecksum() {
        return manifestChecksum;
    }
    
    /**
     * @param manifestChecksum the manifestChecksum to set
     */
    public void setManifestChecksum(String manifestChecksum) {
        this.manifestChecksum = manifestChecksum;
    }
 
}
