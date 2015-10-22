/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog.jpa;

import java.util.Date;
import java.util.Iterator;

import org.duracloud.common.collection.StreamingIterator;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.bitlog.ItemWriteFailedException;
import org.duracloud.mill.db.model.BitIntegrityReport;
import org.duracloud.mill.db.repo.JpaBitIntegrityReportRepo;
import org.duracloud.mill.db.repo.MillJpaRepoConfig;
import org.duracloud.common.collection.jpa.JpaIteratorSource;
import org.duracloud.reportdata.bitintegrity.BitIntegrityReportResult;
import org.duracloud.storage.domain.StorageProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Daniel Bernstein Date: Oct 17, 2014
 */
public class JpaBitLogStore implements
                           BitLogStore {

    private static Logger log = LoggerFactory.getLogger(JpaBitLogStore.class);
    private JpaBitLogItemRepo bitLogItemRepo;
    private JpaBitIntegrityReportRepo bitReportRepo;

    /**
     * @param bitLogRepo
     */
    public JpaBitLogStore(JpaBitLogItemRepo bitLogItemRepo, JpaBitIntegrityReportRepo bitReportRepo) {
        this.bitLogItemRepo = bitLogItemRepo;
        this.bitReportRepo = bitReportRepo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.bitlog.BitLogStore#write(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, long,
     * org.duracloud.storage.domain.StorageProviderType,
     * org.duracloud.mill.bitlog.BitIntegrityResult, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    @Transactional(MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
    @Override
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
                            String details) throws ItemWriteFailedException {
        try {
            JpaBitLogItem item = new JpaBitLogItem();

            item.setAccount(accountId);
            item.setStoreId(storeId);
            item.setStorageProviderType(storeType);
            item.setSpaceId(spaceId);
            item.setContentId(contentId);
            item.setContentChecksum(contentCheckSum);
            item.setStorageProviderChecksum(storageProviderChecksum);
            item.setManifestChecksum(manifestChecksum);
            item.setDetails(details);
            item.setResult(result);
            item.setModified(timestamp);
            return this.bitLogItemRepo.saveAndFlush(item);

        } catch (Exception ex) {
            throw new ItemWriteFailedException(ex);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.bitlog.BitLogStore#getBitLogItems(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Iterator<BitLogItem> getBitLogItems(final String account,
                                               final String storeId,
                                               final String spaceId) {

        return (Iterator) new StreamingIterator<JpaBitLogItem>(new JpaIteratorSource<JpaBitLogItemRepo, JpaBitLogItem>(bitLogItemRepo, 50000) {
            @Override
            protected Page<JpaBitLogItem> getNextPage(Pageable pageable,
                                                      JpaBitLogItemRepo repo) {
                return repo.findByAccountAndStoreIdAndSpaceId(account,
                                                              storeId,
                                                              spaceId,
                                                              pageable);
            }
        });

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.bitlog.BitLogStore#delete(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    @Transactional(MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
    public void delete(String account, String storeId, String spaceId) {
        int deleted = 0;
        while((deleted = bitLogItemRepo.deleteFirst50000ByAccountAndStoreIdAndSpaceId(account,
                                                           storeId,
                                                           spaceId)) > 0){
            log.info("deleted {} bit log items where account = {}, store_id = {}, space_id = {}",
                     deleted,
                     account,
                     storeId,
                     spaceId);
            
            this.bitLogItemRepo.flush();
        }
    }
    
    
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogStore#addReport(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.duracloud.mill.bitlog.BitIntegrityResult, java.util.Date)
     */
    @Transactional(MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
    @Override
    public BitIntegrityReport addReport(String account,
                          String storeId,
                          String spaceId,
                          String reportSpaceId,
                          String reportContentId,
                          BitIntegrityReportResult result,
                          Date completionDate) {
        BitIntegrityReport report = new BitIntegrityReport();
        report.setAccount(account);
        report.setStoreId(storeId);
        report.setSpaceId(spaceId);
        report.setReportSpaceId(reportSpaceId);
        report.setReportContentId(reportContentId);
        report.setCompletionDate(new Date());
        report.setResult(result);
        report.setDisplay(!result.equals(BitIntegrityReportResult.FAILURE));
        return this.bitReportRepo.saveAndFlush(report);
    }
    

}
