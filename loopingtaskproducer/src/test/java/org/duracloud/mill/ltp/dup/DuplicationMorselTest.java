/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;


import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.ltp.dup.DuplicationMorsel;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *	       Date: Nov 9, 2013
 */
public class DuplicationMorselTest {

    @Test
    public void testEquals() {
        DuplicationStorePolicy policyA = new DuplicationStorePolicy();
        policyA.setDestStoreId("1");
        policyA.setSrcStoreId("0");
        
        DuplicationStorePolicy policyB = new DuplicationStorePolicy();
        policyB.setDestStoreId("1");
        policyB.setSrcStoreId("0");
        
        DuplicationMorsel a = new DuplicationMorsel("a", "b", null, policyA);
        DuplicationMorsel b = new DuplicationMorsel("a", "b", null, policyB);
        DuplicationMorsel c = new DuplicationMorsel("a", "b", "c", policyB);
        DuplicationMorsel d = new DuplicationMorsel("a", "b", "c", policyB);
        d.setDeletePerformed(true);
        
        Assert.assertEquals(a, b);
        Assert.assertEquals(a, c);
        Assert.assertEquals(a, d);

    }

}
