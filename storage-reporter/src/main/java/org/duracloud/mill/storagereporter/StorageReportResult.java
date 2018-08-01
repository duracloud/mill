/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagereporter;

import java.util.List;

/**
 * @author danny
 * Date: Jun 29, 2017
 */
public class StorageReportResult {

    private List<AccountStorageReportResult> oversubscribedAccounts;
    private List<AccountStorageReportResult> undersubscribedAccounts;

    /**
     * @param oversubscribedAccounts
     * @param undersubscribedAccounts
     */
    public StorageReportResult(List<AccountStorageReportResult> oversubscribedAccounts,
                               List<AccountStorageReportResult> undersubscribedAccounts) {
        this.oversubscribedAccounts = oversubscribedAccounts;
        this.undersubscribedAccounts = undersubscribedAccounts;
    }

    /**
     * @return the oversubscribedAccounts
     */
    public List<AccountStorageReportResult> getOversubscribedAccounts() {
        return oversubscribedAccounts;
    }

    /**
     * @return the undersubscribedAccounts
     */
    public List<AccountStorageReportResult> getUndersubscribedAccounts() {
        return undersubscribedAccounts;
    }

}
