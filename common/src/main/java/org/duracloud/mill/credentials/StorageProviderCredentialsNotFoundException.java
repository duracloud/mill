/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials;

/**
 * @author Daniel Bernstein
 * Date: Oct 30, 2013
 */
public class StorageProviderCredentialsNotFoundException extends CredentialsRepoException {

    private static final long serialVersionUID = 1L;

    public StorageProviderCredentialsNotFoundException(String message) {
        super(message);
    }

}
