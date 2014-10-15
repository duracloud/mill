/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.db.model.BaseEntity;
import org.duracloud.storage.domain.StorageProviderType;

/**
 * @author Daniel Bernstein
 *         Date: Oct 17, 2014
 */
@Entity
@Table(name="bit_log_item")
public class JpaBitLogItem extends BaseEntity implements BitLogItem {
    @Column(nullable=false)
    private String account;
    @Column(nullable=false)
    private String storeId;
    @Column(nullable=false)
    private String spaceId;
    @Column(nullable=false, length=1024)
    private String contentId;
    private String contentChecksum;
    private String storageProviderChecksum;
    private String manifestChecksum;
    @Enumerated(EnumType.STRING)
    private StorageProviderType storageProviderType;
    @Enumerated(EnumType.STRING)
    private BitIntegrityResult result;
    @Column(length=1024)
    private String details;
    
    public StorageProviderType getStorageProviderType() {
        return storageProviderType;
    }

    public void setStorageProviderType(StorageProviderType storageProviderType) {
        this.storageProviderType = storageProviderType;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public void setContentChecksum(String contentChecksum) {
        this.contentChecksum = contentChecksum;
    }

    public void setStorageProviderChecksum(String storageProviderChecksum) {
        this.storageProviderChecksum = storageProviderChecksum;
    }

    public void setManifestChecksum(String manifestChecksum) {
        this.manifestChecksum = manifestChecksum;
    }

    public void setResult(BitIntegrityResult result) {
        this.result = result;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getAccount()
     */
    @Override
    public String getAccount() {
        return account;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getStoreId()
     */
    @Override
    public String getStoreId() {
        return storeId;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getSpaceId()
     */
    @Override
    public String getSpaceId() {
        return spaceId;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getContentId()
     */
    @Override
    public String getContentId() {
        return contentId;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getStoreType()
     */
    @Override
    public StorageProviderType getStoreType() {
        return storageProviderType;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getContentChecksum()
     */
    @Override
    public String getContentChecksum() {
        return this.contentChecksum;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getStorageProviderChecksum()
     */
    @Override
    public String getStorageProviderChecksum() {
        return this.storageProviderChecksum;
    }


    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getDetails()
     */
    @Override
    public String getDetails() {
        return this.details;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getManifestChecksum()
     */
    @Override
    public String getManifestChecksum() {
        return this.manifestChecksum;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getResult()
     */
    @Override
    public BitIntegrityResult getResult() {
        return this.result;
    }
    
}
