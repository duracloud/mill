/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.util.HashSet;
import java.util.Set;

import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * @author Daniel Bernstein
 *	       Date: Jan 22, 2014
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
        Morsel morsel = new Morsel("test", "space", "marker", new DuplicationStorePolicy("0", "1"));
        State state = new State();
        Assert.notNull(state.toString());
        Set<Morsel> morsels = new HashSet<>();
        morsels.add(morsel);
        state.setMorsels(morsels);
        Assert.isTrue(state.toString().contains("test"));
    }

}
