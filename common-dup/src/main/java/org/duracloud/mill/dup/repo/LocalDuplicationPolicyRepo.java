/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup.repo;

import java.io.File;
import java.io.InputStream;

import org.duracloud.common.util.IOUtil;

/**
 * Provides access to a duplication policy repo that is stored on the local
 * file system, as a directory containing the policy files.
 *
 * @author Bill Branan
 * Date: 10/31/13
 */
public class LocalDuplicationPolicyRepo implements DuplicationPolicyRepo {

    private File repoDir;

    public LocalDuplicationPolicyRepo(String repoDirPath) {
        this.repoDir = new File(repoDirPath);
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            throw new RuntimeException("Path " + repoDirPath + " does not " +
                                       "point to a duplication policy directory");
        }
    }

    @Override
    public InputStream getDuplicationAccounts() {
        File accountsFile = new File(repoDir, DUP_ACCOUNTS_NAME);
        return IOUtil.getFileStream(accountsFile);
    }

    @Override
    public InputStream getDuplicationPolicy(String account) {
        File policyFile = new File(repoDir, account + DUP_POLICY_SUFFIX);
        return IOUtil.getFileStream(policyFile);
    }

}
