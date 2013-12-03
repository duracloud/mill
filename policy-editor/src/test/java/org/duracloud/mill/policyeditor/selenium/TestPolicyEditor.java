/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.policyeditor.selenium;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 2, 2013
 */
public class TestPolicyEditor extends BaseSeleniumTest {
    private static final String LOGIN_LOCATOR = "css=#loginButton";
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
        openAccounts();
        assertTrue(!sc.isElementPresent("css=.alert"));
        clickLoginButton();
        assertTrue(sc.isVisible("css=.alert"));
    }

    /**
     * 
     */
    private void clickLoginButton() {
        assertTrue(sc.isVisible(LOGIN_LOCATOR));
        sc.click(LOGIN_LOCATOR);
        sleep(3000);
    }
    
    @Test
    public void testAccounts() {
        openAccounts();
        doLogin();
        assertTrue(sc.isVisible("css=#new-account"));
    }

    /**
     * 
     */
    private void doLogin() {
        typeField("username");
        typeField("password");
        typeField("subdomain");
        clickLoginButton();
        sleep(3000);
    }

    /**
     * @param string
     */
    private void typeField(String field) {
        assertTrue(sc.isVisible("css=#" + field));
        sc.type("css=#" + field, this.props.get(field).toString());
    }

    /**
     * 
     */
    private void openAccounts() {
        sc.open("#/accounts");
    }

}
