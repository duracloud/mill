/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the persistent state of the <code>LoopingTaskProducer</code>.
 * @author Daniel Bernstein
 *	       Date: Nov 6, 2013
 */
public class State {
    
    private Set<Morsel> morsels = new HashSet<>();

    /**
     * @return the morsels
     */
    public Set<Morsel> getMorsels() {
        return morsels;
    }
    
    /**
     * @param morsels the morsels to set
     */
    public void setMorsels(Set<Morsel> morsels) {
        this.morsels = morsels;
    }
    
   

}
