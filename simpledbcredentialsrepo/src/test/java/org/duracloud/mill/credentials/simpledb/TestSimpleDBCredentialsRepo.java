/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.simpledb;

import java.util.List;

import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.ProviderCredentials;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;

/**
 * A integration test for the SimpleDBCredentialsRepo.
 * 
 * @author Daniel Bernstein 
 *         Date: Oct 29, 2013
 */
public class TestSimpleDBCredentialsRepo {
    private AmazonSimpleDBClient client;
    private SimpleDBCredentialsRepo repo;
    private String testSubdomain = "test";

    @Before
    public void setup() {
        ConfigurationManager configurationManager = new ConfigurationManager();
        configurationManager.init();

        client = new AmazonSimpleDBClient();
        repo = new SimpleDBCredentialsRepo(client);

    }

    @Test
    public void test() throws AccountCredentialsNotFoundException {

        AccountCredentials accountCreds = repo
                .getAccoundCredentialsBySubdomain(testSubdomain);

        Assert.assertNotNull(accountCreds);

        List<ProviderCredentials> creds = accountCreds.getProviderCredentials();

        Assert.assertNotNull(creds);
        Assert.assertTrue(creds.size() > 0);
    }

    @Test
    public void testCache() throws AccountCredentialsNotFoundException {

        repo.getAccoundCredentialsBySubdomain(testSubdomain);
        
        long time = System.currentTimeMillis();
        
        for(int i = 0; i < 100; i++){
            repo.getAccoundCredentialsBySubdomain(testSubdomain);
        }
        
        Assert.assertTrue(System.currentTimeMillis()-time < 2000);
        
        
    }

    
    @Test
    public void testAccountNotFound() {
        String testSubdomain = "testx";

        try {
            repo.getAccoundCredentialsBySubdomain(testSubdomain);
            Assert.assertTrue(false);

        } catch (AccountCredentialsNotFoundException e) {
            Assert.assertTrue(true);
        }

    }

}
