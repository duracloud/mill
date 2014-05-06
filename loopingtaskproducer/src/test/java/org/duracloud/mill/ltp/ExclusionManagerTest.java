/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for  the ExclusionManager.
 * @author Daniel Bernstein
 *	       Date: May 5, 2014
 */
public class ExclusionManagerTest {

    private ExclusionManager exclusionManager;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        File file = new File(getClass().getResource("/exclusions.txt").getFile());
        this.exclusionManager = new ExclusionManager(file);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.duracloud.mill.ltp.ExclusionManager#isExcluded(java.lang.String)}.
     */
    @Test
    public void testIsExcluded() {
        Assert.assertTrue(this.exclusionManager.isExcluded("/account1/space2"));
        Assert.assertFalse(this.exclusionManager.isExcluded("/account2/space2"));
        Assert.assertTrue(this.exclusionManager.isExcluded("/account1/space1"));
        Assert.assertTrue(this.exclusionManager.isExcluded("/account2/space1"));
        Assert.assertTrue(this.exclusionManager.isExcluded("/account3"));
        Assert.assertTrue(this.exclusionManager.isExcluded("/account3/space3"));

    }

    @Test
    public void testIsExcludedNoExclusions() {
        this.exclusionManager = new ExclusionManager();
        Assert.assertFalse(this.exclusionManager.isExcluded("/account1/space2"));
    }
}
