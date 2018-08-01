/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats.aws;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import org.duracloud.s3storage.S3StorageProvider;
import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 * Date: Mar 3, 2016
 */
@RunWith(EasyMockRunner.class)
public class CloudWatchStorageStatsGathererTest extends EasyMockSupport {

    @Mock
    private AmazonCloudWatch client;

    @Mock
    private S3StorageProvider storageProvider;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
    }

    @Test
    public void test() throws Exception {
        String space = "space-id";
        expect(storageProvider.getBucketName(space)).andReturn(space);
        int objectCount = 3;
        List<Capture<GetMetricStatisticsRequest>> requests = new LinkedList<>();

        int byteCount = 100;
        requests.add(setupRequest(objectCount));
        requests.add(setupRequest(byteCount));
        requests.add(setupRequest(byteCount));
        requests.add(setupRequest(byteCount));

        replayAll();
        CloudWatchStorageStatsGatherer gatherer = new CloudWatchStorageStatsGatherer(client, storageProvider);
        BucketStats details = gatherer.getBucketStats(space);
        assertEquals(3 * byteCount, details.getTotalBytes());
        assertEquals(objectCount, details.getTotalItems());

        verifyRequests(requests.get(0), space, "NumberOfObjects", "AllStorageTypes");
        verifyRequests(requests.get(1), space, "BucketSizeBytes", "StandardStorage");
        verifyRequests(requests.get(2), space, "BucketSizeBytes", "StandardIAStorage");
        verifyRequests(requests.get(3), space, "BucketSizeBytes", "ReducedRedundancyStorage");

    }

    /**
     * @param capture
     * @param bucket
     * @param string
     * @param string2
     */
    private void verifyRequests(Capture<GetMetricStatisticsRequest> capture,
                                String bucket,
                                String metric,
                                String storageType) {
        GetMetricStatisticsRequest request = capture.getValue();

        assertEquals(metric, request.getMetricName());
        assertEquals(bucket, request.getDimensions().get(0).getValue());
        assertEquals(storageType, request.getDimensions().get(1).getValue());

    }

    /**
     * @param bucket
     * @param string
     * @param string2
     */
    private Capture<GetMetricStatisticsRequest> setupRequest(int value) {
        Capture<GetMetricStatisticsRequest> capture = new Capture<>();
        GetMetricStatisticsResult result = createMock(GetMetricStatisticsResult.class);
        Datapoint dp = new Datapoint();
        dp.setMaximum((double) value);
        expect(result.getDatapoints()).andReturn(Arrays.asList(dp));
        expect(client.getMetricStatistics(capture(capture))).andReturn(result);
        return capture;
    }
}
