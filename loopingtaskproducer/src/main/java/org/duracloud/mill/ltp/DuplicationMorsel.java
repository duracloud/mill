/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import org.duracloud.mill.dup.DuplicationStorePolicy;

/**
 * A definition of a bite-sized swath of content ids that can be nibbled by the
 * <code>LoopingTaskProducer</code>.
 * 
 * @author Daniel Bernstein 
 *         Date: Nov 7, 2013
 */
public class DuplicationMorsel extends Morsel{
    
    public DuplicationMorsel() {
        
    }
    
    /**
     * @param subdomain
     * @param spaceId
     * @param marker
     * @param storePolicy
     * @param inprocess
     */
    public DuplicationMorsel(String subdomain, String spaceId, String marker,
            DuplicationStorePolicy storePolicy) {
        super(subdomain,spaceId, marker, storePolicy);
    }

}
