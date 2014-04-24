/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

public class RunStats {
    private int deletes = 0;
    private int dups = 0;
    
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

    /**
     * @param currentTotals
     */
    public void copyValuesFrom(RunStats runstats) {
        this.deletes = runstats.deletes;
        this.dups = runstats.dups;
        
    }

    /**
     * @param stats
     */
    public void add(RunStats stats) {
        addToDeletes(stats.deletes);
        addToDups(stats.dups);
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
}