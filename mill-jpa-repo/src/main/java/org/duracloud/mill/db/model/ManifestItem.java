/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.db.model;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Daniel Bernstein
 *         Date: Sep 2, 2014
 */
@Entity
@Table(name = "manifest_item",
       uniqueConstraints = @UniqueConstraint(columnNames = { "account",
                                                             "store_id", 
                                                             "space_id", 
                                                             "content_id" }))
public class ManifestItem extends BaseEntity {
    private String account;
    private String storeId;
    private String storeType;
    private String spaceId;
    private String contentId;
    private String contentChecksum;
    
    public String getAccount() {
        return account;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public String getStoreId() {
        return storeId;
    }
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }
    public String getStoreType() {
        return storeType;
    }
    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }
    public String getSpaceId() {
        return spaceId;
    }
    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }
    public String getContentId() {
        return contentId;
    }
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
    public String getContentChecksum() {
        return contentChecksum;
    }
    public void setContentChecksum(String contentChecksum) {
        this.contentChecksum = contentChecksum;
    }
}
