/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup.repo;

import org.apache.commons.io.FileUtils;
import org.duracloud.common.util.IOUtil;
import org.duracloud.mill.dup.BaseDuplicationPolicyTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author Bill Branan
 *         Date: 11/1/13
 */
public class LocalDuplicationPolicyRepoTest extends BaseDuplicationPolicyTester {

    private File policyDir;

    @Before
    @Override
    public void setUp() throws IOException {
        super.setUp();

        // Create policy directory
        policyDir = new File(System.getProperty("java.io.tmpdir"),
                             "test-dup-policies");
        policyDir.mkdir();
        File acctFile = new File(policyDir, policyAccountsFile.getName());
        FileUtils.copyFile(policyAccountsFile, acctFile);

        String suffix = LocalDuplicationPolicyRepo.DUP_POLICY_SUFFIX;
        // Note the expectation here that the test accounts file includes
        // accounts named: account1, account2, and account3
        File acct1Policy = new File(policyDir, "account1" + suffix);
        File acct2Policy = new File(policyDir, "account2" + suffix);
        File acct3Policy = new File(policyDir, "account3" + suffix);

        FileUtils.copyFile(policyFile, acct1Policy);
        FileUtils.copyFile(policyFile, acct2Policy);
        FileUtils.copyFile(policyFile, acct3Policy);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(policyDir);
    }

    @Test
    public void testLocalDuplicationPolicyRepo() throws IOException {
        LocalDuplicationPolicyRepo policyRepo =
            new LocalDuplicationPolicyRepo(policyDir.getAbsolutePath());

        InputStream accountsStream = policyRepo.getDuplicationAccounts();
        String accounts = IOUtil.readStringFromStream(accountsStream);
        assertThat(accounts, containsString("account1"));

        InputStream policyStream = policyRepo.getDuplicationPolicy("account1");
        String policy = IOUtil.readStringFromStream(policyStream);
        assertThat(policy, containsString("testSpace1"));
    }

}
