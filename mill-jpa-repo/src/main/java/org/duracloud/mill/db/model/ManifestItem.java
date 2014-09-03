/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author Daniel Bernstein
 *         Date: Sep 2, 2014
 */
@Entity
@Table(name = "manifest_item",
       uniqueConstraints = @UniqueConstraint(columnNames = { "unique_key"}))
public class ManifestItem extends BaseEntity {
    @Column(nullable=false)
    private String account;
    @Column(nullable=false)
    private String storeId;
    @Column(nullable=false)
    private String storeType;
    @Column(nullable=false)
    private String spaceId;
    @Column(nullable=false, length=1024)
    private String contentId;
    @Column(nullable=false)
    private String contentChecksum;

    @Column(columnDefinition="unique_key char(32) NOT NULL")
    private String uniqueKey;

    
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
    
    public String getUniqueKey() {
        if(uniqueKey == null){
            this.uniqueKey = DigestUtils.md5Hex(this.account + "/" +
                                                this.storeId + "/" + 
                                                this.spaceId + "/"+ 
                                                this.contentId);
        }
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }
}
