/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.duracloud.common.util.IOUtil;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


/**
 * @author Bill Branan
 *         Date: 10/31/13
 */
public class DuplicationPolicyManagerTest extends BaseDuplicationPolicyTester {

    @Test
    public void testDupPolicyManager() {
        DuplicationPolicyRepo policyRepo =
            EasyMock.createMock(DuplicationPolicyRepo.class);

        EasyMock.expect(policyRepo.getDuplicationAccounts())
                .andReturn(IOUtil.getFileStream(policyAccountsFile));

        // Expecting dup policy to be read 3 times, requires a fresh
        // InputStream each time.
        for(int i=0; i<3; i++) {
            EasyMock.expect(policyRepo.getDuplicationPolicy(
                EasyMock.<String>anyObject()))
                    .andReturn(IOUtil.getFileStream(policyFile));
        }
        EasyMock.replay(policyRepo);

        DuplicationPolicyManager policyManager =
            new DuplicationPolicyManager(policyRepo);

        Set<String> dupAccounts =
            policyManager.getDuplicationAccounts();
        assertThat(dupAccounts, hasItems("account1", "account2", "account3"));
        for(String dupAccount : dupAccounts) {
            DuplicationPolicy policy =
                policyManager.getDuplicationPolicy(dupAccount);
            assertNotNull(policy);
        }

        EasyMock.verify(policyRepo);
    }

}
