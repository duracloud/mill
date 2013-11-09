/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;


import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *	       Date: Nov 9, 2013
 */
public class MorselTest {

    @Test
    public void testEquals() {
        DuplicationStorePolicy policyA = new DuplicationStorePolicy();
        policyA.setDestStoreId("1");
        policyA.setSrcStoreId("0");
        
        DuplicationStorePolicy policyB = new DuplicationStorePolicy();
        policyB.setDestStoreId("1");
        policyB.setSrcStoreId("0");
        
        Morsel a = new Morsel("a", "b", null, policyA);
        Morsel b = new Morsel("a", "b", null, policyB);
        Morsel c = new Morsel("a", "b", "c", policyB);
        Morsel d = new Morsel("a", "b", "c", policyB);
        d.setDeletePerformed(true);
        
        Assert.assertEquals(a, b);
        Assert.assertEquals(a, c);
        Assert.assertEquals(a, d);

    }

}
