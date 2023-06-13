/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.io.File;
import java.util.Comparator;

/**
 * Most recently  modified files are returned first.
 *
 * @author Daniel Bernstein
 * Date: Sep 8, 2014
 */
public class LastModifiedDateFileComparator implements Comparator<File> {
    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(File o1, File o2) {
        return -1 * Long.compare(o1.lastModified(), o2.lastModified());
    }
}
