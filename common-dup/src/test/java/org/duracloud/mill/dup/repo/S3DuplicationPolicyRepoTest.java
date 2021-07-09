/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup.repo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import org.duracloud.common.util.IOUtil;
import org.easymock.EasyMock;
import org.junit.Test;

/**
 * @author Bill Branan
 * Date: 11/1/13
 */
public class S3DuplicationPolicyRepoTest {

    @Test
    public void testS3DuplicationPolicyRepo() throws IOException {
        AmazonS3 s3Client = EasyMock.createMock(AmazonS3.class);

        String policyBucketName = "my-personal-" +
                                  S3DuplicationPolicyRepo.DUP_POLICY_REPO_BUCKET_SUFFIX;

        // Expect getBuckets call
        List<Bucket> buckets = new ArrayList<>();
        buckets.add(new Bucket("other-stuff"));
        buckets.add(new Bucket(policyBucketName));
        EasyMock.expect(s3Client.listBuckets()).andReturn(buckets);

        // Expect getObject for accounts list
        String accountsContent = "accounts-content";
        S3Object accountsObject = new S3Object();
        accountsObject.setObjectContent(
            IOUtil.writeStringToStream(accountsContent));
        String accountsFileName = S3DuplicationPolicyRepo.DUP_ACCOUNTS_NAME;
        EasyMock.expect(s3Client.getObject(policyBucketName, accountsFileName))
                .andReturn(accountsObject);

        // Expect getObject for policy
        String accountName = "my-account";
        String policyContent = "policy-content";
        S3Object policyObject = new S3Object();
        policyObject.setObjectContent(
            IOUtil.writeStringToStream(policyContent));
        String policyFileName =
            accountName + S3DuplicationPolicyRepo.DUP_POLICY_SUFFIX;
        EasyMock.expect(s3Client.getObject(policyBucketName, policyFileName))
                .andReturn(policyObject);

        EasyMock.replay(s3Client);

        S3DuplicationPolicyRepo policyRepo =
            new S3DuplicationPolicyRepo(s3Client);

        InputStream accountsStream = policyRepo.getDuplicationAccounts();
        String accounts = IOUtil.readStringFromStream(accountsStream);
        assertThat(accounts, is(accountsContent));

        InputStream policyStream = policyRepo.getDuplicationPolicy(accountName);
        String policy = IOUtil.readStringFromStream(policyStream);
        assertThat(policy, is(policyContent));

        EasyMock.verify(s3Client);
    }

}
