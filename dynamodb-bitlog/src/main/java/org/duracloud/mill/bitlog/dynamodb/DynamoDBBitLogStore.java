/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog.dynamodb;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.duracloud.common.collection.StreamingIterator;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.util.dynamodb.DynamoDBIteratorSource;
import org.duracloud.mill.util.dynamodb.ItemNotFoundException;
import org.duracloud.mill.util.dynamodb.ItemWriteFailedException;
import org.duracloud.mill.util.dynamodb.KeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.Select;

/**
 * @author Daniel Bernstein Date: Apr 25, 2014
 */
public class DynamoDBBitLogStore implements BitLogStore {

    private final Logger         log = LoggerFactory
                                             .getLogger(DynamoDBBitLogStore.class);

    private AmazonDynamoDBClient client;
    private DynamoDBMapper mapper;

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.bitlog.BitLogStore#write(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, long,
     * org.duracloud.mill.bitlog.BitIntegrityResult, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public BitLogItem write(String account,
            String storeId,
            String spaceId,
            String contentId,
            long timestamp,
            BitIntegrityResult result,
            String contentChecksum,
            String storageProviderChecksum,
            String auditLogChecksum,
            String contentIndexChecksum,
            String details) throws ItemWriteFailedException {

        checkInitialized();
        DynamoDBBitLogItem item = null;
        
        try {
            item =
                new DynamoDBBitLogItem(KeyUtil.calculateContentIdPathBasedHashKey(account,
                                                                          storeId,
                                                                          spaceId,
                                                                          contentId),
                                         account,
                                         storeId,
                                         spaceId,
                                         contentId,
                                         timestamp,
                                         result.name(),
                                         contentChecksum, 
                                         storageProviderChecksum,
                                         auditLogChecksum,
                                         contentIndexChecksum,
                                         details);
            
            
            mapper.save(item);
            log.debug("Item written:  Result: {}", item);
            return item;
        } catch (AmazonClientException ex) {
            String message = "failed to write to db: " + item  + ": " + ex.getMessage();
            log.error(message);
            throw new ItemWriteFailedException(ex, message);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.bitlog.BitLogStore#getBitLogItems(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Iterator<BitLogItem> getBitLogItems(String account,
            String storeId,
            String spaceId,
            String contentId) {
        return getLogItems(account, storeId, spaceId, contentId, true, 0);
    }

    protected Iterator<BitLogItem> getLogItems(String account,
            String storeId,
            String spaceId,
            String contentId,
            boolean ascending,
            int limit) {
        checkInitialized();
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(
                DynamoDBBitLogItem.ID_ATTRIBUTE,
                new Condition().withComparisonOperator(ComparisonOperator.EQ)
                        .withAttributeValueList(
                                new AttributeValue(KeyUtil
                                        .calculateContentIdPathBasedHashKey(
                                                account, storeId, spaceId,
                                                contentId))));

        QueryRequest request = new QueryRequest(DynamoDBBitLogItem.TABLE_NAME)
                .withKeyConditions(keyConditions).withScanIndexForward(
                        ascending);

        if (limit > 0) {
            request.setLimit(limit);
        }

        request.setSelect(Select.ALL_ATTRIBUTES);
        return new StreamingIterator<BitLogItem>(
                new DynamoDBIteratorSource<BitLogItem>(client, request,
                        DynamoDBBitLogItem.class));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.bitlog.BitLogStore#getLatestBitLogItem(java.lang.String
     * , java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public BitLogItem getLatestBitLogItem(String account,
            String storeId,
            String spaceId,
            String contentId) throws ItemNotFoundException {

        Iterator<BitLogItem> items = getLogItems(account, storeId, spaceId,
                contentId, false, 1);

        if (items.hasNext()) {
            return items.next();
        }

        throw new ItemNotFoundException(MessageFormat.format(
                "No items found with path {0}/{1}/{2}/{3}", account, storeId,
                spaceId, contentId));
    }

    /**
     * @param client
     */
    public void initialize(AmazonDynamoDBClient client) {
        this.client = client;
        this.mapper = new DynamoDBMapper(client);
    }

    private void checkInitialized() {
        if (null == client) {
            StringBuilder err = new StringBuilder(getClass().getSimpleName()
                    + " must be ");
            err.append("initialized!");
            log.error(err.toString());
            throw new DuraCloudRuntimeException(err.toString());
        }
    }

}
