/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Daniel Bernstein
 * Date: Sep 5, 2014
 */
public class LogKey {
    private String accountId;
    private String storeId;
    private String spaceId;

    /**
     * @param accountId
     * @param storeId
     * @param spaceId
     */
    public LogKey(String accountId, String storeId, String spaceId) {
        super();
        this.accountId = accountId;
        this.storeId = storeId;
        this.spaceId = spaceId;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * @return
     */
    public String getSpaceId() {
        return this.spaceId;
    }

    /**
     * @return
     */
    public String getStoreId() {
        return this.storeId;
    }

    /**
     * @return
     */
    public String getAccountId() {
        return this.accountId;
    }
}
