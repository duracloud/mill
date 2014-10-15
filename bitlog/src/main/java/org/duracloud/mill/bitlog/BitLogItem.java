/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog;

import java.util.Date;

import org.duracloud.storage.domain.StorageProviderType;

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
    public StorageProviderType getStoreType();
    public BitIntegrityResult getResult();
    public String getContentChecksum();
    public String getStorageProviderChecksum();
    public String getManifestChecksum();
    public String getDetails();
    public Date getModified();

}
