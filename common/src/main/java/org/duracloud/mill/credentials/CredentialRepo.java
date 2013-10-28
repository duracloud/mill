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
public interface CredentialRepo {
    /**
     * Returns a set of credentials associated with a subdomain.
     * 
     * @param accountId
     * @return
     * @throws AccountCredentialsNotFoundException
     */
    AccountCredentials getAccoundCredentialsBySubdomain(String accountId)
            throws AccountCredentialsNotFoundException;
    
}
