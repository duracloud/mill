/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;

import org.duracloud.client.ContentStore;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.error.ContentStoreException;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducer;
import org.duracloud.mill.ltp.MorselQueue;
import org.duracloud.mill.ltp.RunStats;
import org.duracloud.mill.ltp.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 28, 2014
 */
public class LoopingBitIntegrityTaskProducer extends LoopingTaskProducer<BitIntegrityMorsel> {
    private static Logger log = LoggerFactory.getLogger(LoopingBitIntegrityTaskProducer.class);

    public LoopingBitIntegrityTaskProducer(CredentialsRepo credentialsRepo,
            StorageProviderFactory storageProviderFactory,
            TaskQueue taskQueue,
            Cache cache, 
            StateManager<BitIntegrityMorsel> state,
            int maxTaskQueueSize, 
            Frequency frequency) {
        super(credentialsRepo, storageProviderFactory, taskQueue, cache, state,maxTaskQueueSize,frequency);
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#loadMorselQueueFromSource(org.duracloud.mill.ltp.MorselQueue)
     */
    @Override
    protected void loadMorselQueueFromSource(MorselQueue<BitIntegrityMorsel> morselQueue) {
        //generate set of morsels based on duplication policy
        try {
            for(String account :  getAccountsList()){
                List<ContentStore> contentStores = getContentStores(account);
                for(ContentStore contentStore : contentStores){
                    for(String spaceId : contentStore.getSpaces()){
                        morselQueue.add(
                                new BitIntegrityMorsel(account,
                                                       contentStore.getStoreId(), 
                                                       contentStore.getStorageProviderType(), 
                                                       spaceId));
                   }
                }
            }
        } catch (ContentStoreException e) {
            throw new DuraCloudRuntimeException(e);
        }
    }
    
    /**
     * @param account
     * @return
     */
    private List<ContentStore> getContentStores(String account) {
        return null;
    }

    /**
     * @return
     */
    private List<String> getAccountsList() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#nibble(org.duracloud.mill.ltp.Morsel)
     */
    @Override
    protected void nibble(BitIntegrityMorsel morsel) {
        
    }
    
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logIncrementalStatsBySubdomain(java.lang.String, org.duracloud.mill.ltp.RunStats)
     */
    @Override
    protected void logIncrementalStatsBySubdomain(String subdomain,
            RunStats stats) {
            log.info("Session stats by subdomain (incremental): subdomain={}",
                    subdomain);
   
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logCumulativeSessionStats()
     */
    @Override
    protected void logCumulativeSessionStats(Map<String,RunStats> runstats, RunStats cumulativeTotals) {
        log.info("session stats (global cumulative): domains={}",
                runstats.keySet().size());
    }
    
/* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logGlobalncrementalStats(org.duracloud.mill.ltp.RunStats)
     */
    @Override
    protected void logGlobalncrementalStats(RunStats incrementalTotals) {
        log.info("Session stats (global incremental)");
   
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#createRunStats()
     */
    @Override
    protected RunStats createRunStats() {
            return new BitIntegrityRunStats();
    }
      
}
