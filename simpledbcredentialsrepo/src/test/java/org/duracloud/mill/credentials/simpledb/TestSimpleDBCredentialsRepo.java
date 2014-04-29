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
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.credentials.StorageProviderCredentialsNotFoundException;
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
    private String testSubdomain;
    private String testStoreId;
   
    @Before
    public void setup() {

        ConfigurationManager configurationManager = new ConfigurationManager();
        configurationManager.init();

        testSubdomain = System.getProperty("testSubdomain", "test");
        if(testSubdomain == null){
            throw new RuntimeException("testSubdomain parameter is null: add to configuration file or use jvm arg -DtestSubdomain={subdomain}");
        }
        testStoreId =  System.getProperty("testStoreId", "0");
        if(testStoreId == null){
            throw new RuntimeException("testStoreId parameter is null: add to configuration file or use jvm arg -DtestStoreId={storeId}");
        }
 
        client = new AmazonSimpleDBClient();
        repo = new SimpleDBCredentialsRepo(client, "TEST_");

    }

    @Test
    public void test() throws AccountCredentialsNotFoundException,StorageProviderCredentialsNotFoundException {

        StorageProviderCredentials accountCreds = repo
                .getStorageProviderCredentials(testSubdomain, testStoreId);

        Assert.assertNotNull(accountCreds);

    }

    @Test
    public void testCache() throws AccountCredentialsNotFoundException, StorageProviderCredentialsNotFoundException {

        repo.getStorageProviderCredentials(testSubdomain, testStoreId);
        
        long time = System.currentTimeMillis();
        
        for(int i = 0; i < 100; i++){
            repo.getStorageProviderCredentials(testSubdomain, testStoreId);
        }
        
        Assert.assertTrue(System.currentTimeMillis()-time < 2000);
    }

    
    @Test
    public void testAccountNotFound() {
        String testSubdomain = "testx";

        try {
            repo.getStorageProviderCredentials(testSubdomain, testStoreId);
            Assert.assertTrue(false);

        } catch (AccountCredentialsNotFoundException e) {
            Assert.assertTrue(true);
        } catch (StorageProviderCredentialsNotFoundException e) {
            Assert.assertTrue(false);
        }
    }
    
    @Test
    public void testGetAccounts() throws CredentialsRepoException{
        List<String> accounts = repo.getAccounts();
        
        Assert.assertNotNull(accounts);

        Assert.assertTrue(accounts.size() > 0);
        
    }
    
    
    @Test
    public void testGetAccount() throws AccountCredentialsNotFoundException {
        AccountCredentials accountCreds = repo.getAccountCredentials(testSubdomain);

        Assert.assertNotNull(accountCreds);
        Assert.assertNotNull(accountCreds.getProviderCredentials());
        Assert.assertTrue(accountCreds.getProviderCredentials().size() > 0);
        

    }

}
