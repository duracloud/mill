/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducer;
import org.duracloud.mill.ltp.LoopingTaskProducerConfigurationManager;
import org.duracloud.mill.ltp.MorselComparator;
import org.duracloud.mill.ltp.RunStats;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.task.DuplicationTask;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 23, 2014
 */
public class LoopingDuplicationTaskProducer extends LoopingTaskProducer<DuplicationMorsel> {
    private static Logger log = LoggerFactory.getLogger(LoopingDuplicationTaskProducer.class);

    private DuplicationPolicyManager policyManager;

    private Cache cache;

    public LoopingDuplicationTaskProducer(CredentialsRepo credentialsRepo,
            StorageProviderFactory storageProviderFactory,
            DuplicationPolicyManager policyManager, 
            TaskQueue taskQueue,
            Cache cache, 
            StateManager<DuplicationMorsel> state,
            int maxTaskQueueSize, 
            Frequency frequency,
            NotificationManager notificationManager, 
            LoopingTaskProducerConfigurationManager config) {
        super(credentialsRepo,
              storageProviderFactory,
              taskQueue,
              state,
              maxTaskQueueSize,
              frequency,
              null,
              notificationManager,
              config);
        this.cache = cache;
        this.policyManager = policyManager;
    }
    
    /**
     * @return the cache
     */
    private Cache getCache() {
        return cache;
    }
    

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#loadMorselQueueFromSource(java.util.Queue)
     */
    @Override
    protected void loadMorselQueueFromSource(Queue<DuplicationMorsel> morselQueue) {
        //generate set of morsels based on duplication policy
        for(String account : this.policyManager.getDuplicationAccounts()){
            DuplicationPolicy policy = this.policyManager.getDuplicationPolicy(account);
            try {
                final CredentialsRepo credRepo = getCredentialsRepo();
                if(getCredentialsRepo().isAccountActive(account)) {
                    AccountCredentials accountCreds = credRepo.getAccountCredentials(account);
                    for (StorageProviderCredentials cred : accountCreds.getProviderCredentials()) {
                        if (cred.isPrimary()) {
                            StorageProvider provider = getStorageProvider(cred);
                            Iterator<String> spaces = provider.getSpaces();
                            while (spaces.hasNext()) {
                                String spaceId = spaces.next();
                                Set<DuplicationStorePolicy> storePolicies = policy.getDuplicationStorePolicies(spaceId);
                                for (DuplicationStorePolicy storePolicy : storePolicies) {
                                    morselQueue.add(new DuplicationMorsel(account, spaceId, null, storePolicy));
                                }
                            }
                        }
                    }
                }
            } catch (AccountCredentialsNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#nibble(org.duracloud.mill.ltp.Queue<T>)
     */
    @Override
    protected void nibble(Queue<DuplicationMorsel> queue) {
        DuplicationMorsel morsel = queue.poll();
        String account = morsel.getAccount();
        String spaceId = morsel.getSpaceId();
        DuplicationStorePolicy storePolicy = morsel.getStorePolicy();
        
        //get all items from source
        StorageProvider sourceProvider = 
                getStorageProvider(account, 
                                   storePolicy.getSrcStoreId());
        StorageProvider destProvider = 
                getStorageProvider(account, 
                                   storePolicy.getDestStoreId());

        if(!morsel.isDeletePerformed()){
            addDuplicationTasksForContentNotInSource(account,
                                                        spaceId, 
                                                        storePolicy, 
                                                        sourceProvider, 
                                                        destProvider);
            morsel.setDeletePerformed(true);
        }
        
        int maxTaskQueueSize = getMaxTaskQueueSize();
        int taskQueueSize = getTaskQueue().size();
        if(taskQueueSize >= maxTaskQueueSize){
            log.info(
                    "Task queue size ({}) has reached or exceeded max size ({}).",
                    taskQueueSize, maxTaskQueueSize);
            addToReloadList(morsel);
        } else{
            if(addDuplicationTasksFromSource(morsel, sourceProvider, 1000)){
                log.info(
                        "All tasks that could be created were created for account={}, spaceId={}, storePolicy={}. getTaskQueue().size = {}",
                        account, spaceId, storePolicy, getTaskQueue().size());
                log.info(
                        "morsel completely nibbled. No reload necessary in this round.",
                        morsel);
            } else {
                log.info(
                        "morsel nibbled a bit: {}",
                        morsel);
                addToReloadList(morsel);
            }
            
        }   
    }

    @Override
    protected StorageProvider getStorageProvider(String account, String storeId) {
        return super.getStorageProvider(account, storeId);
    }

    /**
     * 
     * @param morsel
     * @param sourceProvider
     * @param maxContentIdsToAdd
     * @return true if morsel exhausted, false if morsel needs to be requeued.
     */
    private boolean addDuplicationTasksFromSource(DuplicationMorsel morsel, StorageProvider sourceProvider, int maxContentIdsToAdd) {

        String account = morsel.getAccount();
        String spaceId = morsel.getSpaceId();
        String marker = morsel.getMarker();
        DuplicationStorePolicy storePolicy = morsel.getStorePolicy();

        //load in next maxContentIdsToAdd or however many remain 
        List<String> contentIds = null;
        
        try {
            contentIds = sourceProvider.getSpaceContentsChunked(spaceId, 
                                                     null, 
                                                     maxContentIdsToAdd, 
                                                     marker);
        }catch(NotFoundException ex){
            log.info("space not found on source provider: " +
                    "account={}, spaceId={}, storeId={}",
                    account, spaceId, sourceProvider);
            
            addDeleteSpaceTaskToQueue(account, 
                                      spaceId, 
                                      storePolicy,
                                      sourceProvider);
            return true;
        }
        //add to queue
        int contentIdCount = contentIds.size();
        
        if(contentIdCount == 0){
            return true;
        }else {
            int added = addToTaskQueue(account, 
                                       spaceId, 
                                       storePolicy,
                                       contentIds);
            ((DuplicationRunStats)getStats(account)).addToDups(added);
            //if no tasks were added, it means that all contentIds in this morsel
            //have been touched in this run.
            if(added == 0){
                return true;
            }else{
                marker = contentIds.get(contentIds.size()-1);
                morsel.setMarker(marker);
            }
        } 
        
        return false;
    }

    private void addDuplicationTasksForContentNotInSource(
            String account, String spaceId,
            DuplicationStorePolicy storePolicy, StorageProvider sourceProvider,
            StorageProvider destProvider) {

        Cache cache = getCache();
        try{
            //load all source into ehcache
            Iterator<String> sourceContentIds = sourceProvider.getSpaceContents(spaceId, null);
            while(sourceContentIds.hasNext()){
                cache.put(new Element(sourceContentIds.next(), null));
            }
        }catch(NotFoundException ex){
            log.info("space not found on source provider: " +
                    "account={}, spaceId={}, storeId={}",
                    account, spaceId, sourceProvider);
        }

        
        //get all items from dest
        Iterator<String> destContentIds = null;
        
        try{
            destContentIds = destProvider.getSpaceContents(spaceId, null);
        }catch(NotFoundException ex){
            log.info("space not found on destination provider: " +
                     "account={}, spaceId={}, storeId={}",
                     account, spaceId, destProvider);
            return;
        }
        
        int deletionTaskCount = 0;
        //for each one 
        List<String> deletions = new LinkedList<String>();
        while(destContentIds.hasNext()){
            String destContentId = destContentIds.next();
            //if not in cache
            if(!cache.isKeyInCache(destContentId)){
                deletions.add(destContentId);
                //periodically add deletions to prevent OOM
                //in case that there are millions of content ids to delete
                if(deletions.size() == 10000){
                    //create dup task
                    deletionTaskCount += addToTaskQueue(account, spaceId, storePolicy, deletions);
                    deletions.clear();
                }
            }
        }
        
        //add any remaining deletions
        deletionTaskCount += addToTaskQueue(account, spaceId, storePolicy, deletions);
        ((DuplicationRunStats)getStats(account)).addToDeletes(deletionTaskCount);
        
        log.info(
                "added {} deletion tasks: account={}, spaceId={}, sourceStoreId={}, destStoreId={}",
                deletionTaskCount, 
                account, 
                spaceId, 
                storePolicy.getSrcStoreId(),
                storePolicy.getDestStoreId());
        cache.removeAll();
    }

    /**
     * @param account
     * @param spaceId
     * @param storePolicy
     * @param sourceProvider
     */
    private void addDeleteSpaceTaskToQueue(String account,
            String spaceId,
            DuplicationStorePolicy storePolicy,
            StorageProvider sourceProvider) {
        // drop a delete space message. (ie a duplication message with
        // no content - should trigger a destination space deletion);
        DuplicationTask task = new DuplicationTask();
        task.setAccount(account);
        task.setSourceStoreId(storePolicy.getSrcStoreId());
        task.setContentId(""); 
        task.setDestStoreId(storePolicy.getDestStoreId());
        task.setSpaceId(spaceId);
        this.getTaskQueue().put(task.writeTask());
        log.info("destintation space delete task added to queue " +
                "since source does not exist: " +
                "account={}, spaceId={}, storeId={}",
                account, spaceId, sourceProvider);
    }

    private int addToTaskQueue(String account, String spaceId,
            DuplicationStorePolicy storePolicy, List<String> contentIds) {
        Set<Task> tasks = new HashSet<>();
        int addedCount = 0;
        
        for(String contentId : contentIds){
            DuplicationTask dupTask = new DuplicationTask();
            dupTask.setAccount(account);
            dupTask.setContentId(contentId);
            dupTask.setSpaceId(spaceId);
            dupTask.setStoreId(storePolicy.getSrcStoreId());
            dupTask.setSourceStoreId(storePolicy.getSrcStoreId());
            dupTask.setDestStoreId(storePolicy.getDestStoreId());
            
            Task task = dupTask.writeTask();
            tasks.add(task);
            addedCount++;
        }

        getTaskQueue().put(tasks);
        
        return addedCount;
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logIncrementalStatsByAccount(java.lang.String, org.duracloud.mill.ltp.RunStats)
     */
    @Override
    protected void logIncrementalStatsByAccount(String account,
            RunStats stats) {
    DuplicationRunStats dstats = (DuplicationRunStats)stats;
            log.info("Session stats by account (incremental): account={} dups={} deletes={}",
                    account, 
                    dstats.getDups(), 
                    dstats.getDeletes());
   
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logCumulativeSessionStats()
     */
    @Override
    protected void logCumulativeSessionStats(Map<String,RunStats> runstats, RunStats cumulativeTotals) {
        DuplicationRunStats dCumulativeTotals = (DuplicationRunStats)cumulativeTotals;
        log.info("session stats (global cumulative): domains={} dups={}  deletes={}",
                runstats.keySet().size(), dCumulativeTotals.getDups(), dCumulativeTotals.getDeletes());
   
    }
    
/* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logGlobalncrementalStats(org.duracloud.mill.ltp.RunStats)
     */
    @Override
    protected void logGlobalncrementalStats(RunStats incrementalTotals) {
        DuplicationRunStats dIncrementalTotals = (DuplicationRunStats) incrementalTotals;

        log.info("Session stats (global incremental): dups={} deletes={}",
                dIncrementalTotals.getDups(), 
                dIncrementalTotals.getDeletes());
   
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#createRunStats()
     */
    @Override
    protected RunStats createRunStats() {
            return new DuplicationRunStats();
    }
      
    /**/
    @Override
    protected Queue<DuplicationMorsel> createQueue() {
        return new PriorityBlockingQueue<>(1000, new MorselComparator());
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#getLoopingProducerTypePrefix()
     */
    @Override
    protected String getLoopingProducerTypePrefix() {
        return "dup";
    }
    
}
