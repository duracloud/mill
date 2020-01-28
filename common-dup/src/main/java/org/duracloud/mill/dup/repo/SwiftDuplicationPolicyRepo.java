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
     * Creates an S3 policy repo connection. Expects that S3 credentials
     * will be available from the environment, as described here:
     * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3Client
     * .html#AmazonS3Client%28%29
     */
    public SwiftDuplicationPolicyRepo(String accessKey, String secretKey, String endpoint, String region, String signer) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(OPTS.SWIFT_S3_ENDPOINT.name(), endpoint);
        map.put(OPTS.AWS_REGION.name(), region);
        map.put(OPTS.SWIFT_S3_SIGNER_TYPE.name(), signer);

        this.s3Client = S3ProviderUtil.getAmazonS3Client(accessKey, secretKey, map);
        this.policyRepoBucketSuffix = DUP_POLICY_REPO_BUCKET_SUFFIX;
        init();
    }

    public SwiftDuplicationPolicyRepo(String accessKey, String secretKey, String endpoint, String region, String signer, String policyRepoBucketSuffix) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(OPTS.SWIFT_S3_ENDPOINT.name(), endpoint);
        map.put(OPTS.AWS_REGION.name(), region);
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
