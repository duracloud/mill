/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;

/**
 * @author Bill Branan
 * Date: 10/30/13
 */
public class DuplicationPolicyManager {

    private Map<String, DuplicationPolicy> dupAccounts = null;

    private DuplicationPolicyRepo policyRepo;

    public DuplicationPolicyManager(DuplicationPolicyRepo policyRepo) {
        this.policyRepo = policyRepo;
    }

    private void refreshPolicies() {
        dupAccounts = new HashMap<>();

        // Load policies
        try {
            List<String> dupAccountList =
                readDupAccounts(policyRepo.getDuplicationAccounts());
            for (String dupAccount : dupAccountList) {
                DuplicationPolicy policy =
                    readDupPolicy(policyRepo.getDuplicationPolicy(dupAccount));
                dupAccounts.put(dupAccount, policy);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load duplication policies " +
                                       "due to: " + e.getMessage(), e);
        }
    }

    /**
     * Reads duplication account listing. File is expected to be a JSON
     * formatted list of String values.
     *
     * @param dupAccountsStream stream
     * @return list of duplication accounts
     * @throws IOException
     */
    private List<String> readDupAccounts(InputStream dupAccountsStream)
        throws IOException {
        ObjectMapper objMapper = new ObjectMapper();
        return objMapper.readValue(
            dupAccountsStream,
            new TypeReference<List<String>>() {
            });
    }

    private DuplicationPolicy readDupPolicy(InputStream dupPolicyStream)
        throws IOException {
        return DuplicationPolicy.unmarshall(dupPolicyStream);
    }

    /**
     * Provides a listing of DuraCloud accounts which require duplication.
     * Accounts are identified using their subdomain value.
     *
     * @return
     */
    public Set<String> getDuplicationAccounts() {
        ensurePoliciesLoaded();
        return new HashSet<>(dupAccounts.keySet());
    }

    /**
     *
     */
    private synchronized void ensurePoliciesLoaded() {
        if (dupAccounts == null) {
            refreshPolicies();
        }
    }

    /**
     * Provides the duplication policy for a given account
     *
     * @param account
     * @return
     */
    public DuplicationPolicy getDuplicationPolicy(String account) {
        ensurePoliciesLoaded();
        return dupAccounts.get(account);
    }

    /**
     * Causes the cached duplication policy set to be cleared.
     */
    public void clearPolicyCache() {
        dupAccounts = null;
    }

}
