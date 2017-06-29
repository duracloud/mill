/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagereporter;

import java.util.LinkedList;
import java.util.List;

import org.duracloud.account.db.model.AccountInfo;
import org.duracloud.account.db.model.StorageProviderAccount;

/**
 * @author dbernstein 
 * @since: Jun 28, 2017
 */
public class AccountStorageReportResult {

    private List<StorageProviderResult> storageProviderResults = new LinkedList<>();
    private AccountInfo account;
    /**
     * @param account
     */
    public AccountStorageReportResult(AccountInfo account) {
        this.account = account;
    }

    /**
     * @param storageProviderAccount
     * @param total
     */
    public void addStorageProviderResult(
                                         StorageProviderAccount storageProviderAccount,
                                         long total) {
        this.storageProviderResults.add(new StorageProviderResult(storageProviderAccount, total));
    }

    
    /**
     * @return
     */
    public boolean isOversubscribed() {
        for(StorageProviderResult r : this.storageProviderResults){
            if(r.isOversubscribed()){
                return true;
            }
        }
        
        return false;
    }

    /**
     * @return
     */
    public AccountInfo getAccount() {
        return this.account;
    }
 
    /**
     * @return the storageProviderResults
     */
    public List<StorageProviderResult> getStorageProviderResults() {
        return storageProviderResults;
    }
}
