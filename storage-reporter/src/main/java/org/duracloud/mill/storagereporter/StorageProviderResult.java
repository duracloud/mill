/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagereporter;

import org.duracloud.account.db.model.StorageProviderAccount;

/**
 * 
 * @author dbernstein 
 * @since: Jun 28, 2017
 */
public class StorageProviderResult {
    public static final long TB = 1000*1000*1000*1000l;
    private long totalBytes;
    private StorageProviderAccount storageProviderAccount;
    /**
     * @param storageProviderAccount
     * @param totalBytes
     */
    public StorageProviderResult(StorageProviderAccount storageProviderAccount,
                                 long totalBytes) {
        this.totalBytes = totalBytes;
        this.storageProviderAccount = storageProviderAccount;
    }

    /**
     * @return the storageProviderAccount
     */
    public StorageProviderAccount getStorageProviderAccount() {
        return storageProviderAccount;
    }
    
    /**
     * @return the totalBytes
     */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * @return
     */
    public boolean isOversubscribed() {
        return totalBytes > storageProviderAccount.getStorageLimit()*TB;
    }
    
}

