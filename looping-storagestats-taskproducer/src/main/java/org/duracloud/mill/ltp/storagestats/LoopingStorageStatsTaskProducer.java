/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.storagestats;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.storageprovider.StorageStatsTask;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducer;
import org.duracloud.mill.ltp.PathFilterManager;
import org.duracloud.mill.ltp.RunStats;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein Date: Apr 28, 2014
 */
public class LoopingStorageStatsTaskProducer
        extends LoopingTaskProducer<StorageStatsMorsel> {
    private static Logger log = LoggerFactory
            .getLogger(LoopingStorageStatsTaskProducer.class);
    private PathFilterManager exclusionManager;
    private int waitTimeInMsBeforeQueueSizeCheck = 10000;

    public LoopingStorageStatsTaskProducer(CredentialsRepo credentialsRepo,
                                           StorageProviderFactory storageProviderFactory,
                                           TaskQueue queue,
                                           StateManager<StorageStatsMorsel> state,
                                           int maxTaskQueueSize,
                                           Frequency frequency,
                                           NotificationManager notificationManager,
                                           PathFilterManager exclusionManager,
                                           LoopingStorageStatsTaskProducerConfigurationManager config) {
        super(credentialsRepo,
              storageProviderFactory,
              queue,
              state,
              maxTaskQueueSize,
              frequency,
              notificationManager,
              config);
        this.exclusionManager = exclusionManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.ltp.LoopingTaskProducer#loadMorselQueueFromSource(java
     * .util.Queue)
     */
    @Override
    protected void
              loadMorselQueueFromSource(Queue<StorageStatsMorsel> morselQueue) {
        // generate set of morsels based on duplication policy
        try {
            for (String account : getAccountsList()) {
                String accountPath = "/" + account;

                log.debug("loading {}", account);

                if (exclusionManager.isExcluded(accountPath)) {
                    continue;
                }

                AccountCredentials accountCreds = getCredentialsRepo()
                        .getAccountCredentials(account);
                for (StorageProviderCredentials cred : accountCreds
                        .getProviderCredentials()) {
                    String storePath = accountPath + "/" + cred.getProviderId();
                    if (exclusionManager.isExcluded(storePath)) {
                        continue;
                    }

                    StorageProvider store = getStorageProvider(cred);

                    Iterator<String> spaces = store.getSpaces();
                    while (spaces.hasNext()) {
                        String spaceId = spaces.next();
                        String spacePath = storePath + "/" + spaceId;
                        if (!exclusionManager.isExcluded(spacePath)) {
                            morselQueue.add(new StorageStatsMorsel(account,
                                                                   cred.getProviderId(),
                                                                   cred.getProviderType()
                                                                           .name(),
                                                                   spaceId));
                        }
                    }

                    log.info("loaded {} into morsel queue.", account);

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
        return getCredentialsRepo().getActiveAccounts();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.ltp.LoopingTaskProducer#nibble(org.duracloud.mill.ltp.
     * Morsel)
     */
    @Override
    protected void nibble(Queue<StorageStatsMorsel> queue) {
        StorageStatsMorsel morsel = queue.peek();
        String storeId = morsel.getStoreId();

        StorageProvider store = getStorageProvider(morsel.getAccount(),
                                                   storeId);

        int maxTaskQueueSize = getMaxTaskQueueSize();
        int taskQueueSize = getTaskQueue().size();

        if (taskQueueSize >= maxTaskQueueSize) {
            log.info("Task queue size ({}) has reached or exceeded max size ({}).",
                     taskQueueSize,
                     maxTaskQueueSize);
        } else {

            addTask(morsel, store);
            queue.remove(morsel);
        }
    }

    /**
     * @param ms
     */
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param morsel
     * @param store
     * @param biteSize
     * @return
     */
    private void addTask(StorageStatsMorsel morsel, StorageProvider store) {
        String account = morsel.getAccount();
        String storeId = morsel.getStoreId();
        String spaceId = morsel.getSpaceId();
        StorageStatsTask storageStatsTask = new StorageStatsTask();
        storageStatsTask.setAccount(account);
        storageStatsTask.setSpaceId(spaceId);
        storageStatsTask.setStoreId(storeId);
        Task task = storageStatsTask.writeTask();

        getTaskQueue().put(task);

        ((StorageStatsRunStats) getStats(account)).add(1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.ltp.LoopingTaskProducer#logIncrementalStatsBySubdomain
     * (java.lang.String, org.duracloud.mill.ltp.RunStats)
     */
    @Override
    protected void logIncrementalStatsByAccount(String account,
                                                RunStats stats) {
        log.info("Session stats by account (incremental): account={} tasksAdded={}",
                 account,
                 ((StorageStatsRunStats) stats).getAdded());

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.ltp.LoopingTaskProducer#logCumulativeSessionStats()
     */
    @Override
    protected void logCumulativeSessionStats(Map<String, RunStats> runstats,
                                             RunStats cumulativeTotals) {
        log.info("session stats (global cumulative): domains={} tasksAdded={}",
                 runstats.keySet().size(),
                 ((StorageStatsRunStats) cumulativeTotals).getAdded());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.ltp.LoopingTaskProducer#logGlobalncrementalStats(org.
     * duracloud.mill.ltp.RunStats)
     */
    @Override
    protected void logGlobalncrementalStats(RunStats incrementalTotals) {
        log.info("Session stats (global incremental): tasksAdded={}",
                 ((StorageStatsRunStats) incrementalTotals).getAdded());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#createRunStats()
     */
    @Override
    protected RunStats createRunStats() {
        return new StorageStatsRunStats();
    }

    public void setWaitTimeInMsBeforeQueueSizeCheck(int ms) {
        this.waitTimeInMsBeforeQueueSizeCheck = ms;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.ltp.LoopingTaskProducer#getLoopingProducerTypePrefix()
     */
    @Override
    protected String getLoopingProducerTypePrefix() {
        return "storagestats";
    }
}
