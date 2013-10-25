/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Daniel Bernstein Date: Oct 25, 2013
 */
public class ConfigurationManagerTest {


    /**
     * Test method for
     * {@link org.duracloud.mill.workman.ConfigurationManager#getCredentialsFilePath()}
     * .
     */
    @Test
    public void test() {
        ConfigurationManager c = new ConfigurationManager();
        System.setProperty(
                ConfigurationManager.DURACLOUD_WORKMAN_CONFIG_FILE_KEY,
                "src/test/resources/workman-test.properties");
        c.init();
        Assert.assertNotNull(c.getCredentialsFilePath());
        Assert.assertNotNull(c.getQueueUrl());

    }

}
