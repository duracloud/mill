/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog;

import java.util.Iterator;

import org.duracloud.storage.domain.StorageProviderType;

/**
 * @author Daniel Bernstein Date: Apr 25, 2014
 */
public interface BitLogStore {
    /**
     * Creates a new BitLogItem and writes it to the store.
     * @param accountId
     * @param storeId
     * @param spaceId
     * @param contentId
     * @param timestamp
     * @param storeType 
     * @param result
     * @param contentCheckSum
     * @param storageProviderChecksum
     * @param auditLogCheckSum
     * @param contentIndexChecksum
     * @param String details
     * @return the newly created BitLogItem
     */
    public BitLogItem write(String accountId,
            String storeId,
            String spaceId,
            String contentId,
            long timestamp,
            StorageProviderType storeType,
            BitIntegrityResult result,
            String contentCheckSum,
            String storageProviderChecksum,
            String auditLogCheckSum,
            String contentIndexChecksum,
            String details) throws ItemWriteFailedException;

    /**
     * Returns a iterator of BitLogItems in chronological order.  If no matches are found, the 
     * iterator's hasNext() method will return false.
     * @param account
     * @param storeId
     * @param spaceId
     * @param contentId
     * @return
     */
    public Iterator<BitLogItem> getBitLogItems(String account,
            String storeId,
            String spaceId,
            String contentId);
    
    /**
     * Returns the latest BitLogItem matching the criteria. If not found a
     * NotFoundException is thrown.
     * 
     * @param account
     * @param storeId
     * @param spaceId
     * @param contentId
     * @return
     */
    public BitLogItem getLatestBitLogItem(String account,
            String storeId,
            String spaceId,
            String contentId) throws ItemNotFoundException;
    
}
