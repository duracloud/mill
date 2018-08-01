/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

public abstract class RunStats {

    public abstract void reset();

    public abstract void copyValuesFrom(RunStats runstats);

    public abstract void add(RunStats stats);

}