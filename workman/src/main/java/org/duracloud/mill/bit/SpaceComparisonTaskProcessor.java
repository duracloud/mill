/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.Iterator;

import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.db.model.ManifestItem;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.util.WriteOnlyStringSet;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task processor loops through all the items in the manifest and makes sure that 
 * they exist in storage provider.
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
        log.info("starting space comparison where account={} storeId={} spaceId={}",
                 account,
                 storeId,
                 spaceId);

        
        File spaceListing = null;
        try {
            
            //write out space listing to file, counting all the items in the process.
            spaceListing = File.createTempFile("spaces", ".txt");
            int count = 0;
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(spaceListing))){
                Iterator<String> it = store.getSpaceContents(spaceId, null);
                while(it.hasNext()){
                    if(count > 0){
                        writer.write("\n");
                    }
                    writer.write(it.next());
                    count++;
                }
                writer.close();
            }
            
            //load the set of content ids
            WriteOnlyStringSet set = new WriteOnlyStringSet(count);
            try(BufferedReader reader = new BufferedReader(new FileReader(spaceListing))){
                String contentId = null;
                while((contentId = reader.readLine()) != null){
                    set.add(contentId);
                }
            }
                
              
            // iterate current non-deleted manifest items
            Iterator<ManifestItem> manifestIterator = this.manifestStore
                    .getItems(account, storeId, spaceId);
            
            while (manifestIterator.hasNext()) {
                // if not in storage provider
                ManifestItem item = manifestIterator.next();
                
                String contentId = item.getContentId();
                if(set.contains(contentId)){
                    // reset missingInStorageProvider flag to false if true.
                    if (item.isMissingFromStorageProvider()) {
                        manifestStore
                                .updateMissingFromStorageProviderFlag(account,
                                                                      storeId,
                                                                      spaceId,
                                                                      contentId,
                                                                      false);
                    }
                }else{
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
            log.error(ex.getMessage(), ex);
            throw new TaskExecutionFailedException("failed to complete task: "
                    + ex.getMessage(), ex);
        } finally {
            if(spaceListing != null && spaceListing.exists()){
                spaceListing.delete();
            }
        }
    }
}
