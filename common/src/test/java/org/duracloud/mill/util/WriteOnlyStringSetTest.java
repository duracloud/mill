/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Daniel Bernstein
 *         Date: May 22, 2015
 */
public class WriteOnlyStringSetTest {

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
    public void test() {
        int capacity = 100;
        
        WriteOnlyStringSet set = new WriteOnlyStringSet(capacity);
        String idPrefix = StringUtils.repeat("a", 200);
        for (int i = 0; i < capacity; i++){
            set.add(idPrefix.concat(i+""));
        }
        
        assertEquals(capacity, set.size());


        for (int i = 0; i < capacity; i++){
            assertTrue(set.contains(idPrefix.concat(i+"")));
        }

        for (int i = 0; i < capacity; i++){
            set.remove(idPrefix.concat(i+""));
        }

        for (int i = 0; i < capacity; i++){
            assertFalse(set.contains(idPrefix.concat(i+"")));
        }

        assertEquals(0, set.size());
    }

    @Test
    public void testAdditionOf1MillionItemsTakesLessThan200Seconds() {
        long start = System.currentTimeMillis();
        int capacity = 1000*1000*1;
        
        WriteOnlyStringSet set = new WriteOnlyStringSet(capacity);
        String idPrefix = StringUtils.repeat("a", 200);
        for (int i = 0; i < capacity; i++){
            set.add(idPrefix.concat(i+""));
        }
        
        assertEquals(capacity, set.size());
        assertTrue(System.currentTimeMillis()-start < 20*1000);
    }

}
