/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.Set;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.S3DuplicationPolicyRepo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for the DuplicatonPolicyManager which interacts with S3
 *
 * NOTE: This test expects that environment variables AWS_ACCESS_KEY_ID and
 * AWS_SECRET_KEY be set to contain AWS credentials with read and write access
 * to S3.
 *
 * @author Bill Branan
 * Date: 11/1/13
 */
public class TestDuplicationPolicyManager extends BaseDuplicationPolicyTester {

    private AmazonS3 s3Client;
    private String bucketName;

    @Before
    public void setup() {
        s3Client = AmazonS3ClientBuilder.standard().build();

        // Create policy bucket
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        bucketName = accessKey.toLowerCase() + "." +
                     S3DuplicationPolicyRepo.DUP_POLICY_REPO_BUCKET_SUFFIX;
        s3Client.createBucket(bucketName);

        // Load accounts list
        s3Client.putObject(bucketName,
                           DuplicationPolicyRepo.DUP_ACCOUNTS_NAME,
                           policyAccountsFile);

        // Load policies
        String acct1PolicyName =
            "account1" + DuplicationPolicyRepo.DUP_POLICY_SUFFIX;
        s3Client.putObject(bucketName, acct1PolicyName, policyFile);
        String acct2PolicyName =
            "account2" + DuplicationPolicyRepo.DUP_POLICY_SUFFIX;
        s3Client.putObject(bucketName, acct2PolicyName, policyFile);
        String acct3PolicyName =
            "account3" + DuplicationPolicyRepo.DUP_POLICY_SUFFIX;
        s3Client.putObject(bucketName, acct3PolicyName, policyFile);
    }

    @After
    public void teardown() {
        // Clear policy bucket contents
        for (S3ObjectSummary object :
            s3Client.listObjects(bucketName).getObjectSummaries()) {
            s3Client.deleteObject(bucketName, object.getKey());
        }

        // Remove policy bucket
        s3Client.deleteBucket(bucketName);
    }

    @Test
    public void testDuplicationPolicyManager() {
        DuplicationPolicyManager policyManager =
            new DuplicationPolicyManager(new S3DuplicationPolicyRepo());

        Set<String> dupAccounts = policyManager.getDuplicationAccounts();
        assertThat(dupAccounts, hasItems("account1", "account2", "account3"));
        for (String dupAccount : dupAccounts) {
            DuplicationPolicy policy =
                policyManager.getDuplicationPolicy(dupAccount);
            assertThat(policy.getSpaces(), hasItems("testSpace1", "testSpace2"));
        }
    }

}
