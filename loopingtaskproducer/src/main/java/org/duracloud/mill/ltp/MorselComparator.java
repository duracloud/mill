/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.util.Comparator;

/**
 * The prioritization strategy for the <code>MorselQueue</code>. The intention
 * is to ensure that spaces that have been started get worked before spaces that
 * haven't. Also, ideally morsels from different accounts are evenly disperses
 * throughout the queue. So, given those motivations, here's an initial stab at
 * the logic.
 * 
 * 1. Morsels with non-null markers should go first. 
 * 2. Otherwise, order by space followed by domain.
 * 
 * @author Daniel Bernstein 
 *         Date: Nov 7, 2013
 */
public class MorselComparator implements Comparator<Morsel> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Morsel o1, Morsel o2) {
        if(o1.getMarker() != null && o2.getMarker() == null){
            return 1;
        }
        
        if(o2.getMarker() != null && o1.getMarker() == null){
            return -1;
        }
        
        int spaceCompare = o1.getSpaceId().compareTo(o2.getSpaceId());
        if(spaceCompare == 0){
            return o1.getSubdomain().compareTo(o2.getSubdomain());
        }else{
            return spaceCompare;
        }
    }
}
