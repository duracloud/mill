/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials;

import java.util.List;

/**
 * This interface mediates all interaction with the credential-providing subsystem.
 * @author Daniel Bernstein
 *
 */
public interface CredentialsRepo {
    /**
     * Returns a set of credentials associated with an account.
     * 
     * @param account The id of the DuraCloud account - maps directly to the subdomain.
     * @param storeId The storage provider id
     * @return
     * @throws CredentialsRepoException
     */
    StorageProviderCredentials getStorageProviderCredentials(String account,
            String storeId) throws CredentialsRepoException;
    
    /**
     * Returns a list of accounts
     * @return
     * @throws CredentialsRepoException
     */
    public List<String> getActiveAccounts() throws CredentialsRepoException;

    /**
     * Returns a list of storage provider  associated with an account
     * @param account
     * @return
     * @throws CredentialsRepoException
     */
    public AccountCredentials getAccountCredentials(String account) throws AccountCredentialsNotFoundException;
    
    /**
     * Returns true if the account is active.
     * @param account
     * @return
     * @throws AccountCredentialsNotFoundException if no account  found. 
     */
    public boolean isAccountActive(String account) throws AccountCredentialsNotFoundException;
}
