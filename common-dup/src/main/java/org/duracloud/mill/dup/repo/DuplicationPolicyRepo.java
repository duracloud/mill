/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup.repo;

import java.io.InputStream;

/**
 * Provides a connection to a duplication policy repository, which is
 * essentially a storage location for the policy files used to define
 * how duplication is to be handled for DuraCloud accounts.
 *
 * @author Bill Branan
 * Date: 10/31/13
 */
public interface DuplicationPolicyRepo {

    /**
     * The expected name of the file which contains the listing of accounts
     * that need duplication actions to occur.
     */
    public static final String DUP_ACCOUNTS_NAME = "duplication-accounts.json";

    /**
     * The expected filename suffix of all files which define a duplication
     * policy for an account.
     */
    public static final String DUP_POLICY_SUFFIX = "-duplication-policy.json";

    /**
     * Provides a listing of DuraCloud accounts which require duplication.
     * Accounts are identified using their subdomain value.
     *
     * @return
     */
    public InputStream getDuplicationAccounts();

    /**
     * Provides the duplication policy for a given account
     *
     * @param account
     * @return
     */
    public InputStream getDuplicationPolicy(String account);

}
