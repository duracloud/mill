/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.common.util.DateUtil;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.db.model.ManifestItem;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Daniel Bernstein Date: Oct 21, 2014
 */
public class SpaceComparisonTaskProcessor implements
                                         TaskProcessor {

    private static Logger log = LoggerFactory
            .getLogger(SpaceComparisonTaskProcessor.class);

    private BitIntegrityCheckReportTask bitTask;
    private BitLogStore bitLogStore;
    private ManifestStore manifestStore;
    private StorageProvider store;
    private StorageProviderType storageProviderType;
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DateUtil.DateFormat.DEFAULT_FORMAT
            .getPattern());

    /**
     * @param bitTask
     * @param bitLogStore
     * @param manifestStore
     * @param store
     * @param storageProviderType
     */
    public SpaceComparisonTaskProcessor(BitIntegrityCheckReportTask bitTask,
                                        BitLogStore bitLogStore,
                                        ManifestStore manifestStore,
                                        StorageProvider store,
                                        StorageProviderType storageProviderType) {
        this.bitTask = bitTask;
        this.bitLogStore = bitLogStore;
        this.manifestStore = manifestStore;
        this.storageProviderType = storageProviderType;
        this.store = store;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.workman.TaskProcessor#execute()
     */
    @Override
    public void execute() throws TaskExecutionFailedException {
        String account = bitTask.getAccount();
        String storeId = bitTask.getStoreId();
        String spaceId = bitTask.getSpaceId();
        try {
            // iterate through all content items in the storage provider
            Iterator<String> it = this.store.getSpaceContents(spaceId, null);

            while (it.hasNext()) {
                String contentId = it.next();
                ManifestItem item;
                try{
                    item = this.manifestStore.getItem(account,
                                                      storeId,
                                                      spaceId,
                                                      contentId);
                }catch(org.duracloud.error.NotFoundException ex){
                    item = null;
                }
                
                if (item == null || item.isDeleted()) {
                    // if there is no non-deleted item in the manifest
                    // if storage provider content is less than a day old
                    Map<String, String> props = store
                            .getContentProperties(spaceId, contentId);
                    String modified = props
                            .get(StorageProvider.PROPERTIES_CONTENT_MODIFIED);
                    String checksum = props
                            .get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
                    Date modifiedDate = null;
                    if (modified != null) {
                        modifiedDate = DATE_FORMAT.parse(modified);
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DATE, -1);
                    Date oneDayAgo = calendar.getTime();
                    if (modified == null || modifiedDate.before(oneDayAgo)) {
                        this.bitLogStore
                                .write(account,
                                       storeId,
                                       spaceId,
                                       contentId,
                                       new Date(),
                                       storageProviderType,
                                       BitIntegrityResult.ERROR,
                                       null,
                                       checksum,
                                       null,
                                       "Content item is in the storage provider but not in the manifest and the content is more than a day old.");

                    }
                }
            }

            // iterate current non-deleted manifest items
            Iterator<ManifestItem> manifestIterator = this.manifestStore
                    .getItems(account, storeId, spaceId);

            while (manifestIterator.hasNext()) {
                // if not in storage provider
                ManifestItem item = manifestIterator.next();
                String contentId = item.getContentId();

                try {
                    store.getContentProperties(spaceId, contentId);
                    // reset missingInStorageProvider flag to false if true.
                    if (item.isMissingFromStorageProvider()) {
                        manifestStore
                                .updateMissingFromStorageProviderFlag(account,
                                                                      storeId,
                                                                      spaceId,
                                                                      contentId,
                                                                      false);
                    }
                } catch (NotFoundException ex) {
                    log.debug("no content found in storage provider for manifest item: {}",
                              item);
                    if (!item.isDeleted()) {
                        // is missingInStorageProvider flag true?
                        if (item.isMissingFromStorageProvider()) {
                            this.bitLogStore
                                    .write(account,
                                           storeId,
                                           spaceId,
                                           contentId,
                                           new Date(),
                                           storageProviderType,
                                           BitIntegrityResult.ERROR,
                                           null,
                                           null,
                                           item.getContentChecksum(),
                                           "Content item is in the manifest "
                                                   + "but not in the storage provider in "
                                                   + "the course of the last two bit integrity runs.");
                        } else {
                            manifestStore
                                    .updateMissingFromStorageProviderFlag(account,
                                                                          storeId,
                                                                          spaceId,
                                                                          contentId,
                                                                          true);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            throw new TaskExecutionFailedException("failed to complete task: "
                    + ex.getMessage(), ex);
        }

    }

}
