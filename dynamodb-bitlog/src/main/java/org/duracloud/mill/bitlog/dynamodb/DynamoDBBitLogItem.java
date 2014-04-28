/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog.dynamodb;

import org.duracloud.mill.bitlog.BitLogItem;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 25, 2014
 */
@DynamoDBTable(tableName = DynamoDBBitLogItem.TABLE_NAME)
public class DynamoDBBitLogItem implements BitLogItem{

    public static final String TABLE_NAME = "BitIntegrityLog";

    public static final String ACCOUNT_ATTRIBUTE = "Account";
    public static final String STORE_ID_ATTRIBUTE = "StoreId";
    public static final String SPACE_ID_ATTRIBUTE = "SpaceId";
    public static final String CONTENT_ID_ATTRIBUTE = "ContentId";
    public static final String CONTENT_CHECKSUM_ATTRIBUTE = "ContentChecksum";
    public static final String STORAGE_PROVIDER_CHECKSUM_ATTRIBUTE = "StorageProviderChecksum";
    public static final String AUDIT_LOG_CHECKSUM_ATTRIBUTE = "AuditLogChecksum";
    public static final String CONTENT_INDEX_CHECKSUM_ATTRIBUTE = "ContentIndexChecksum";
    public static final String STORE_TYPE_ATTRIBUTE = "StoreType";
    public static final String RESULT_ATTRIBUTE = "Result";
    public static final String DETAILS_ATTRIBUTE = "Details";

    public static final String TIMESTAMP_ATTRIBUTE = "TimeStamp";
    public static final String ID_TIMESTAMP_INDEX = "IdTimeStamp";
    public static final String ID_ATTRIBUTE = "ItemId";

    private String id;
    private String account;
    private String storeId;
    private String spaceId;
    private String contentId;
    private long timestamp;
    private String storeType;
    private String result;
    private String details;
    private String contentChecksum;
    private String storageProviderChecksum;
    private String auditLogChecksum;
    private String contentIndexChecksum;
    
    

    /**
     * Empty constructor necessary for dynamo mapper to instantiate instance.
     */
    public DynamoDBBitLogItem() {}
    
    /**
     * @param id
     * @param account
     * @param storeId
     * @param spaceId
     * @param contentId
     * @param timestamp
     * @param storeType
     * @param result
     * @param contentChecksum
     * @param storageProviderChecksum
     * @param auditLogChecksum
     * @param contentIndexChecksum
     * @param details
     */
    public DynamoDBBitLogItem(String id,
            String account,
            String storeId,
            String spaceId,
            String contentId,
            long timestamp,
            String storeType,
            String result,
            String contentChecksum,
            String storageProviderChecksum,
            String auditLogChecksum,
            String contentIndexChecksum,
            String details) {
        super();
        this.id = id;
        this.account = account;
        this.storeId = storeId;
        this.spaceId = spaceId;
        this.contentId = contentId;
        this.timestamp = timestamp;
        this.storeType = storeType;
        this.result = result;
        this.contentChecksum = contentChecksum;
        this.storageProviderChecksum = storageProviderChecksum;
        this.auditLogChecksum = auditLogChecksum;
        this.contentIndexChecksum = contentIndexChecksum;
        this.details = details;

    }

    @DynamoDBHashKey(attributeName = ID_ATTRIBUTE)
    public String getId() {
        return id;
    }
    
    @DynamoDBRangeKey(attributeName = TIMESTAMP_ATTRIBUTE)
    public long getTimestamp() {
        return timestamp;
    }


    @DynamoDBAttribute(attributeName = ACCOUNT_ATTRIBUTE)
    @Override
    public String getAccount() {
        return this.account;
    }

    @DynamoDBAttribute(attributeName = STORE_ID_ATTRIBUTE)
    @Override
    public String getStoreId() {
        return this.storeId;
    }

    @DynamoDBAttribute(attributeName = SPACE_ID_ATTRIBUTE)
    @Override
    public String getSpaceId() {
        return this.spaceId;
    }

    @DynamoDBAttribute(attributeName = CONTENT_ID_ATTRIBUTE)
    @Override
    public String getContentId() {
        return contentId;
    }

    
    /**
     * @return the result
     */
    @DynamoDBAttribute(attributeName = RESULT_ATTRIBUTE)
    @Override
    public String getResult() {
        return result;
    }

    
    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getDetails()
     */
    @DynamoDBAttribute(attributeName = DETAILS_ATTRIBUTE)
    @Override
    public String getDetails() {
        return this.details;
    }
    /**
     * @param result the result to set
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * @return the contentChecksum
     */
    @DynamoDBAttribute(attributeName = CONTENT_CHECKSUM_ATTRIBUTE)
    @Override
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
     * @return the storageProviderChecksum
     */
    @DynamoDBAttribute(attributeName = STORAGE_PROVIDER_CHECKSUM_ATTRIBUTE)
    @Override
    public String getStorageProviderChecksum() {
        return storageProviderChecksum;
    }

    /**
     * @param storageProviderChecksum the storageProviderChecksum to set
     */
    public void setStorageProviderChecksum(String storageProviderChecksum) {
        this.storageProviderChecksum = storageProviderChecksum;
    }

    /**
     * @return the auditLogChecksum
     */
    @DynamoDBAttribute(attributeName = AUDIT_LOG_CHECKSUM_ATTRIBUTE)
    @Override
    public String getAuditLogChecksum() {
        return auditLogChecksum;
    }

    /**
     * @param auditLogChecksum the auditLogChecksum to set
     */
    public void setAuditLogChecksum(String auditLogChecksum) {
        this.auditLogChecksum = auditLogChecksum;
    }

    /**
     * @return the contentIndexChecksum
     */
    @DynamoDBAttribute(attributeName = CONTENT_INDEX_CHECKSUM_ATTRIBUTE)
    @Override
    public String getContentIndexChecksum() {
        return contentIndexChecksum;
    }

    /**
     * @param contentIndexChecksum the contentIndexChecksum to set
     */
    public void setContentIndexChecksum(String contentIndexChecksum) {
        this.contentIndexChecksum = contentIndexChecksum;
    }


    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogItem#getStoreType()
     */
    @DynamoDBAttribute(attributeName = STORE_TYPE_ATTRIBUTE)
    @Override
    public String getStoreType() {
        return this.storeType;
    }
    
    /**
     * @param storeType the storeType to set
     */
    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }
    
    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param account the account to set
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * @param storeId the storeId to set
     */
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    /**
     * @param spaceId the spaceId to set
     */
    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    /**
     * @param contentId the contentId to set
     */
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * @param details the details to set
     */
    public void setDetails(String details) {
        this.details = details;
    }
}

