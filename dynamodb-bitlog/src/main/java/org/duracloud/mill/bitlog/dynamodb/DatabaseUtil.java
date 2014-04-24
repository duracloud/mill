package org.duracloud.mill.bitlog.dynamodb;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
/**
 * A utility class for creating and dropping the audit log table and indices.
 * 
 * @author Daniel Bernstein
 *
 */
public class DatabaseUtil {
    private static final Logger log = LoggerFactory.getLogger(DatabaseUtil.class);
    
    public static final String DEFAULT_LOCAL_ENDPOINT = "http://localhost:8000";

    private static final String STRING_ATTRIBUTE_TYPE = "S";

    public static void drop(AmazonDynamoDBClient client) {
        try {

            DeleteTableRequest request =
                new DeleteTableRequest().withTableName(DynamoDBBitLogItem.TABLE_NAME);

            DeleteTableResult result = client.deleteTable(request);

            log.info(result.toString());
        } catch (AmazonServiceException ase) {
            System.err.println("Failed to delete table "
                + DynamoDBBitLogItem.TABLE_NAME + " " + ase);
        }
    }

    public static void create(AmazonDynamoDBClient client) {
        String tableName = DynamoDBBitLogItem.TABLE_NAME;

        try {
            String hashKeyName = DynamoDBBitLogItem.ID_ATTRIBUTE;
            String rangeKeyName = DynamoDBBitLogItem.TIMESTAMP_ATTRIBUTE;
            long readCapacityUnits = 10;
            long writeCapacityUnits = 5;
            log.info("Creating table " + tableName);
            ArrayList<KeySchemaElement> ks = new ArrayList<KeySchemaElement>();
            ArrayList<AttributeDefinition> attributeDefinitions =
                new ArrayList<AttributeDefinition>();

            ks.add(new KeySchemaElement().withAttributeName(hashKeyName)
                                         .withKeyType(KeyType.HASH));
            attributeDefinitions.add(new AttributeDefinition().withAttributeName(hashKeyName)
                                                              .withAttributeType(STRING_ATTRIBUTE_TYPE));

            ks.add(new KeySchemaElement().withAttributeName(rangeKeyName)
                                         .withKeyType(KeyType.RANGE));
            attributeDefinitions.add(new AttributeDefinition().withAttributeName(rangeKeyName)
                                                              .withAttributeType("N"));

            // Provide initial provisioned throughput values as Java long data
            // types
            ProvisionedThroughput provisionedthroughput =
                new ProvisionedThroughput().withReadCapacityUnits(readCapacityUnits)
                                           .withWriteCapacityUnits(writeCapacityUnits);

            CreateTableRequest request =
                new CreateTableRequest().withTableName(tableName)
                                        .withKeySchema(ks)
                                        .withProvisionedThroughput(provisionedthroughput);


            request.setAttributeDefinitions(attributeDefinitions);

            CreateTableResult result = client.createTable(request);
            log.info("result: " + result.getTableDescription());
        } catch (AmazonServiceException ase) {
            System.err.println("Failed to create table "
                + tableName + " " + ase);
        }

    }


}
