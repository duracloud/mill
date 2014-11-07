/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog;

import java.util.Date;
import java.util.Iterator;

import org.duracloud.mill.db.model.BitIntegrityReport;
import org.duracloud.reportdata.bitintegrity.BitIntegrityReportResult;
import org.duracloud.storage.domain.StorageProviderType;

/**
 * @author Daniel Bernstein 
 *         Date: Apr 25, 2014
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
     * @param manifestChecksum
     * @param String details
     * @return the newly created BitLogItem
     */
    public BitLogItem write(String accountId,
            String storeId,
            String spaceId,
            String contentId,
            Date timestamp,
            StorageProviderType storeType,
            BitIntegrityResult result,
            String contentCheckSum,
            String storageProviderChecksum,
            String manifestChecksum,
            String details) throws ItemWriteFailedException;

    /**
     * Returns a iterator of BitLogItems in chronological order.  If no matches are found, the 
     * iterator's hasNext() method will return false.
     * @param account
     * @param storeId
     * @param spaceId
     * @return
     */
    public Iterator<BitLogItem> getBitLogItems(String account,
            String storeId,
            String spaceId);

    /**
     * @param account
     * @param storeId
     * @param spaceId
     */
    public void delete(String account, String storeId, String spaceId);

    /**
     * 
     * @param account
     * @param storeId
     * @param spaceId
     * @param reportSpaceId
     * @param reportContentId
     * @param result
     * @param completionDate
     * @return
     */
    public BitIntegrityReport addReport(String account,
                          String storeId,
                          String spaceId,
                          String reportSpaceId,
                          String reportContentId,
                          BitIntegrityReportResult result,
                          Date completionDate);

}
