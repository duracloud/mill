/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Erik Paulsson
 *         Date: 4/25/14
 */
public class IteratorsTest {

    @Test
    public void differenceTest() throws IOException {
        List<String> listA = new ArrayList<>();
        populate(listA, 1, 1000);
        List<String> listB = new ArrayList<>();
        populate(listB, 11, 1000);

        List<String> expectedDiffA = Arrays.asList(
            new String[]{"1","2","3","4","5","6","7","8","9","10"});
        Iterator<String> diffA = Iterators.difference(listA.iterator(),
                                                     listB.iterator());
        while(diffA.hasNext()) {
            assertTrue(expectedDiffA.contains(diffA.next()));
        }

        Iterator<String> diffB = Iterators.difference(listB.iterator(),
                                                      listA.iterator());
        assertFalse(diffB.hasNext());
    }

    private void populate(Collection<String> coll, int start, int end) {
        for(int i = start; i<=end; i++) {
            coll.add(Integer.toString(i));
        }
    }
}
