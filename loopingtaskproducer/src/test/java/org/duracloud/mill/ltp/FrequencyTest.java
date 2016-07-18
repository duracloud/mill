/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.text.ParseException;

import org.junit.Assert;

import org.junit.Test;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 11, 2013
 */
public class FrequencyTest {

     /**
     * Test method for {@link org.duracloud.mill.ltp.Frequency#getValue()}.
     */
    @Test
    public void testSuccess() {
        test("60s", true);
        test("30M", true);
        test("7d", true);
        test("1m",true);
        test("1x",false);
        test("0s",true);
        test("01s",false);
        
    }

    /**
     * @param string
     */
    private void test(String frequency, boolean success) {
        boolean error = false;
        try {
            new Frequency(frequency);
        }catch(ParseException ex){
            error = true;
        }
        
        if(success){
            Assert.assertTrue(!error);
        }else{
            Assert.assertTrue(error);
        }
    }

}
