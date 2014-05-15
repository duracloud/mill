/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.test;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
 * @author Daniel Bernstein Date: Apr 25, 2014
 */
public class DynamoDBTestUtil {
    public static AmazonDynamoDBClient createDynamoDBLocalClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(
                new BasicAWSCredentials("username", "password"));
        client.setRegion(Region.getRegion(Regions.DEFAULT_REGION));
        client.setEndpoint("http://localhost:"
                + System.getProperty("dynamodb.port", "8000"));

        return client;
    }
}
