/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest.builder;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.duracloud.client.ContentStore;
import org.duracloud.common.util.DateUtil;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Daniel Bernstein Date: Jan 2, 2015
 */
@Component
public class ManifestBuilder {
    private static final Logger log = LoggerFactory
            .getLogger(ManifestBuilder.class);
    private Collection<ContentStore> contentStores;
    private List<String> spaceList;
    private String account;
    private boolean dryRun;
    private boolean clean;
    private ManifestStore manifestStore;
    private ThreadPoolExecutor executor;
    private int successes = 0;
    private int errors = 0;
    private int totalProcessed = 0;
    @Autowired
    public ManifestBuilder(ManifestStore manifestStore) {
        this.manifestStore = manifestStore;
    }

    /**
     * 
     * @param account
     * @param contentStores
     * @param spaceList
     * @param manifestItemRepo
     * @param clean
     * @param dryRun
     * @param threads 
     */
    public void init(String account,
                     Collection<ContentStore> contentStores,
                     List<String> spaceList,
                     boolean clean,
                     boolean dryRun, 
                     int threads) {
        this.account = account;
        this.contentStores = contentStores;
        this.spaceList = spaceList;
        this.clean = clean;
        this.dryRun = dryRun;
        
        if(this.executor != null){
            this.executor.shutdownNow();
        }

        this.successes = 0;
        this.errors = 0;
        this.totalProcessed = 0;

        this.executor = new ThreadPoolExecutor(threads,
                                               threads,
                                               0l,
                                               TimeUnit.MILLISECONDS,
                                               new LinkedBlockingQueue<Runnable>(500));  

    }

    public void execute() throws Exception {
        long startTime = System.currentTimeMillis();
        
        if(clean){
            clean();
        }
        build();
        
        this.executor.shutdown();
        log.info("awaiting the completion of all outstanding tasks...");
        if(this.executor.awaitTermination(5, TimeUnit.MINUTES)){
            log.info("Completed all tasks.");
        }else{
            log.info("Unable to complete all tasks within the timeout period of 5 minutes.");
            this.executor.shutdownNow();
        }
        
        String duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis()-startTime);
        
        log.info("duration={} total_item_processed={}  successes={} errors={}",
                 duration,
                 totalProcessed,
                 successes,
                 errors);
        
    }
    

    /**
     * 
     */
    private void build() throws Exception {
        for (ContentStore store : contentStores) {
            String storeId = store.getStoreId();
            List<String> spaces = store.getSpaces();
            for (String spaceId : spaces) {
                if (spaceList.isEmpty() || spaceList.contains(spaceId)) {
                    buildSpace(storeId, spaceId, store);
                }
            }
        }

        
    }

    /**
     * @param spaceId
     * @param spaceId 
     * @param store
     */
    private void
            buildSpace(final String storeId, final String spaceId, final ContentStore store) throws Exception {
        log.info("starting manifest rebuild for storeId={} spaceId={}",storeId, spaceId);
        Iterator<String> contentIds = store.getSpaceContents(spaceId);
        while (contentIds.hasNext()) {
            final String contentId = contentIds.next();

            while(true){
                try{
                    this.executor.execute(new Runnable(){
                        /* (non-Javadoc)
                         * @see java.lang.Runnable#run()
                         */
                        @Override
                        public void run() {
                            try {
                                updateContentId(storeId,
                                                spaceId,
                                                contentId,
                                                store);
                                successes++;
                            } catch (Exception e) {
                                errors++;
                                log.error(MessageFormat
                                                  .format("failed to update manifest for storeId={0} spaceId={1} contentId={2} message={3}",
                                                          storeId,
                                                          spaceId,
                                                          contentId,
                                                          e.getMessage()),
                                          e);
                            }
                            
                        }
                        
                    });
                    
                    break;
                }catch(RejectedExecutionException ex){
                    log.debug("failed to add new task: {} : thread executor -> taskCount={}, current pool size={}",
                              ex.getMessage(),
                              executor.getTaskCount(),
                              executor.getPoolSize());
                    log.debug("Thread pool busy sleeping for 10ms");
                    sleep();
                }
            }

            this.totalProcessed++;

        }
        
        log.info("all manifest rebuild tasks scheduled for  for storeId={} spaceId={}",storeId, spaceId);
 
    }

    private void sleep() {
        try{
            Thread.sleep(10);
        }catch(InterruptedException e){}
    }

    /**
     * @param store
     * @param spaceId
     * @param contentId
     */
    private void updateContentId(String storeId,
                                 String spaceId,
                                 String contentId,
                                 ContentStore store) throws Exception {
        String message = MessageFormat
                .format("rebuilt manifest entry for storeId={0} spaceId=\"{1}\" contentId=\"{2}\"",
                        storeId,
                        spaceId,
                        contentId);
        if (dryRun) {
            log.info("(dry run: no update) - " + message);
        } else {
            log.debug("about to rebuild manifest entry for storeId={} spaceId=\"{}\" contentId=\"{}\"",
                      storeId,
                      spaceId,
                      contentId);
            Map<String, String> props = store.getContentProperties(spaceId,
                                                                   contentId);
            String modified = props
                    .get(StorageProvider.PROPERTIES_CONTENT_MODIFIED);
            Date timeStamp;

            try {
                timeStamp = DateUtil.convertToDate(modified);
            } catch (Exception ex) {
                timeStamp = new Date();
            }

            manifestStore.addUpdate(account, 
                                    storeId, 
                                    spaceId, 
                                    contentId, 
                                    props.get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM), 
                                    props.get(StorageProvider.PROPERTIES_CONTENT_MIMETYPE), 
                                    props.get(StorageProvider.PROPERTIES_CONTENT_SIZE), 
                                    timeStamp);

            log.info(message);

        }
    }

    /**
     * 
     */
    private void clean() throws Exception {
        for (ContentStore store : contentStores) {
            String storeId = store.getStoreId();
            List<String> spaces = store.getSpaces();
            for (String spaceId : spaces) {
                if (spaceList.isEmpty() || spaceList.contains(spaceId)) {
                    if (!dryRun) {
                        manifestStore.delete(account, storeId, spaceId);
                        log.info("manifest deleted for storeId={} spaceId={}",
                                 storeId,
                                 spaceId);
                    } else {
                        log.info("you're in dry run mode: manifest for storeId={} spaceId={} will be "
                                         + "deleted when doing a \"wet\" run. No modifications have been made.",
                                 storeId,
                                 spaceId);
                    }
                }
            }
        }
    }
}
