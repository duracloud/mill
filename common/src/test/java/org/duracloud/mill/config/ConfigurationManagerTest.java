/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.config;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Daniel Bernstein 
 *         Date: Oct 25, 2013
 */
public class ConfigurationManagerTest {


    /**
     * Test method for
     * {@link org.duracloud.mill.config.ConfigurationManager#getCredentialsFilePath()}
     * .
     */
    @Test
    public void test() {
        ConfigurationManager c = new ConfigurationManager();
        System.setProperty(
                ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
                "src/test/resources/mill-config-test.properties");
        c.init();
        Assert.assertNotNull(c.getCredentialsFilePath());
        Assert.assertNotNull(c.getDuplicationQueueName());

    }

}
