/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats.aws;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.duracloud.account.db.model.StorageProviderAccount;
import org.duracloud.s3storage.S3StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

/**
 * @author Daniel Bernstein Date: Mar 1, 2016
 */
public class CloudWatchStorageStatsGatherer {
    private Logger log = LoggerFactory
            .getLogger(CloudWatchStorageStatsGatherer.class);

    private AmazonCloudWatchClient cloudWatchClient;
    private S3StorageProvider s3StorageProvider;
    public CloudWatchStorageStatsGatherer(AmazonCloudWatchClient cloudWatchClient, S3StorageProvider s3StorageProvider) {
        this.cloudWatchClient = cloudWatchClient;
        this.s3StorageProvider = s3StorageProvider;
    }

    public BucketStats getBucketStats(String spaceId) {
        log.info("Starting storage reporter for space {}", spaceId);
        String bucketName = s3StorageProvider.getBucketName(spaceId);
        BucketStats bucketDetails = new BucketStats();
        bucketDetails.setBucketName(bucketName);
        bucketDetails
                .setTotalItems(getTotalItems(bucketName, cloudWatchClient));
        bucketDetails
                .setTotalBytes(getTotalBytes(bucketName, cloudWatchClient));
        return bucketDetails;
    }

    /**
     * Retrieves the total number of items for a space
     *
     * @param bucketName
     * @return
     */
    private long getTotalItems(String bucketName,
                               AmazonCloudWatchClient client) {
        return getMetricData(bucketName,
                             "NumberOfObjects",
                             "AllStorageTypes",
                             client);
    }

    /**
     * Gets the total byte count for all storage types and combines them to
     * produce a single stored bytes value
     *
     * @param bucketName
     * @return
     */
    private long getTotalBytes(String bucketName,
                               AmazonCloudWatchClient client) {
        long totalBytes = 0;
        totalBytes += getMetricData(bucketName,
                                    "BucketSizeBytes",
                                    "StandardStorage",
                                    client);
        totalBytes += getMetricData(bucketName,
                                    "BucketSizeBytes",
                                    "StandardIAStorage",
                                    client);
        totalBytes += getMetricData(bucketName,
                                    "BucketSizeBytes",
                                    "ReducedRedundancyStorage",
                                    client);
        return totalBytes;
    }

    private long getMetricData(String bucketName,
                               String metricName,
                               String storageType,
                               AmazonCloudWatchClient client) {
        GetMetricStatisticsRequest request = buildRequest(bucketName,
                                                          metricName,
                                                          storageType);
        GetMetricStatisticsResult result = client.getMetricStatistics(request);

        List<Datapoint> datapoints = result.getDatapoints();
        if (datapoints.size() > 0) {
            return datapoints.get(0).getMaximum().longValue();
        } else {
            return 0;
        }
    }

    /**
     * Create the request to ask for bucket metrics
     *
     * @param bucketName
     * @param metricName
     * @param storageType
     * @return
     */
    private GetMetricStatisticsRequest buildRequest(String bucketName,
                                                    String metricName,
                                                    String storageType) {
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest();
        request.setMetricName(metricName);
        request.setNamespace("AWS/S3");
        request.setPeriod(360);
        request.setStatistics(Collections.singletonList("Maximum"));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        request.setStartTime(cal.getTime());
        request.setEndTime(new Date());

        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(new Dimension().withName("BucketName")
                .withValue(bucketName));
        dimensions.add(new Dimension().withName("StorageType")
                .withValue(storageType));
        request.setDimensions(dimensions);

        return request;
    }
}
