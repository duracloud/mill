/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import java.text.MessageFormat;

import org.duracloud.mill.ltp.RunStats;

public class DuplicationRunStats extends RunStats {
    private int deletes = 0;
    private int dups = 0;
    
    @Override
    public void reset(){
        deletes = 0;
        dups = 0;
    }
    
    /**
     * @return the dups
     */
    public int getDups() {
        return dups;
    }
    
    /**
     * @return the deletes
     */
    public int getDeletes() {
        return deletes;
    }


    @Override
    public void copyValuesFrom(RunStats runstats) {
        DuplicationRunStats dstats = (DuplicationRunStats)runstats;
        this.deletes = dstats.deletes;
        this.dups = dstats.dups;
    }

    /**
     * @param stats
     */
    @Override
    public void add(RunStats stats) {
        DuplicationRunStats dstats = (DuplicationRunStats)stats;
        addToDeletes(dstats.deletes);
        addToDups(dstats.dups);
    }

    /**
     * @param dupsToAdd
     */
    public void addToDups(int dupsToAdd) {
        this.dups += dupsToAdd;        
    }

    /**
     * @param deletesToAdd
     */
    public void addToDeletes(int deletesToAdd) {
      this.deletes += deletesToAdd;
        
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return MessageFormat.format("delete_messages={0} duplication_messages={1}", this.deletes, dups);
    }
}