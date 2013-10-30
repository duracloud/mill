/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials;


/**
 * This interface mediates all interaction with the credential-providing subsystem.
 * @author Daniel Bernstein
 *
 */
public interface CredentialsRepo {
    /**
     * Returns a set of credentials associated with a subdomain.
     * 
     * @param subdomain The subdomain of the DuraCloud account.
     * @param storeId The storage provider id
     * @return
     * @throws CredentialsRepoException
     */
    StorageProviderCredentials getStorageProviderCredentials(String subdomain,
            String storeId) throws CredentialsRepoException;
    
}
