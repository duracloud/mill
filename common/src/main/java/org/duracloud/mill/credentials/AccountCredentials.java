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
 * 
 * @author Daniel Bernstein
 *
 */
public class AccountCredentials {
    private String subDomain;
    private List<StorageProviderCredentials> providerCredentials;

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }
    
    public String getSubdomain() {
        return subDomain;
    }

    public List<StorageProviderCredentials> getProviderCredentials() {
        return providerCredentials;
    }

    public void setSProviderCredentials(List<StorageProviderCredentials> storageProviderCredentials) {
        this.providerCredentials = storageProviderCredentials;
    }

}
