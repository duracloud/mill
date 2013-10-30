/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Bill Branan
 *         Date: 10/30/13
 */
public class DuplicationPolicyManager {

    private Map<String, DuplicationPolicy> dupAccounts;

    public DuplicationPolicyManager() {
         dupAccounts = new HashMap<>();

         // TODO: Retrieve account duplication information from bucket
    }

    /**
     * Provides a listing of DuraCloud accounts which require duplication.
     * Accounts are identified using their subdomain value.
     *
     * @return
     */
    public Set<String> getDuplicationAccounts() {
        return dupAccounts.keySet();
    }

    /**
     * Provides the duplication policy for a given account
     *
     * @param account
     * @return
     */
    public DuplicationPolicy getDuplicationPolicy(String account) {
        return dupAccounts.get(account);
    }

}
