/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.sf.ehcache.Cache;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for filling the duplication <code>TaskQueue</code>
 * by looping through all duplication policies for all accounts and spaces and
 * blindly creates duplication tasks. It will create tasks for all items in the
 * the source destination as well as all items in the destination provider but
 * not in the source provider. A notable feature of this task producer is that
 * it attempts to respect a designated maximum task queue size. Once the limit
 * has been reached, the producer will stop. On subsequent runs, the producer
 * will pick up where it left off, starting with the next account,space,set of
 * content items, and duplication store policy. If all content items are visited
 * within a single run before the task queue limit has been reached, the
 * producer will exit.
 * 
 * For more information about how this process fits into the whole system of
 * collaborating components, see
 * https://wiki.duraspace.org/display/DSPINT/DuraCloud+Duplication+-+System+Overview
 * 
 * @author Daniel Bernstein Date: Nov 5, 2013
 */
public abstract class LoopingTaskProducer<T extends Morsel> implements Runnable {
    private static Logger log = LoggerFactory.getLogger(LoopingTaskProducer.class);
    private TaskQueue taskQueue;
    private CredentialsRepo credentialsRepo;
    private Cache cache;
    private StateManager<T> stateManager;
    private int maxTaskQueueSize;
    private StorageProviderFactory storageProviderFactory;
    private List<T> morselsToReload = new LinkedList<>();
    private Frequency frequency;
    private RunStats cumulativeTotals;
    
    private Map<String,RunStats> runstats = new HashMap<>();

    public LoopingTaskProducer(CredentialsRepo credentialsRepo,
                               StorageProviderFactory storageProviderFactory,
                               TaskQueue taskQueue,
                               Cache cache, 
                               StateManager<T> state,
                               int maxTaskQueueSize, 
                               Frequency frequency) {
        
        this.credentialsRepo = credentialsRepo;
        this.storageProviderFactory = storageProviderFactory;
        this.taskQueue = taskQueue;
        this.cache = cache;
        this.stateManager = state;
        this.credentialsRepo = credentialsRepo;
        this.maxTaskQueueSize = maxTaskQueueSize;
        this.frequency = frequency;
        this.cumulativeTotals = createRunStats();
    }
    
    protected Frequency getFrequency(){
        return this.frequency;
    }
    
    /**
     * @return the cache
     */
    protected Cache getCache() {
        return cache;
    }
    

    protected CredentialsRepo getCredentialsRepo() {
        return credentialsRepo;
    }


    protected TaskQueue getTaskQueue() {
        return taskQueue;
    }

    protected int getMaxTaskQueueSize() {
        return maxTaskQueueSize;
    }
    
    public void run(){
        Timer timer = new Timer();
        try {
            
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logSessionStats();
                }

            }, 5 * 60 * 1000, 5 * 60 * 1000);  
            
            if(runLater()){
                return;
            }
            
            log.info("Starting run...");
            MorselQueue<T> morselQueue = loadMorselQueue();
            
            while(!morselQueue.isEmpty() && this.taskQueue.size() < maxTaskQueueSize){
                nibble(morselQueue.poll());
                persistMorsels(morselQueue, morselsToReload);
                
                if(morselQueue.isEmpty()){
                    morselQueue = reloadMorselQueue();
                }
            }
    
            if(morselQueue.isEmpty()){
                scheduleNextRun();
            }
            
            logSessionStats();
            log.info("Session ended.");
        }finally {
            timer.cancel();
        }
    }

    private void resetIncrementalSessionStats() {
        synchronized (runstats){
            for(String subdomain : runstats.keySet()){
                RunStats stats = runstats.get(subdomain);
                stats.reset();
            }
        }
    }
    
    protected RunStats calculateStatTotals(RunStats currentTotals){
        RunStats totals = createRunStats();
        totals.copyValuesFrom(currentTotals);

        synchronized (runstats){
            for(String subdomain : runstats.keySet()){
                RunStats stats = runstats.get(subdomain);
                totals.add(stats);
            }
            return totals;
        }
    }

    private void logSessionStats() {
        synchronized (runstats){
            for(String subdomain : runstats.keySet()){
                RunStats stats = runstats.get(subdomain);
                logIncrementalStatsBySubdomain(subdomain, stats);
            }

            RunStats incrementalTotals = calculateStatTotals(createRunStats());
            logGlobalncrementalStats(incrementalTotals);
            
            this.cumulativeTotals = calculateStatTotals(cumulativeTotals);
            logCumulativeSessionStats(runstats, this.cumulativeTotals);
            resetIncrementalSessionStats();
        }
    }

    /**
     * 
     */
    private void scheduleNextRun() {
        Date currentStartDate = this.stateManager.getCurrentRunStartDate();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentStartDate.getTime());
        c.add(this.frequency.getTimeUnit(), this.frequency.getValue());
        Date nextRun = c.getTime();
        this.stateManager.setNextRunStartDate(nextRun);
        this.stateManager.setCurrentRunStartDate(null);
        log.info("The run has completed.  Scheduling the next run for {}", nextRun);
    }

    /**
     * @return true if the process should wait until later
     */
    private boolean runLater() {
        boolean runLater = true;
        Date nextRun = this.stateManager.getNextRunStartDate();
        if(nextRun != null){
            Date now = new Date();
            if(now.after(nextRun)){
                this.stateManager.setCurrentRunStartDate(now);
                this.stateManager.setNextRunStartDate(null);
                runLater = false;
                log.info("Time to start a new run: the next run was scheduled to run on {}. Let's roll.", nextRun);
            }else{
                log.info("It's not yet time start a new run: the next run is scheduled to run on {}.", nextRun);
            }
        }else{
            Date currentRunStartDate = this.stateManager.getCurrentRunStartDate();
            if(currentRunStartDate == null){
                this.stateManager.setCurrentRunStartDate(new Date());
                log.info("We're starting the first run on this machine");
            }else{
                log.info("We're continuing the current run which was started on {}", currentRunStartDate);
            }
            
            runLater = false;
        }
        
        return runLater;
    }



    /**
     * @return
     */
    private MorselQueue<T> reloadMorselQueue() {
        List<T> morsels = morselsToReload;
        morselsToReload = new LinkedList<>();
        MorselQueue<T> queue = new MorselQueue<>();
        queue.addAll(morsels);
        return queue;
    }

    /**
     * Loads the morsels from the persistent state if there are any; otherwise it loads  all other morsels based on
     * on duplication policy manager.
     * 
     * @return
     */
    private MorselQueue<T> loadMorselQueue() {
        MorselQueue<T> morselQueue = new MorselQueue<>();
        
        //load morsels from state;
        Set<T> morsels = new HashSet<>(this.stateManager.getMorsels());
        
        morselQueue.addAll(morsels);

        if(morselQueue.isEmpty()){
            loadMorselQueueFromSource(morselQueue);
        }
        
        return morselQueue;
    }

    private void persistMorsels(MorselQueue<T> queue, List<T> morselsToReload){
        Set<T> morsels = new HashSet<>();
        morsels.addAll(queue);
        morsels.addAll(morselsToReload);
        stateManager.setMorsels(morsels);
    }

    /**
     * @param morsel
     */
    protected void addToReloadList(T morsel) {
        log.info(
                "adding morsel to reload list: {}",
                morsel);
        morselsToReload.add(morsel);
    }

    /**
     * @param subdomain
     * @return
     */
    protected RunStats getStats(String subdomain) {
        synchronized(runstats){
            RunStats stats = this.runstats.get(subdomain);
            if(stats == null){
                this.runstats.put(subdomain, stats = createRunStats());
            }
            return stats;
        }
    }

    protected StorageProvider getStorageProvider(String subdomain,
            String storeId)  {
        StorageProviderCredentials creds;
        try {
            creds = credentialsRepo.getStorageProviderCredentials(subdomain, 
                                                          storeId);
        } catch (CredentialsRepoException e) {
            throw new RuntimeException(e);
        }
        
        return getStorageProvider(creds);
    }

    /**
     * @param creds
     * @return
     */
    protected StorageProvider getStorageProvider(StorageProviderCredentials creds) {
        return storageProviderFactory.create(creds);
    }

    /**
     * @param morselQueue
     */
    protected abstract void loadMorselQueueFromSource(MorselQueue<T> morselQueue);

    /**
     * @param morsel
     */
    protected abstract void nibble(T morsel);
    

    /**
     * @return
     */
    protected abstract RunStats createRunStats();

    /**
     * @param incrementalTotals
     */
    protected abstract void logGlobalncrementalStats(RunStats incrementalTotals);

    /**
     * @param subdomain
     * @param stats
     */
    protected abstract void logIncrementalStatsBySubdomain(String subdomain, RunStats stats);

    /**
     * 
     * @param runstats
     * @param cumulativeTotals
     */
    protected abstract void logCumulativeSessionStats(Map<String,RunStats> runstats, RunStats cumulativeTotals);

}
