/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;

import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.bi.BitIntegrityCheckTask;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducer;
import org.duracloud.mill.ltp.MorselQueue;
import org.duracloud.mill.ltp.RunStats;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
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
                AccountCredentials accountCreds = getCredentialsRepo().getAccountCredentials(account);
                for(StorageProviderCredentials cred : accountCreds.getProviderCredentials()){
                    StorageProvider store = getStorageProvider(cred);
                    
                    Iterator<String> spaces = store.getSpaces();
                    while(spaces.hasNext()){
                        String spaceId = spaces.next();
                        morselQueue.add(
                                new BitIntegrityMorsel(account,
                                                       cred.getProviderId(), 
                                                       cred.getProviderType().name(), 
                                                       spaceId));
                    }
                }
            }
        } catch (Exception e) { 
            log.error(e.getMessage(), e);
            throw new DuraCloudRuntimeException(e);
        }
    }
    
    /**
     * @return
     * @throws CredentialsRepoException 
     */
    private List<String> getAccountsList() throws CredentialsRepoException {
        return getCredentialsRepo().getAccounts();
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#nibble(org.duracloud.mill.ltp.Morsel)
     */
    @Override
    protected void nibble(BitIntegrityMorsel morsel) {
        String account = morsel.getAccount();
        String storeId = morsel.getStoreId();
        String spaceId = morsel.getSpaceId();
        
        StorageProvider store = 
                getStorageProvider(morsel.getAccount(),storeId);

        
        int maxTaskQueueSize = getMaxTaskQueueSize();
        int taskQueueSize = getTaskQueue().size();
        if(taskQueueSize >= maxTaskQueueSize){
            log.info(
                    "Task queue size ({}) has reached or exceeded max size ({}).",
                    taskQueueSize, maxTaskQueueSize);
            addToReloadList(morsel);
        } else{
            
            if(addTasks(morsel, store, 1000)){
                log.info(
                        "All bit integritytasks that could be created were created for account={}, storeId={}, spaceId={}. getTaskQueue().size = {}",
                        account, storeId, spaceId, getTaskQueue().size());
                log.info(
                        "{} completely nibbled. No reload necessary in this round.",
                        morsel);
            } else {
                log.info(
                        "morsel nibbled down: {}",
                        morsel);
                addToReloadList(morsel);
            }
            
        }   
        
    }
    
    
    /**
     * @param morsel
     * @param store
     * @param bitSize
     * @return
     */
    private boolean addTasks(BitIntegrityMorsel morsel,
            StorageProvider store,
            int biteSize) {
        String account = morsel.getAccount();
        String storeId = morsel.getStoreId();
        String spaceId = morsel.getSpaceId();
        String marker = morsel.getMarker();
        
        //load in next maxContentIdsToAdd or however many remain 
        List<String> contentIds = null;
        
        try {
            contentIds = store.getSpaceContentsChunked(spaceId, 
                                                     null, 
                                                     biteSize, 
                                                     marker);

            //add to queue
            int contentIdCount = contentIds.size();
            
            if(contentIdCount == 0){
                return true;
            }else {
                int added = addToTaskQueue(account, 
                                           storeId, 
                                           spaceId,
                                           contentIds);

                ((BitIntegrityRunStats)getStats(account)).add(added);
                //if no tasks were added, it means that all contentIds in this morsel
                //have been touched in this run.
                if(added == 0){
                    return true;
                }else{
                    marker = contentIds.get(contentIds.size()-1);
                    morsel.setMarker(marker);
                }
                
                return false;
            } 
            

        }catch(NotFoundException ex){
            log.info("space not found on storage provider: " +
                    "subdomain={}, spaceId={}, storeId={}",
                    account, spaceId, storeId);
            
           return true;
        }
    }

    /**
     * @param account
     * @param storeId
     * @param contentIds
     * @return
     */
    private int addToTaskQueue(
            String account,
            String storeId,
            String spaceId,
            List<String> contentIds) {
        Set<Task> tasks = new HashSet<>();
        int addedCount = 0;
        
        for(String contentId : contentIds){
            BitIntegrityCheckTask bitIntegrityTask = new BitIntegrityCheckTask();
            bitIntegrityTask.setAccount(account);
            bitIntegrityTask.setContentId(contentId);
            bitIntegrityTask.setSpaceId(spaceId);
            bitIntegrityTask.setStoreId(storeId);
            
            Task task = bitIntegrityTask.writeTask();
            tasks.add(task);
            addedCount++;
        }

        getTaskQueue().put(tasks);
        
        return addedCount;
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
