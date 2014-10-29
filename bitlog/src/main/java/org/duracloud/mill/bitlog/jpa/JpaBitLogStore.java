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
import java.util.List;

import org.duracloud.common.collection.StreamingIterator;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.bitlog.ItemWriteFailedException;
import org.duracloud.mill.db.model.BitIntegrityReport;
import org.duracloud.mill.db.model.BitIntegrityReportResult;
import org.duracloud.mill.db.repo.JpaBitIntegrityReportRepo;
import org.duracloud.mill.db.repo.MillJpaRepoConfig;
import org.duracloud.mill.db.util.JpaIteratorSource;
import org.duracloud.storage.domain.StorageProviderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author Daniel Bernstein Date: Oct 17, 2014
 */
@Transactional(MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
public class JpaBitLogStore implements
                           BitLogStore {

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

        return (Iterator) new StreamingIterator<JpaBitLogItem>(new JpaIteratorSource<JpaBitLogItemRepo, JpaBitLogItem>(bitLogItemRepo) {
            @Override
            protected Page<JpaBitLogItem> getNextPage(Pageable pageable,
                                                      JpaBitLogItemRepo repo) {
                return repo
                        .findByAccountAndStoreIdAndSpaceIdOrderByContentIdAsc(account,
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
    public void delete(String account, String storeId, String spaceId) {
        bitLogItemRepo.deleteByAccountAndStoreIdAndSpaceId(account,
                                                           storeId,
                                                           spaceId);
    }
    
    
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogStore#addReport(java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.duracloud.mill.bitlog.BitIntegrityResult, java.util.Date)
     */
    @Override
    public void addReport(String account,
                          String storeId,
                          String spaceId,
                          String reportContentId,
                          BitIntegrityReportResult result,
                          Date completionDate) {
        BitIntegrityReport report = new BitIntegrityReport();
        report.setAccount(account);
        report.setStoreId(storeId);
        report.setSpaceId(spaceId);
        report.setReportContentId(reportContentId);
        report.setCompletionDate(new Date());
        report.setResult(result);
        this.bitReportRepo.save(report);
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.bitlog.BitLogStore#isCompletelySuccessful(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean isCompletelySuccessful(String account,
                                          String storeId,
                                          String spaceId) {
        List<BitLogItem> items = this.bitLogItemRepo.findErrorsAndFailures(account, storeId, spaceId);
        
        return (CollectionUtils.isEmpty(items));
    }
}
