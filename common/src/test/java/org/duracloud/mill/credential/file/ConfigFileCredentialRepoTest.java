/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credential.file;

import java.io.File;

import junit.framework.Assert;

import org.duracloud.mill.credential.CredentialRepo;
import org.duracloud.mill.credential.Credentials;
import org.duracloud.mill.credential.CredentialsGroup;
import org.duracloud.mill.credential.file.ConfigFileCredentialRepo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Daniel Bernstein
 *
 */
public class ConfigFileCredentialRepoTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        File testCredFile = new File("src/test/resources/test.credentials.json");
        Assert.assertTrue(testCredFile.exists());
        System.setProperty("credentials.file.path", testCredFile.getAbsolutePath());
        CredentialRepo repo = new ConfigFileCredentialRepo();
        CredentialsGroup group = repo.getCredentialGroupByAccountId("0");
        Assert.assertNotNull(group);
        Assert.assertNotNull(group.getSubdomain());
        Credentials storage = group.getCredentialsByProviderId("0");
        Assert.assertNotNull(storage);
        Assert.assertNotNull(storage.getAccessKey());
        Assert.assertNotNull(storage.getSecretKey());
        Assert.assertNotNull(storage.getProviderType());
        
    }

}
