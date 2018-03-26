/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import java.text.MessageFormat;

import org.duracloud.mill.ltp.RunStats;

/**
 * @author Daniel Bernstein
 * Date: Apr 28, 2014
 */
public class BitIntegrityRunStats extends RunStats {

    private int added = 0;

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.RunStats#reset()
     */
    @Override
    public void reset() {
        added = 0;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.RunStats#copyValuesFrom(org.duracloud.mill.ltp.RunStats)
     */
    @Override
    public void copyValuesFrom(RunStats runstats) {
        added = ((BitIntegrityRunStats) runstats).added;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.RunStats#add(org.duracloud.mill.ltp.RunStats)
     */
    @Override
    public void add(RunStats runstats) {
        add(((BitIntegrityRunStats) runstats).added);
    }

    /**
     * @param added
     */
    public void add(int added) {
        this.added += added;
    }

    /**
     * @return the added
     */
    public int getAdded() {
        return added;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return MessageFormat.format("bit_check_messages_added={0}", added);
    }

}
