/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents the persistent state of the <code>LoopingTaskProducer</code>.
 * @author Daniel Bernstein
 *	       Date: Nov 6, 2013
 */
public class State<T> {
    
    private Set<T> morsels = new HashSet<>();
    private Date currentRunStartDate = null;
    private Date nextRunStartDate = null;

    /**
     * @return the morsels
     */
    public Set<T> getMorsels() {
        return morsels;
    }
    
    /**
     * @param morsels the morsels to set
     */
    public void setMorsels(Set<T> morsels) {
        this.morsels = morsels;
    }

    /**
     * @return
     */
    public Date getCurrentRunStartDate() {
        return this.currentRunStartDate;
    }

    /**
     * @param time
     */
    public void setCurrentRunStartDate(Date time) {
        this.currentRunStartDate = time;
    }

    /**
     * @return
     */
    public Date getNextRunStartDate() {
        return this.nextRunStartDate;
    }

    /**
     * @param time
     */
    public void setNextRunStartDate(Date time) {
        this.nextRunStartDate = time;
    }
    
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    

}
