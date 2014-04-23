/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.duracloud.mill.dup.DuplicationStorePolicy;

/**
 * A definition of a bite-sized swath of content ids that can be nibbled by the
 * <code>LoopingTaskProducer</code>.
 * 
 * @author Daniel Bernstein 
 *         Date: Nov 7, 2013
 */
public class Morsel {
    protected static final String[] EXCLUSIONS = {"marker", "deletePerformed"};
    private String subdomain;
    private String spaceId;
    private String marker;
    private DuplicationStorePolicy storePolicy;
    private boolean deletePerformed = false;
    
    public Morsel() {
        
    }
    
    /**
     * @param subdomain
     * @param spaceId
     * @param marker
     * @param storePolicy
     * @param inprocess
     */
    public Morsel(String subdomain, String spaceId, String marker,
            DuplicationStorePolicy storePolicy) {
        super();
        this.subdomain = subdomain;
        this.spaceId = spaceId;
        this.marker = marker;
        this.storePolicy = storePolicy;
    }


    /**
     * @return the subdomain
     */
    public String getSubdomain() {
        return subdomain;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, getExclusions());
    }

    /**
     * @return
     */
    private String[] getExclusions() {
        return EXCLUSIONS;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, getExclusions());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    /**
     * @return the spaceId
     */
    public String getSpaceId() {
        return spaceId;
    }


    /**
     * @return the marker
     */
    public String getMarker() {
        return marker;
    }

    /**
     * @param marker the marker to set
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }

    /**
     * @return the storePolicy
     */
    public DuplicationStorePolicy getStorePolicy() {
        return storePolicy;
    }

    /**
     * @return the deletePerformed
     */
    public boolean isDeletePerformed() {
        return deletePerformed;
    }

    /**
     * @param deletePerformed the deletePerformed to set
     */
    public void setDeletePerformed(boolean deletePerformed) {
        this.deletePerformed = deletePerformed;
    }
}
