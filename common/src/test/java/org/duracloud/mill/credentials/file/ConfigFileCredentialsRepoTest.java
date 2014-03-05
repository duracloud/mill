/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.file;

import java.io.File;

import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.credentials.StorageProviderCredentialsNotFoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Daniel Bernstein
 *
 */
public class ConfigFileCredentialsRepoTest {
    private CredentialsRepo repo;
    
    @Before
    public void setUp() throws Exception {
        File testCredFile = new File("src/test/resources/test.credentials.json");
        Assert.assertTrue(testCredFile.exists());
        System.setProperty("credentialsFilePath", testCredFile.getAbsolutePath());
        repo = new ConfigFileCredentialRepo();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception  {
        StorageProviderCredentials storage = repo.getStorageProviderCredentials("test", "0");
        Assert.assertNotNull(storage);
        Assert.assertNotNull(storage);
        Assert.assertNotNull(storage.getAccessKey());
        Assert.assertNotNull(storage.getSecretKey());
        Assert.assertNotNull(storage.getProviderType());
    }
    
    @Test 
    public void testAccountNotFound(){
        try {
            repo.getStorageProviderCredentials("nonExistentSubDomain", "0");
            Assert.assertTrue(false);
        } catch (CredentialsRepoException e) {
            Assert.assertTrue(e instanceof AccountCredentialsNotFoundException);
        }
    }

    @Test 
    public void testProviderNotFound(){
        try {
            repo.getStorageProviderCredentials("test", "xxx");
            Assert.assertTrue(false);
        } catch (CredentialsRepoException e) {
            Assert.assertTrue(e instanceof StorageProviderCredentialsNotFoundException);
        }
    }

}
