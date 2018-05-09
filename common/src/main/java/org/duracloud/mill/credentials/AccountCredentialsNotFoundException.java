/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials;

/**
 * An exception indicating an account was not found.
 *
 * @author Daniel Bernstein
 * Date: Oct 28, 2013
 */
public class AccountCredentialsNotFoundException extends CredentialsRepoException {

    private static final long serialVersionUID = 1L;

    public AccountCredentialsNotFoundException(String message) {
        super(message);
    }
}
