/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.util.PriorityQueue;

/**
 * A queue for <code>Morsels</code>
 * @author Daniel Bernstein
 *	       Date: Nov 7, 2013
 */
public class MorselQueue extends PriorityQueue<Morsel> {
       public MorselQueue(){
        super(100000, new MorselComparator()); // there should never be anywhere
                                               // near this number of morsels in
                                               // the queue;
       }
}
