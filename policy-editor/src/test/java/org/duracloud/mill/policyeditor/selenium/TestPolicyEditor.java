/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.policyeditor.selenium;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.browserlaunchers.Sleeper;

import com.thoughtworks.selenium.DefaultSelenium;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 2, 2013
 */
public class TestPolicyEditor extends BaseSeleniumTest {

    /**
     * @throws java.lang.Exception
     */
    public void setUp() throws Exception {
        super.before();
    }

    /**
     * @throws java.lang.Exception
     */
    public void tearDown() throws Exception {
        super.after();
    }

    @Test
    public void testLoginFailure() {
        sc.start();
        sc.open("#/accounts");
        String loginLocator = "css=#loginButton";
        assertTrue(sc.isVisible(loginLocator));
        assertTrue(!sc.isElementPresent("css=.alert"));
        sc.click(loginLocator);
        sleep(3000);
        assertTrue(sc.isVisible("css=.alert"));
    }

}
