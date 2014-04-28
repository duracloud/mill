/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog;

/**
 * Represents the logged info for a bit integrity check for 
 * a single item.
 * @author Daniel Bernstein
 *	       Date: Apr 25, 2014
 */
public interface BitLogItem {
    public String getAccount();
    public String getStoreId();
    public String getSpaceId();
    public String getContentId();
    public long getTimestamp();
    public String getStoreType();
    public String getResult();
    public String getContentChecksum();
    public String getStorageProviderChecksum();
    public String getAuditLogChecksum();
    public String getContentIndexChecksum();
    public String getDetails();

}
