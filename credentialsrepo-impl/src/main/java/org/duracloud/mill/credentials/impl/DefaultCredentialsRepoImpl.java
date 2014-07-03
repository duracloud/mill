/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.impl;

import org.duracloud.account.db.model.AccountInfo;
import org.duracloud.account.db.model.ServerDetails;
import org.duracloud.account.db.model.StorageProviderAccount;
import org.duracloud.account.db.repo.DuracloudAccountRepo;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.credentials.StorageProviderCredentialsNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A jpa-based implementation of the <code>CredentialsRepo</code>.
 * 
 * @author Daniel Bernstein Date: Oct 29, 2013
 */
@Component
public class DefaultCredentialsRepoImpl implements CredentialsRepo {
    
    private DuracloudAccountRepo accountRepo;
    /**
     * 
     * @param client
     * @param tablePrefix
     */
    @Autowired
    public DefaultCredentialsRepoImpl(DuracloudAccountRepo accountRepo) {
        this.accountRepo = accountRepo;
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
            String subdomain, String storeId)
            throws AccountCredentialsNotFoundException,
            StorageProviderCredentialsNotFoundException {
        
        AccountInfo account  = accountRepo.findBySubdomain(subdomain);
        Long id = Long.valueOf(storeId);
        if(account == null){
            throw new AccountCredentialsNotFoundException("no account found for subdomain " + subdomain);
        }else {
            ServerDetails details =  account.getServerDetails();
            if(details != null){
                StorageProviderAccount provider = details.getPrimaryStorageProviderAccount();
                if(provider.getId().equals(id)){
                    return createStorageProviderCredentials(storeId, provider);
                }else{
                    for(StorageProviderAccount sp : details.getSecondaryStorageProviderAccounts()){
                        if(sp.getId().equals(id)){
                            return createStorageProviderCredentials(storeId, sp);
                        }
                    }
                }
            }
        }
        
        throw new StorageProviderCredentialsNotFoundException(
                "No storage provider on subdomain " + subdomain
                        + " with storeId " + storeId + " found.");
    }

    private StorageProviderCredentials createStorageProviderCredentials(
            String storeId, StorageProviderAccount provider) {
        return new StorageProviderCredentials(storeId,
                provider.getUsername(), provider.getPassword(),
                provider.getProviderType());
    }


}
