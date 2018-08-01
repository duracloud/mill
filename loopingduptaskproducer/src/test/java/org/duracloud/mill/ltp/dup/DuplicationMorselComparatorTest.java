/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.ltp.Morsel;
import org.duracloud.mill.ltp.MorselComparator;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 * Date: Nov 7, 2013
 */
public class DuplicationMorselComparatorTest {

    /**
     * Test method for
     * {@link org.duracloud.mill.ltp.MorselComparator#compare(org.duracloud.mill.ltp.Morsel, org.duracloud.mill.ltp.Morsel)}.
     */
    @Test
    public void testCompare() {
        DuplicationStorePolicy policy = new DuplicationStorePolicy();
        policy.setDestStoreId("1");
        policy.setSrcStoreId("0");

        Morsel a = new DuplicationMorsel("subdomain-a", "space-a", "marker", policy);
        Morsel b = new DuplicationMorsel("subdomain-b", "space-a", "marker", policy);

        Morsel c = new DuplicationMorsel("subdomain-z", "space-a", null, policy);

        Morsel d = new DuplicationMorsel("subdomain-a", "space-b", null, policy);
        Morsel e = new DuplicationMorsel("subdomain-b", "space-c", "marker", policy);

        MorselComparator com = new MorselComparator();
        Assert.assertTrue(com.compare(c, d) < 0);
        Assert.assertTrue(com.compare(d, a) < 0);
        Assert.assertEquals(1, com.compare(a, d));
        Assert.assertTrue(com.compare(e, b) > 0);
        Assert.assertTrue(com.compare(a, b) < 0);

    }

}
