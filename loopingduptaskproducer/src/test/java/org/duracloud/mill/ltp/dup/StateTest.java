/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import java.util.LinkedHashSet;

import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.ltp.Morsel;
import org.duracloud.mill.ltp.State;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 * Date: Jan 22, 2014
 */
public class StateTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testToString() {
        DuplicationMorsel morsel =
            new DuplicationMorsel("test", "space", "marker", new DuplicationStorePolicy("0", "1"));
        State<Morsel> state = new State<>();
        Assert.assertNotNull(state.toString());
        LinkedHashSet<Morsel> morsels = new LinkedHashSet<>();
        morsels.add(morsel);
        state.setMorsels(morsels);
        Assert.assertTrue(state.toString().contains("test"));
    }

}
