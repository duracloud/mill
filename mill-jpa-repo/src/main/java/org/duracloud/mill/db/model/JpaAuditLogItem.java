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

import org.duracloud.audit.AuditLogItem;
/**
 * 
 * @author Daniel Bernstein
 *
 */
@Entity
@Table(name = "audit_log_item",
       uniqueConstraints = @UniqueConstraint(columnNames = { "account",
                                                             "store_id", 
                                                             "space_id", 
                                                             "content_md5", 
                                                             "timestamp" }))
public class JpaAuditLogItem extends BaseEntity implements AuditLogItem {
    private String account;
    private String storeId;
    private String spaceId;
    private String contentId;
    private String contentMd5;
    private String mimetype;
    private String contentSize;
    private String contentProperties;
    private String spaceAcls;
    private String action;
    private String username;
    private String sourceSpaceId;
    private String sourceContentId;
    private Long timestamp;
    
    @Override
    public String getAccount() {
        return this.account;
    }

    @Override
    public String getStoreId() {
        return this.storeId;
    }

    @Override
    public String getSpaceId() {
        return this.spaceId;
    }

    @Override
    public String getContentId() {
        return this.contentId;
    }

    @Override
    public String getContentMd5() {
        return this.contentMd5;
    }

    @Override
    public String getMimetype() {
        return this.mimetype;
    }

    @Override
    public String getContentSize() {
        return this.contentSize;
    }

    @Override
    public String getContentProperties() {
        return this.contentProperties;
    }

    @Override
    public String getSpaceAcls() {
        return this.spaceAcls;
    }

    @Override
    public String getAction() {
        return this.action;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getSourceSpaceId() {
        return this.sourceSpaceId;
    }

    @Override
    public String getSourceContentId() {
        return this.sourceContentId;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
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

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setContentSize(String contentSize) {
        this.contentSize = contentSize;
    }

    public void setContentProperties(String contentProperties) {
        this.contentProperties = contentProperties;
    }

    public void setSpaceAcls(String spaceAcls) {
        this.spaceAcls = spaceAcls;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setSourceSpaceId(String sourceSpaceId) {
        this.sourceSpaceId = sourceSpaceId;
    }

    public void setSourceContentId(String sourceContentId) {
        this.sourceContentId = sourceContentId;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    
}
