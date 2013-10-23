/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credential;

import org.duracloud.storage.domain.StorageProviderType;

/**
 * 
 * @author Daniel Bernstein
 *
 */
public class Credentials {
    private String providerId;
    private String accessKey;
    private String secretKey;
    private StorageProviderType providerType;
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public StorageProviderType getProviderType() {
        return providerType;
    }
    
    public void setProviderType(StorageProviderType providerType) {
        this.providerType = providerType;
    }
}
