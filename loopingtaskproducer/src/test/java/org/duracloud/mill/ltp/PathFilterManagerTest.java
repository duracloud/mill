/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for the ExclusionManager.
 * 
 * @author Daniel Bernstein Date: May 5, 2014
 */
public class PathFilterManagerTest {

    private PathFilterManager pathFilterManager;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.pathFilterManager = new PathFilterManager();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAllExclused() {

        StringBuilder exclusions = new StringBuilder();
        exclusions.append("/*/*/*\n");

        setupPathFilterManager("", exclusions.toString());
        
        Assert.assertTrue(this.pathFilterManager.isExcluded("/account1"));
        Assert.assertTrue(this.pathFilterManager.isExcluded("/account1/store1"));
        Assert.assertTrue(this.pathFilterManager.isExcluded("/account1/store1/space1"));
    }

    @Test
    public void testCommentedOutLines() {

        StringBuilder exclusions = new StringBuilder();
        exclusions.append("#/test/*/*");
        setupPathFilterManager("", exclusions.toString());
        Assert.assertFalse(this.pathFilterManager.isExcluded("/test"));
        Assert.assertFalse(this.pathFilterManager.isExcluded("#/test"));

        exclusions = new StringBuilder();
        exclusions.append("/test/*/*");
        setupPathFilterManager("", exclusions.toString());
        Assert.assertTrue(this.pathFilterManager.isExcluded("/test"));

    }

    @Test
    public void testInclusionsNoExclusions() {
        StringBuilder inclusions = new StringBuilder();
        inclusions.append("/test/*/*");
        setupPathFilterManager(inclusions.toString(),"");
        Assert.assertFalse(this.pathFilterManager
                           .isExcluded("/test"));
        Assert.assertTrue(this.pathFilterManager.isExcluded("/test1"));
    }

    @Test
    public void testWithInclusionsAndExclusions() {
        StringBuilder inclusions = new StringBuilder();
        inclusions.append("/test/*/*");
        
        StringBuilder exclusions = new StringBuilder();
        exclusions.append("/*/*/x-duracloud-admin");

        setupPathFilterManager(inclusions.toString(),exclusions.toString());

        Assert.assertFalse(this.pathFilterManager
                           .isExcluded("/test"));
        Assert.assertFalse(this.pathFilterManager
                           .isExcluded("/test/blah"));
        Assert.assertFalse(this.pathFilterManager
                          .isExcluded("/test/blah/test-space"));

        Assert.assertTrue(this.pathFilterManager.isExcluded("/test/blah/x-duracloud-admin"));
        Assert.assertTrue(this.pathFilterManager.isExcluded("/test1/blah/x-duracloud-admin"));

    }

    /**
     * @param object
     * @param exclusions
     */
    private void setupPathFilterManager(String inclusions, String exclusions) {

        if (inclusions != null) {
            this.pathFilterManager.setInclusions(toFile(inclusions));
        }

        if (exclusions != null) {
            this.pathFilterManager.setExclusions(toFile(exclusions));
        }

    }

    /**
     * @param inclusions
     * @return
     */
    private File toFile(String contents) {
        try {
            File file = File.createTempFile("pathfiltertmp", ".txt");
            file.deleteOnExit();
            FileOutputStream fis = new FileOutputStream(file);
            fis.write(contents.getBytes());
            fis.close();
            return file;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Test
    public void testNoInclusionsNoExclusions() {
        setupPathFilterManager("", "");
        this.pathFilterManager = new PathFilterManager();
        Assert.assertFalse(this.pathFilterManager
                .isExcluded("/account1/space2"));
    }
    
    @Test
    public void testNullInclusionsAndExclusions() {
        setupPathFilterManager(null, null);
        this.pathFilterManager = new PathFilterManager();
        Assert.assertFalse(this.pathFilterManager
                .isExcluded("/account1/space2"));
    }

}
