/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.simpledb;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.credentials.StorageProviderCredentialsNotFoundException;
import org.duracloud.storage.domain.StorageProviderType;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;

/**
 * A simpledb-based implementation of the <code>CredentialsRepo</code>. Once it
 * has been read from simpledb all account information is cached indefinitely.
 * 
 * @author Daniel Bernstein Date: Oct 29, 2013
 */
public class SimpleDBCredentialsRepo implements CredentialsRepo {
    private AmazonSimpleDBClient client;
    private Map<String, AccountCredentials> cache = new HashMap<>();
    private String tablePrefix;
    
    /**
     * 
     * @param client
     * @param tablePrefix
     */
    public SimpleDBCredentialsRepo(AmazonSimpleDBClient client, String tablePrefix) {
        this.client = client;
        this.tablePrefix = tablePrefix;
    }

    /**
     * @param client
     */
    public SimpleDBCredentialsRepo(AmazonSimpleDBClient client) {
        this(client, "");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.credentials.CredentialRepo#getStorageProviderCredentials
     * (java.lang.String, java.lang.String)
     */
    @Override
    public StorageProviderCredentials getStorageProviderCredentials(
            String account, String storeId)
            throws AccountCredentialsNotFoundException,
            StorageProviderCredentialsNotFoundException {
        
        AccountCredentials accountCredentials = getAccountCredentials(account);

        StorageProviderCredentials storeCred = getStorageProvider(storeId, accountCredentials); 
        if(storeCred != null){
            return storeCred;
        }else{
            throw new StorageProviderCredentialsNotFoundException(
                    "no provider where id = " + storeId + " on account "
                            + account);
        }
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.credentials.CredentialsRepo#getAccountCredentials(java.lang.String)
     */
    @Override
    
    public AccountCredentials getAccountCredentials(String subdomain)
            throws AccountCredentialsNotFoundException {
        
        AccountCredentials accountCredentials = cache.get(subdomain);
        
        if(null != accountCredentials){
            return accountCredentials;
        }
        
        // get server details id
        SelectResult result = this.client.select(new SelectRequest(
                "select SERVER_DETAILS_ID from " + tablePrefix + "DURACLOUD_ACCOUNTS where SUBDOMAIN = '"
                        + subdomain + "'"));
        List<Item> items = result.getItems();

        if (items.size() == 0) {
            throw new AccountCredentialsNotFoundException("subdomain \""
                    + subdomain + "\" not found.");
        }

        String serverDetailsId = items.get(0).getAttributes().get(0).getValue();

        // get server details
        String serverDetailsDomain = tablePrefix + "DURACLOUD_SERVER_DETAILS";
        GetAttributesResult getResult = this.client
                .getAttributes(new GetAttributesRequest(serverDetailsDomain,
                        serverDetailsId));
        List<StorageProviderCredentials> creds = new LinkedList<StorageProviderCredentials>();

        // get primary providers
        String primaryId = getValue(getResult,
                "PRIMARY_STORAGE_PROVIDER_ACCOUNT_ID", serverDetailsDomain);
        creds.add(getProviderCredentials(primaryId));
        // parse secondary ids
        String secondaryIdsStr = getValue(getResult,
                "SECONDARY_STORAGE_PROVIDER_ACCOUNT_IDS", serverDetailsDomain);

        if(!StringUtils.isBlank(secondaryIdsStr)){
            String[] secondaryIds = secondaryIdsStr.trim().split(",");
            // for each secondary get providers
            for (String secondaryId : secondaryIds) {
                creds.add(getProviderCredentials(secondaryId));
            }
        }

        // build account credentials object.
        accountCredentials = new AccountCredentials(subdomain, creds);
        this.cache.put(subdomain, accountCredentials);
        return accountCredentials;
    }

    private StorageProviderCredentials getStorageProvider(String storeId,
            AccountCredentials accountCredentials) {
        for(StorageProviderCredentials storeCred : accountCredentials.getProviderCredentials()){
            if(storeCred.getProviderId().equals(storeId)){
                return storeCred;
            }
        }
        
        return null;
    }

    /**
     * @param primaryId
     * @return
     */
    private StorageProviderCredentials getProviderCredentials(String id) {
        String domain = tablePrefix + "DURACLOUD_STORAGE_PROVIDER_ACCOUNTS";
        GetAttributesResult result = this.client
                .getAttributes(new GetAttributesRequest(domain, id));
        String username = getValue(result, "USERNAME", domain);
        String password = getValue(result, "PASSWORD", domain);
        StorageProviderType providerType = StorageProviderType
                .valueOf(getValue(result, "PROVIDER_TYPE", domain));
        return new StorageProviderCredentials(id, username, password,
                providerType);
    }

    private String getValue(GetAttributesResult getResult,
            String attributeName, String domain) {
        List<Attribute> attrs = getResult.getAttributes();
        for (Attribute attr : attrs) {
            if (attr.getName().equals(attributeName)) {
                return attr.getValue();
            }
        }

        throw new RuntimeException("Not attribute named " + attributeName
                + " exists in result: domain=" + domain);
    }
    
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.credentials.CredentialsRepo#getAccounts()
     */
    @Override
    public List<String> getAccounts() throws CredentialsRepoException {
        List<String> accounts = new LinkedList<String>();
        // get server details id
        SelectResult result = this.client.select(new SelectRequest(
                "select SUBDOMAIN from " + tablePrefix + "DURACLOUD_ACCOUNTS limit 2500"));
        List<Item> items = result.getItems();

        for(Item item : items ){
            String accountId = item.getAttributes().get(0).getValue();
            accounts.add(accountId);
        }

        return accounts;
    }
}
