/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup.repo;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

import java.io.InputStream;
import java.util.List;

/**
 * Provides access to a duplication policy repo that is stored in S3, as a
 * bucket which contains the policy files.
 *
 * @author Bill Branan
 *         Date: 10/31/13
 */
public class S3DuplicationPolicyRepo implements DuplicationPolicyRepo {

    public static final String DUP_POLICY_REPO_BUCKET_SUFFIX =
        "duplication-policy-repo";

    private AmazonS3Client s3Client;
    private String policyRepoBucketName;
    private String policyRepoBucketSuffix;

    /**
     * Creates an S3 policy repo connection. Expects that S3 credentials
     * will be available from the environment, as described here:
     * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3Client.html#AmazonS3Client%28%29
     */
    public S3DuplicationPolicyRepo() {
        this.s3Client = new AmazonS3Client();
        this.policyRepoBucketSuffix = DUP_POLICY_REPO_BUCKET_SUFFIX;
        init();
    }

    /**
     * Creates an S3 policy repo connection. Uses the provided bucket suffix.
     * Expects that S3 credentials will be available from the environment,
     * as described here:
     * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3Client.html#AmazonS3Client%28%29
     */
    public S3DuplicationPolicyRepo(String policyRepoBucketSuffix) {
        this.s3Client = new AmazonS3Client();
        this.policyRepoBucketSuffix = policyRepoBucketSuffix;
        init();
    }

    /**
     * Intended for testing
     */
    protected S3DuplicationPolicyRepo(AmazonS3Client s3Client) {
        this.s3Client = s3Client;
        this.policyRepoBucketSuffix = DUP_POLICY_REPO_BUCKET_SUFFIX;
        init();
    }

    private void init() {
        List<Bucket> buckets = s3Client.listBuckets();
        for(Bucket bucket : buckets) {
            String bucketName = bucket.getName();
            if(bucketName.endsWith(policyRepoBucketSuffix)) {
                policyRepoBucketName = bucketName;
            }
        }

        if(null == policyRepoBucketName) {
            throw new RuntimeException("Unable to find duplication policy " +
                                       "repo bucket in S3. Bucket suffix: " +
                                       policyRepoBucketSuffix);
        }
    }

    @Override
    public InputStream getDuplicationAccounts() {
        return s3Client.getObject(policyRepoBucketName, DUP_ACCOUNTS_NAME)
                       .getObjectContent();
    }

    @Override
    public InputStream getDuplicationPolicy(String account) {
        return s3Client.getObject(policyRepoBucketName,
                                  account + DUP_POLICY_SUFFIX)
                       .getObjectContent();
    }

}
