/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credential;

import java.util.List;

/**
 * 
 * @author Daniel Bernstein
 *
 */
public class CredentialsGroup {
    private String subDomain;
    private List<Credentials> providerCredentials;

    
    public Credentials getCredentialsByProviderId(String id){
        for(Credentials c : providerCredentials){
            if(id.equals(c.getProviderId())){
                return c;
            }
        }
        
        throw new RuntimeException("provider with " + id + " not found.");
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }
    
    public String getSubdomain() {
        return subDomain;
    }

    public List<Credentials> getProviderCredentials() {
        return providerCredentials;
    }

    public void setSProviderCredentials(List<Credentials> storageProviderCredentials) {
        this.providerCredentials = storageProviderCredentials;
    }

}
