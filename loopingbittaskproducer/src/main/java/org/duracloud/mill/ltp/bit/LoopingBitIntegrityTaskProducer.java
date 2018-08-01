/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.mill.bit.BitIntegrityCheckReportTask;
import org.duracloud.mill.bit.BitIntegrityCheckTask;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.db.model.BitIntegrityReport;
import org.duracloud.mill.db.repo.JpaBitIntegrityReportRepo;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducer;
import org.duracloud.mill.ltp.PathFilterManager;
import org.duracloud.mill.ltp.RunStats;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.reportdata.bitintegrity.BitIntegrityReportResult;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: Apr 28, 2014
 */
public class LoopingBitIntegrityTaskProducer extends LoopingTaskProducer<BitIntegrityMorsel> {
    private static Logger log = LoggerFactory.getLogger(LoopingBitIntegrityTaskProducer.class);
    private PathFilterManager exclusionManager;
    private int waitTimeInMsBeforeQueueSizeCheck = 10000;
    private TaskQueue bitReportTaskQueue;
    private JpaBitIntegrityReportRepo bitReportRepo;
    private int waitBetweenRetriesMs = 5000;

    public LoopingBitIntegrityTaskProducer(CredentialsRepo credentialsRepo,
                                           JpaBitIntegrityReportRepo bitReportRepo,
                                           StorageProviderFactory storageProviderFactory,
                                           TaskQueue bitTaskQueue,
                                           TaskQueue bitReportTaskQueue,
                                           StateManager<BitIntegrityMorsel> state,
                                           int maxTaskQueueSize,
                                           Frequency frequency,
                                           NotificationManager notificationManager,
                                           PathFilterManager exclusionManager,
                                           LoopingBitTaskProducerConfigurationManager config) {
        super(credentialsRepo,
              storageProviderFactory,
              bitTaskQueue,
              state,
              maxTaskQueueSize,
              frequency,
              null,
              notificationManager,
              config);
        this.exclusionManager = exclusionManager;
        this.bitReportTaskQueue = bitReportTaskQueue;
        this.bitReportRepo = bitReportRepo;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#loadMorselQueueFromSource(java.util.Queue)
     */
    @Override
    protected void loadMorselQueueFromSource(Queue<BitIntegrityMorsel> morselQueue) {
        //generate set of morsels based on duplication policy
        try {
            for (String account : getAccountsList()) {
                String accountPath = "/" + account;

                log.debug("loading {}", account);

                if (exclusionManager.isExcluded(accountPath)) {
                    continue;
                }

                AccountCredentials accountCreds = getCredentialsRepo().getAccountCredentials(account);
                for (StorageProviderCredentials cred : accountCreds.getProviderCredentials()) {
                    String storeId = cred.getProviderId();
                    String storePath = accountPath + "/" + storeId;
                    if (exclusionManager.isExcluded(storePath)) {
                        continue;
                    }

                    StorageProvider store = getStorageProvider(cred);

                    Iterator<String> spaces = store.getSpaces();
                    while (spaces.hasNext()) {
                        String spaceId = spaces.next();
                        String spacePath = storePath + "/" + spaceId;
                        if (!exclusionManager.isExcluded(spacePath)) {

                            //check if most recent
                            BitIntegrityReport report = bitReportRepo
                                .findFirstByAccountAndStoreIdAndSpaceIdOrderByCompletionDateDesc(account,
                                                                                                 storeId,
                                                                                                 spaceId);
                            if (report != null) {
                                //skip if last report was a success that completed less than 60 days ago
                                long oneDayInMs = 24 * 60 * 60 * 1000;
                                if (report.getCompletionDate()
                                          .after(new Date(System.currentTimeMillis() - (60 * oneDayInMs)))
                                    && report.getResult().equals(BitIntegrityReportResult.SUCCESS)) {
                                    continue;
                                }
                            }

                            morselQueue.add(new BitIntegrityMorsel(account,
                                                                   cred.getProviderId(),
                                                                   cred.getProviderType().name(),
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

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#nibble(org.duracloud.mill.ltp.Morsel)
     */
    @Override
    protected void nibble(Queue<BitIntegrityMorsel> queue) {
        BitIntegrityMorsel morsel = queue.peek();
        String storeId = morsel.getStoreId();
        String account = morsel.getAccount();
        StorageProvider store;

        try {
            store = getStorageProvider(account, storeId);
        } catch (Exception ex) {
            if (morsel.getMarker() != null) {
                throw new DuraCloudRuntimeException(
                    "Failed to get storage provider for " + morsel + ". Morsel has already been nibbled. " +
                    "Likely cause:  a storage provider was removed in the middle of processing the morsel. " +
                    "Further investigation and clean up recommended before restarting the run." +
                    "In most cases you should be able to remove the state file and restart the run.", ex);
            } else {
                //remove morsel.
                queue.poll();
                String message =
                    MessageFormat.format("Failed to get storage provider for {0}. Likely cause: A storage " +
                                         "provider was removed after the bit integrity run was started. Since no " +
                                         "tasks have been added yet for this morsel, we will simply skip it. " +
                                         "No further action required.", morsel);
                log.warn(message, morsel);
                sendEmail("Failed to get storage provider for " + morsel, message);
                return;
            }
        }

        int maxTaskQueueSize = getMaxTaskQueueSize();
        int taskQueueSize = getTaskQueue().size();

        while (taskQueueSize < maxTaskQueueSize) {

            if (taskQueueSize >= maxTaskQueueSize) {
                log.info("Task queue size ({}) has reached or exceeded max size ({}).",
                         taskQueueSize, maxTaskQueueSize);
            } else {

                if (addTasks(morsel, store, 1000)) {
                    log.info("All bit integrity tasks that could be created were created for account={}, storeId={}, " +
                             "spaceId={}. getTaskQueue().size = {}",
                             morsel.getAccount(), storeId, morsel.getSpaceId(), getTaskQueue().size());
                    log.info("{} completely nibbled.", morsel);

                    // check if queue is empty after waiting a few moments: It is possible that AWS will not have
                    // registered a new task that was added in the previous step (or even is pending). I observed
                    // this problem while debugging this code.
                    // If I put the breakpoint on the if statement and I ran the task producer against a space with a
                    // single content item then size would be reported as 0.  However if I put the breakpoint a line
                    // above on "long size = ..." then the size variable was evaluating to 1. At 5 seconds, I was
                    // still seeing the inconsistency.  At 10 seconds the matter seems to be resolved.
                    // Makes me a little nervous.  --dbernstein
                    log.debug("delay before checking the queue size in ms: {}", waitTimeInMsBeforeQueueSizeCheck);
                    sleep(waitTimeInMsBeforeQueueSizeCheck);
                    long size = getTaskQueue().sizeIncludingInvisibleAndDelayed();
                    if (size == 0) {
                        addReportTaskProcessorTask(queue.poll());
                    } else {
                        log.info("{} (queue) is not empty: {} items remain to be processed before " +
                                 "creating report generation task.", getTaskQueue().getName(), size);
                    }

                    break;

                } else {
                    log.info("morsel nibbled down: {}", morsel);
                }
            }

            taskQueueSize = getTaskQueue().size();
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
     *
     */
    private void addReportTaskProcessorTask(BitIntegrityMorsel morsel) {
        BitIntegrityCheckReportTask task = new BitIntegrityCheckReportTask();
        task.setAccount(morsel.getAccount());
        task.setStoreId(morsel.getStoreId());
        task.setSpaceId(morsel.getSpaceId());
        Task t = task.writeTask();
        this.bitReportTaskQueue.put(t);
        log.info("added report task {} to {}", t, this.bitReportTaskQueue);
    }

    /**
     * @param morsel
     * @param store
     * @param biteSize
     * @return
     */
    private boolean addTasks(BitIntegrityMorsel morsel,
                             final StorageProvider store,
                             final int biteSize) {
        final String account = morsel.getAccount();
        final String storeId = morsel.getStoreId();
        final String spaceId = morsel.getSpaceId();
        final String marker = morsel.getMarker();

        //load in next maxContentIdsToAdd or however many remain
        List<String> contentIds = null;

        try {

            contentIds = (List<String>) new Retrier(3, waitBetweenRetriesMs, 2).execute(new Retriable() {
                /* (non-Javadoc)
                 * @see org.duracloud.common.retry.Retriable#retry()
                 */
                @Override
                public Object retry() throws Exception {
                    return store.getSpaceContentsChunked(spaceId, null, biteSize, marker);
                }
            });

            int added = addToTaskQueue(account, storeId, spaceId, contentIds);

            ((BitIntegrityRunStats) getStats(account)).add(added);
            //if no tasks were added, it means that all contentIds in this morsel
            //have been touched in this run.
            if (added == 0) {
                return true;
            } else {
                String newMarker = contentIds.get(contentIds.size() - 1);
                morsel.setMarker(newMarker);
                return false;
            }

        } catch (Exception ex) {
            String message = MessageFormat.format("Bit integrity producer failure on  " +
                                                  "subdomain={0}, spaceId={1}, storeId={2} due to: {3}",
                                                  account, spaceId, storeId, ex.getMessage());
            log.error(message, ex);

            sendEmail(message, ex);
            return true;
        }
    }

    /**
     * @param account
     * @param storeId
     * @param contentIds
     * @return
     */
    private int addToTaskQueue(String account,
                               String storeId,
                               String spaceId,
                               List<String> contentIds) {
        Set<Task> tasks = new HashSet<>();
        int addedCount = 0;

        for (String contentId : contentIds) {
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
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logIncrementalStatsBySubdomain(java.lang.String, org.duracloud
     * .mill.ltp.RunStats)
     */
    @Override
    protected void logIncrementalStatsByAccount(String account, RunStats stats) {
        log.info("Session stats by account (incremental): account={} tasksAdded={}",
                 account, ((BitIntegrityRunStats) stats).getAdded());

    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logCumulativeSessionStats()
     */
    @Override
    protected void logCumulativeSessionStats(Map<String, RunStats> runstats, RunStats cumulativeTotals) {
        log.info("session stats (global cumulative): domains={} tasksAdded={}",
                 runstats.keySet().size(), ((BitIntegrityRunStats) cumulativeTotals).getAdded());
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#logGlobalncrementalStats(org.duracloud.mill.ltp.RunStats)
     */
    @Override
    protected void logGlobalncrementalStats(RunStats incrementalTotals) {
        log.info("Session stats (global incremental): tasksAdded={}",
                 ((BitIntegrityRunStats) incrementalTotals).getAdded());
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#createRunStats()
     */
    @Override
    protected RunStats createRunStats() {
        return new BitIntegrityRunStats();
    }

    public void setWaitTimeInMsBeforeQueueSizeCheck(int ms) {
        this.waitTimeInMsBeforeQueueSizeCheck = ms;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.ltp.LoopingTaskProducer#getLoopingProducerTypePrefix()
     */
    @Override
    protected String getLoopingProducerTypePrefix() {
        return "bit";
    }

    /**
     * Modify the wait between retries
     *
     * @param waitBetweenRetriesMs
     */
    public void setWaitBetweenRetriesMs(int waitBetweenRetriesMs) {
        this.waitBetweenRetriesMs = waitBetweenRetriesMs;
    }

}
