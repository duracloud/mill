/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;

/**
 * @author Bill Branan
 * Date: 11/1/13
 */
public class BaseDuplicationPolicyTester {

    public File policyAccountsFile;
    public File policyFile;

    @Before
    public void setUp() throws IOException {
        policyAccountsFile =
            new File("src/test/resources/duplication-accounts.json");
        policyFile =
            new File("src/test/resources/duplication-policy.json");

        assertTrue(policyAccountsFile.exists());
        assertTrue(policyFile.exists());
    }

}
