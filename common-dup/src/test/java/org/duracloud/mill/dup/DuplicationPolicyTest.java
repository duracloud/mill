/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.junit.Test;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Erik Paulsson
 *         Date: 10/29/13
 */
public class DuplicationPolicyTest extends BaseDuplicationPolicyTester {

    //@Test
    public void testDuplicationPolicyMarshall() throws Exception {
        DuplicationStorePolicy storePolicy12 = new DuplicationStorePolicy();
        storePolicy12.setSrcStoreId("1");
        storePolicy12.setDestStoreId("2");
        DuplicationStorePolicy storePolicy13 = new DuplicationStorePolicy();
        storePolicy13.setSrcStoreId("1");
        storePolicy13.setDestStoreId("3");

        DuplicationPolicy duplicationPolicy = new DuplicationPolicy();
        duplicationPolicy.addDuplicationStorePolicy("testSpace1", storePolicy12);
        duplicationPolicy.addDuplicationStorePolicy("testSpace1", storePolicy13);
        duplicationPolicy.addDuplicationStorePolicy("testSpace2", storePolicy12);

        String json = DuplicationPolicy.marshall(duplicationPolicy);
        assertNotNull(json);
        System.out.println("#######\n" + json + "\n######");
    }

    @Test
    public void testDuplicationPolicyUnmarshall() throws Exception {
        DuplicationPolicy duplicationPolicy =
            DuplicationPolicy.unmarshall(new FileInputStream(policyFile));
        assertThat(2, is(equalTo(duplicationPolicy.getSpaces().size())));
        assertTrue(duplicationPolicy.getSpaces().contains("testSpace1"));
        assertTrue(duplicationPolicy.getSpaces().contains("testSpace2"));

        Set<DuplicationStorePolicy> space1DuplicationStorePolicies =
            duplicationPolicy.getDuplicationStorePolicies("testSpace1");
        Set<DuplicationStorePolicy> space2DuplicationStorePolicies =
            duplicationPolicy.getDuplicationStorePolicies("testSpace2");

        assertThat(2, is(equalTo(space1DuplicationStorePolicies.size())));
        assertThat(1, is(equalTo(space2DuplicationStorePolicies.size())));

        Iterator<DuplicationStorePolicy> space1Iter = space1DuplicationStorePolicies.iterator();
        DuplicationStorePolicy duplicationStorePolicy = space1Iter.next();
        assertThat("1", is(equalTo(duplicationStorePolicy.getSrcStoreId())));
        assertThat("2", is(equalTo(duplicationStorePolicy.getDestStoreId())));
        duplicationStorePolicy = space1Iter.next();
        assertThat("1", is(equalTo(duplicationStorePolicy.getSrcStoreId())));
        assertThat("3", is(equalTo(duplicationStorePolicy.getDestStoreId())));
        assertFalse(space1Iter.hasNext());

        Iterator<DuplicationStorePolicy> space2Iter = space2DuplicationStorePolicies.iterator();
        duplicationStorePolicy = space2Iter.next();
        assertThat("1", is(equalTo(duplicationStorePolicy.getSrcStoreId())));
        assertThat("2", is(equalTo(duplicationStorePolicy.getDestStoreId())));
        assertFalse(space2Iter.hasNext());
    }
}
