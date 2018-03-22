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
 * @author Daniel Bernstein
 */
public class AccountCredentials {
    private String account;
    private List<StorageProviderCredentials> providerCredentials;

    public AccountCredentials() {

    }

    /**
     * @param account
     * @param creds
     */
    public AccountCredentials(String account, List<StorageProviderCredentials> creds) {
        this.account = account;
        this.providerCredentials = creds;
    }

    /**
     * @return the account
     */
    public String getAccount() {
        return account;
    }

    public List<StorageProviderCredentials> getProviderCredentials() {
        return providerCredentials;
    }

}
