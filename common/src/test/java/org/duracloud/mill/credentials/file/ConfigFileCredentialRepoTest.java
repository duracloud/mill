/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.file;

import java.io.File;

import junit.framework.Assert;

import org.duracloud.mill.credentials.CredentialRepo;
import org.duracloud.mill.credentials.ProviderCredentials;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.file.ConfigFileCredentialRepo;
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
        AccountCredentials group = repo.getAccoundCredentials("0");
        Assert.assertNotNull(group);
        Assert.assertNotNull(group.getSubdomain());
        ProviderCredentials storage = group.getProviderCredentials("0");
        Assert.assertNotNull(storage);
        Assert.assertNotNull(storage.getAccessKey());
        Assert.assertNotNull(storage.getSecretKey());
        Assert.assertNotNull(storage.getProviderType());
        
    }

}
