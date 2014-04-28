/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog.dynamodb;

import java.util.Date;
import java.util.Iterator;

import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.test.DynamoDBTestUtil;
import org.duracloud.mill.util.dynamodb.ItemNotFoundException;
import org.duracloud.storage.domain.StorageProviderType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
 * @author Daniel Bernstein Date: Apr 25, 2014
 */
public class DynamoDBBitLogStoreIT {

    private AmazonDynamoDBClient client;
    private DynamoDBBitLogStore  store;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        client = DynamoDBTestUtil.createDynamoDBLocalClient();
        DatabaseUtil.create(client);

        store = new DynamoDBBitLogStore();
        store.initialize(client);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        DatabaseUtil.drop(client);
    }

    @Test
    public void testGetBitLogItemsNoResults() {
        Iterator<BitLogItem> items = this.store.getBitLogItems("account",
                "storeId", "spaceId", "contentId");
        Assert.assertFalse(items.hasNext());
    }

    @Test
    public void testGetBitLogItems() throws Exception{
        int stores = 2;
        int content = 5;
        int dates = 2;
        loadData(client, 2,stores,2,content,dates);
        
        
        int count = 0;
        Iterator<BitLogItem> items = this.store.getBitLogItems("account0",
                "store0", "space0", "content0");
        
        while(items.hasNext()){
            BitLogItem item = items.next();
            Assert.assertEquals(BitIntegrityResult.SUCCESS.name(), item.getResult());
            Assert.assertEquals("contentChecksum", item.getContentChecksum());
            Assert.assertEquals("storageProviderChecksum", item.getStorageProviderChecksum());
            Assert.assertEquals("contentIndexChecksum", item.getContentIndexChecksum());
            Assert.assertEquals("auditLogChecksum", item.getAuditLogChecksum());
            Assert.assertEquals("details", item.getDetails());
            Assert.assertEquals(StorageProviderType.AMAZON_S3.name(), item.getStoreType());

            count++;
        }

        Assert.assertEquals(2,count);
        
    }

    @Test
    public void testGetLatestBitLogItemNoResults() {
        try {
            BitLogItem item = this.store.getLatestBitLogItem("account",
                    "storeId", "spaceId", "contentId");
            Assert.fail();
        } catch (ItemNotFoundException ex) {
        }
    }

    private void loadData(AmazonDynamoDBClient client,
            int accounts,
            int stores,
            int spaces,
            int content,
            int dates) throws Exception {

        for (int i = 0; i < accounts; i++) {
            String account = "account" + i;

            for (int j = 0; j < stores; j++) {
                String storeId = "store" + j;

                for (int k = 0; k < spaces; k++) {
                    String spaceId = "space" + k;
                    for (int l = 0; l < content; l++) {
                        for (int m = 0; m < dates; m++) {
                            // add dates one day apart moving into the past.
                            Date timestamp = new Date(
                                    System.currentTimeMillis()
                                            - (24 * 60 * 60 * 1000 * m));
                            String contentId = "content" + l;
                            store.write(account, storeId, spaceId, contentId,
                                    timestamp.getTime(),
                                    StorageProviderType.AMAZON_S3,
                                    BitIntegrityResult.SUCCESS,
                                    "contentChecksum",
                                    "storageProviderChecksum",
                                    "auditLogChecksum", 
                                    "contentIndexChecksum",
                                    "details");
                        }
                    }
                }
            }
        }
    }
}