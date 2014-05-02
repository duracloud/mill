/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import org.duracloud.mill.ltp.Morsel;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 28, 2014
 */
public class BitIntegrityMorsel extends Morsel {
    private String storeId;
    private String storageProviderType;
    
    public BitIntegrityMorsel(){
        super();
    }
    
    /**
     * @param account
     * @param storeId
     * @param storageProviderType
     * @param spaceId
     */
    public BitIntegrityMorsel(String account,
            String storeId,
            String storageProviderType,
            String spaceId) {
        super(account, spaceId, null);
        setStoreId(storeId);
        setStorageProviderType(storageProviderType);
    }

    /**
     * @return the storageProviderType
     */
    public String getStorageProviderType() {
        return storageProviderType;
    }
    
    /**
     * @param storageProviderType the storageProviderType to set
     */
    public void setStorageProviderType(String storageProviderType) {
        this.storageProviderType = storageProviderType;
    }
    
    /**
     * @return the storeId
     */
    public String getStoreId() {
        return storeId;
    }
    
    /**
     * @param storeId the storeId to set
     */
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }
}
