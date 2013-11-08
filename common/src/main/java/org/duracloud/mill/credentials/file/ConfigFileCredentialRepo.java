/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.file;

import java.io.File;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.CredentialsRepoBase;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.credentials.StorageProviderCredentialsNotFoundException;

/**
 * A simple implementation of the Credential Repo based on a local configuration file.
 * 
 * @author Daniel Bernstein
 * 
 */
public class ConfigFileCredentialRepo extends CredentialsRepoBase {
    private List<AccountCredentials> accountList;

    public ConfigFileCredentialRepo(){
        this(System.getProperty(ConfigurationManager.CREDENTIALS_FILE_PATH_KEY));
    }
    
    public ConfigFileCredentialRepo(String path) {


        if (path == null) {
            throw new RuntimeException("System property "
                    + ConfigurationManager.CREDENTIALS_FILE_PATH_KEY + " not defined.");
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("File not found: "
                    + file.getAbsoluteFile());
        }

        ObjectMapper m = new ObjectMapper();
        try {
            this.accountList = m.readValue(file,
                    new TypeReference<List<AccountCredentials>>() {
                    });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    
    /* (non-Javadoc)
     * @see org.duracloud.mill.credentials.CredentialRepo#getStorageProviderCredentials(java.lang.String, java.lang.String)
     */
    @Override
    public StorageProviderCredentials getStorageProviderCredentials(
            String subdomain, String storeId)
            throws AccountCredentialsNotFoundException,
            StorageProviderCredentialsNotFoundException {

        for(AccountCredentials accountCreds : accountList){
            if(accountCreds.getSubdomain().equals(subdomain)){
                for(StorageProviderCredentials storeCred : accountCreds.getProviderCredentials()){
                    if(storeCred.getProviderId().equals(storeId)){
                        return storeCred;
                    }
                }
                
                throw new StorageProviderCredentialsNotFoundException(
                        "storeId=" + storeId + " not found for subdomain "
                                + subdomain);
            }

        }
        
        throw new AccountCredentialsNotFoundException("No account found with subdomain \""+ subdomain + "\".");
    }
}
