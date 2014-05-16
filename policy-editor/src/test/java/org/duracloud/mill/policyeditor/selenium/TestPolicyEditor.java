/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.policyeditor.selenium;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.S3DuplicationPolicyRepo;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * @author Daniel Bernstein Date: Dec 2, 2013
 */
public class TestPolicyEditor extends BaseSeleniumTest {
    private static final String LOGIN_LOCATOR = "css=#loginButton";

    private static AmazonS3Client      s3Client;
    private static String              bucketName;
 
    /**
     * @throws java.lang.Exception
     */
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        File policyAccountsFile = new File(
                "src/test/resources/duplication-accounts.json");
        File policyFile = new File("src/test/resources/duplication-policy.json");

        assertTrue(policyAccountsFile.exists());
        assertTrue(policyFile.exists());
        s3Client = new AmazonS3Client();

        // Create policy bucket
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        bucketName = accessKey.toLowerCase()  + "." +
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
    }
    
    @AfterClass
    public static void afterClass(){
        // Clear policy bucket contents
        for(S3ObjectSummary object :
            s3Client.listObjects(bucketName).getObjectSummaries()) {
            s3Client.deleteObject(bucketName, object.getKey());
        }

        // Remove policy bucket
        s3Client.deleteBucket(bucketName);

    }
    
    @Before
    public void setUp() throws Exception {
        super.before();
    }

    /**
     * @throws java.lang.Exception
     */
    public void tearDown() throws Exception {
        super.after();
    }

    @Test
    public void testLoginFailure() {
        openAccounts();
        assertTrue(!sc.isElementPresent("css=.alert"));
        clickLoginButton();
        assertTrue(sc.isVisible("css=.alert"));
    }

    /**
     * 
     */
    private void clickLoginButton() {
        assertTrue(sc.isVisible(LOGIN_LOCATOR));
        sc.click(LOGIN_LOCATOR);
        sleep(4000);
    }

    @Test
    public void testAccounts() {
        openAccounts();
        doLogin();
        assertTrue(sc.isVisible("css=#new-account"));
    }

    /**
     * 
     */
    private void doLogin() {
        typeField("username");
        typeField("password");
        typeField("subdomain");
        clickLoginButton();
        sleep(4000);
    }

    /**
     * @param string
     */
    private void typeField(String field) {
        assertTrue(sc.isVisible("css=#" + field));
        sc.type("css=#" + field, this.props.get(field).toString());
    }

    /**
     * 
     */
    private void openAccounts() {
        sc.open("#/accounts");
    }

}
