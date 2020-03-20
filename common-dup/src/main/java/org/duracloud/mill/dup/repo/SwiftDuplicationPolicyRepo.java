/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup.repo;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import org.duracloud.s3storage.S3ProviderUtil;
import org.duracloud.storage.domain.StorageAccount.OPTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwiftDuplicationPolicyRepo implements DuplicationPolicyRepo {
    public static final String DUP_POLICY_REPO_BUCKET_SUFFIX =
            "duplication-policy-repo";
    private static final Logger log = LoggerFactory.getLogger(SwiftDuplicationPolicyRepo.class);

    private AmazonS3 s3Client;
    private String policyRepoBucketName;
    private String policyRepoBucketSuffix;

    /**
     * Creates a Swift duplication policy repo connection.
     * Connection details must be configured in .properties file.
     */
    public SwiftDuplicationPolicyRepo(String accessKey, String secretKey, String endpoint, String signer) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(OPTS.SWIFT_S3_ENDPOINT.name(), endpoint);
        map.put(OPTS.SWIFT_S3_SIGNER_TYPE.name(), signer);

        this.s3Client = S3ProviderUtil.getAmazonS3Client(accessKey, secretKey, map);
        this.policyRepoBucketSuffix = DUP_POLICY_REPO_BUCKET_SUFFIX;
        init();
    }

    public SwiftDuplicationPolicyRepo(String accessKey,
                                      String secretKey,
                                      String endpoint,
                                      String signer,
                                      String policyRepoBucketSuffix) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(OPTS.SWIFT_S3_ENDPOINT.name(), endpoint);
        map.put(OPTS.SWIFT_S3_SIGNER_TYPE.name(), signer);

        this.s3Client = S3ProviderUtil.getAmazonS3Client(accessKey, secretKey, map);
        this.policyRepoBucketSuffix = policyRepoBucketSuffix;
        init();
    }

    public void init() {
        List<Bucket> buckets = s3Client.listBuckets();
        for (Bucket bucket : buckets) {
            String bucketName = bucket.getName();
            if (bucketName.endsWith(policyRepoBucketSuffix)) {
                policyRepoBucketName = bucketName;
            }
        }

        if (null == policyRepoBucketName) {
            throw new RuntimeException("Unable to find duplication policy " +
                    "repo bucket in Swift. Bucket suffix: " +
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
