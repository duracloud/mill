/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.CredentialsRepoException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.notification.NotificationManager;
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
 * @author Daniel Bernstein
 * Date: Nov 5, 2013
 */
public abstract class LoopingTaskProducer<T extends Morsel> implements Runnable {
    private static Logger log = LoggerFactory.getLogger(LoopingTaskProducer.class);
    private TaskQueue taskQueue;
    private CredentialsRepo credentialsRepo;
    private StateManager<T> stateManager;
    private int maxTaskQueueSize;
    private StorageProviderFactory storageProviderFactory;
    private List<T> morselsToReload = new LinkedList<>();
    private Frequency frequency;
    private LocalTime startTime;
    private RunStats cumulativeTotals;
    private NotificationManager notificationManager;
    private LoopingTaskProducerConfigurationManager config;
    private Map<String, RunStats> runstats = new HashMap<>();

    public LoopingTaskProducer(CredentialsRepo credentialsRepo,
                               StorageProviderFactory storageProviderFactory,
                               TaskQueue taskQueue,
                               StateManager<T> state,
                               int maxTaskQueueSize,
                               Frequency frequency,
                               LocalTime startTime,
                               NotificationManager notificationManager,
                               LoopingTaskProducerConfigurationManager config) {

        this.credentialsRepo = credentialsRepo;
        this.storageProviderFactory = storageProviderFactory;
        this.taskQueue = taskQueue;
        this.stateManager = state;
        this.credentialsRepo = credentialsRepo;
        this.maxTaskQueueSize = maxTaskQueueSize;
        this.frequency = frequency;
        this.startTime = startTime;
        this.cumulativeTotals = createRunStats();
        this.notificationManager = notificationManager;
        this.config = config;
    }

    protected Frequency getFrequency() {
        return this.frequency;
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

    public void run() {

        Timer timer = new Timer();
        try {

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logSessionStats();
                }

            }, 5 * 60 * 1000, 5 * 60 * 1000);

            if (runLater()) {
                return;
            }

            log.info("Starting run...");
            Queue<T> morselQueue = loadMorselQueue();

            while (!morselQueue.isEmpty() && this.taskQueue.size() < maxTaskQueueSize) {
                T morsel = morselQueue.peek();

                if (morsel != null) {
                    //check that account is still active
                    //if not remove from list, but only if
                    //it has not yet been started.  If an account becomes
                    //inactive in the middle of processing a morsel,  then
                    //allow to finish even if it results in errors.
                    String account = morsel.getAccount();
                    try {
                        if (!this.credentialsRepo.isAccountActive(account)) {
                            if (morsel.getMarker() == null) {
                                log.info("account {} has become inactive.  Abandonning morsel {}.", account, morsel);
                                morselQueue.poll();
                                continue;
                            } else {
                                String message = MessageFormat
                                    .format("account {0} has become inactive in the middle of processing {1}. "
                                            + "  Allowing this morsel to continue but failure is likely.  "
                                            + "\nExpect items to appear in the dead letter queue shortly.",
                                            account, morsel);
                                log.warn(message);
                                sendEmail(getSimpleName() +
                                          " attempting into account after start of morsel processing.",
                                          message);
                            }
                        }
                    } catch (AccountCredentialsNotFoundException ex) {
                        String message =
                            MessageFormat.format("account {0} does not exist.  Abandonning morsel {1}.",
                                                 account, morsel);
                        log.warn(message);
                        sendEmail(getSimpleName() + " attempted to access into non-existent account", message);

                    }
                }
                nibble(morselQueue);
                persistMorsels(morselQueue, morselsToReload);

                if (morselQueue.isEmpty()) {
                    morselQueue = reloadMorselQueue();
                } else {
                    //break if nothing was removed from the queue
                    //if nothing was removed from the queue we can assume
                    //that the for whatever reason the morsel could not be processed
                    //at this time, so the process should wait for the next run.
                    if (morsel.equals(morselQueue.peek())) {
                        break;
                    }
                }

            }

            logSessionStats();

            if (morselQueue.isEmpty()) {
                scheduleNextRun();
                writeCompletionFile();
            }

            log.info("Session ended.");
        } catch (Exception ex) {
            log.error("failed to complete run on " + getSimpleName() + ": " + ex.getMessage(), ex);
            sendEmail( "failed to complete run on " + getSimpleName(),
                       ex.getClass().getCanonicalName() + ":" + ex.getMessage() );
        } finally {
            timer.cancel();
        }
    }

    private String getSimpleName() {
        return getClass().getSimpleName();
    }

    /**
     * Writes zero length file to the work directory to mark the completion of a run.
     */
    private void writeCompletionFile() {
        File completionFile = getCompletionFile();
        try {
            if (completionFile.createNewFile()) {
                log.info("successfully created completion marker file: {}",
                         completionFile.getAbsolutePath());
            } else {
                log.warn("completion marker file unexpectably exists already " +
                         "- something may be amiss: {}",
                         completionFile.getAbsolutePath());

            }
        } catch (IOException e) {
            log.error("Unable to create the completion file {}: {}",
                      completionFile.getAbsolutePath(),
                      e.getMessage());
        }
    }

    /**
     * Deletes the completion marker file if it exists.
     */
    private void deleteCompletionFileIfExists() {
        File completionFile = getCompletionFile();
        if (completionFile.exists()) {
            completionFile.delete();
        }
    }

    /**
     * @return
     */
    private File getCompletionFile() {
        return new File(this.config.getWorkDirectoryPath(),
                        getLoopingProducerTypePrefix() + "-producer-complete.txt");
    }

    private void resetIncrementalSessionStats() {
        synchronized (runstats) {
            for (String account : runstats.keySet()) {
                RunStats stats = runstats.get(account);
                stats.reset();
            }
        }
    }

    protected RunStats calculateStatTotals(RunStats currentTotals) {
        RunStats totals = createRunStats();
        totals.copyValuesFrom(currentTotals);

        synchronized (runstats) {
            for (String account : runstats.keySet()) {
                RunStats stats = runstats.get(account);
                totals.add(stats);
            }
            return totals;
        }
    }

    private void logSessionStats() {
        synchronized (runstats) {
            for (String account : runstats.keySet()) {
                RunStats stats = runstats.get(account);
                logIncrementalStatsByAccount(account, stats);
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
        Date nextRun = calculateNextRunDate(currentStartDate);
        this.stateManager.setNextRunStartDate(nextRun);
        this.stateManager.setCurrentRunStartDate(null);

        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("unable to get hostname:" + e.getMessage());
        }

        String subject = getClass().getSimpleName() + "'s run completed on " + hostname;
        StringBuilder builder = new StringBuilder();
        builder.append(subject + "\n");
        builder.append(this.cumulativeTotals.toString() + "\n");

        if (nextRun != null) {
            builder.append("Scheduling the next run for " + nextRun + "\n");
            log.info(subject + ": next run will start " + nextRun);
        }

        sendEmail(subject, builder.toString());
    }

    private Date calculateNextRunDate(Date previousDate) {
        Date nextRun;
        Calendar c = Calendar.getInstance();
        //if the current start date is null calculate based on present moment.
        if (previousDate == null) {
            //if a start time is defined
            if (this.startTime != null) {
                //calculate based on start time.
                c.set(Calendar.HOUR_OF_DAY, this.startTime.getHour());
                c.set(Calendar.MINUTE, this.startTime.getMinute());
                c.set(Calendar.SECOND, this.startTime.getSecond());

                //if the start time is before the present moment
                LocalTime now = LocalTime.now();
                if (this.startTime.isBefore(now)) {
                    // if less than 10 minutes has passed since the start time,
                    // allow the next run to occur now (don't push to tomorrow)
                    LocalTime startTimePlusTen = LocalTime.of(startTime.getHour(),
                                                              startTime.getMinute() + 10,
                                                              startTime.getSecond());
                    if (startTimePlusTen.isAfter(now)) {
                        log.info("Less than 10 minutes has passed since the scheduled start time, " +
                                 "allowing run to begin now.");
                    } else {
                        //move to the following day
                        log.info("The start time has passed for today. " +
                                 "Setting next run to occur tomorrow");
                        c.add(Calendar.DATE, 1);
                    }
                }
                nextRun = c.getTime();
            } else {
                //next run is now
                nextRun = new Date();
            }

        } else {  //otherwise calculate based on previous  date
            //add the frequency
            c.setTimeInMillis(previousDate.getTime());
            c.add(this.frequency.getTimeUnit(), this.frequency.getValue());
            nextRun = c.getTime();
        }

        //return null if the frequency is zero or less.
        if (this.frequency.getValue() <= 0) {
            nextRun = null;
        }

        return nextRun;
    }

    protected void sendEmail(String subject, String body) {
        this.notificationManager.sendEmail(subject, body);
    }

    /**
     * @return true if the process should wait until later
     */
    private boolean runLater() {
        boolean runLater = true;
        Date nextRun = this.stateManager.getNextRunStartDate();

        if (nextRun != null) {
            Date now = new Date();
            if (getFrequency().getValue() <= 0) {
                log.info("The frequency is set to {}: all scheduled runs will be cancelled.",
                         getFrequency());
                this.stateManager.setNextRunStartDate(null);
            } else if (now.after(nextRun)) {
                deleteCompletionFileIfExists();
                this.stateManager.setCurrentRunStartDate(now);
                this.stateManager.setNextRunStartDate(null);
                runLater = false;
                log.info("Time to start a new run: the next run was scheduled to run on {}. Let's roll.", nextRun);
            } else {
                log.info("It's not yet time start a new run: the next run is scheduled to run on {}.", nextRun);
            }
        } else {
            Date currentRunStartDate = this.stateManager.getCurrentRunStartDate();
            if (currentRunStartDate == null && getFrequency().getValue() <= 0) {
                log.info("The frequency is set to {}: no future runs will be scheduled.", getFrequency());
            } else {
                if (currentRunStartDate == null) {
                    Date startDate = calculateNextRunDate(null);

                    if (startDate.getTime() <= System.currentTimeMillis()) {
                        this.stateManager.setCurrentRunStartDate(startDate);
                        log.info("We're starting the first run on this machine");
                    } else {
                        this.stateManager.setNextRunStartDate(startDate);
                        log.info("We will start the first run on this machine at {}", startDate);
                        return true;
                    }
                } else {
                    log.info("We're continuing the current run which was started on {}", currentRunStartDate);
                }
                runLater = false;
            }
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
    private Queue<T> loadMorselQueue() {
        Queue<T> morselQueue = createQueue();

        //load morsels from state;
        Set<T> morsels = new LinkedHashSet<>(this.stateManager.getMorsels());

        morselQueue.addAll(morsels);

        if (morselQueue.isEmpty()) {
            loadMorselQueueFromSource(morselQueue);
        }

        return morselQueue;
    }

    /**
     * @return
     */
    protected Queue<T> createQueue() {
        return new LinkedList<T>();
    }

    private void persistMorsels(Queue<T> queue, List<T> morselsToReload) {
        LinkedHashSet<T> morsels = new LinkedHashSet<>();
        morsels.addAll(queue);
        morsels.addAll(morselsToReload);
        stateManager.setMorsels(morsels);
    }

    /**
     * @param morsel
     */
    protected void addToReloadList(T morsel) {
        log.info("adding morsel to reload list: {}", morsel);
        morselsToReload.add(morsel);
    }

    /**
     * @param account
     * @return
     */
    protected RunStats getStats(String account) {
        synchronized (runstats) {
            RunStats stats = this.runstats.get(account);
            if (stats == null) {
                this.runstats.put(account, stats = createRunStats());
            }
            return stats;
        }
    }

    protected StorageProvider getStorageProvider(String account,
                                                 String storeId) {
        StorageProviderCredentials creds;
        try {
            creds = credentialsRepo.getStorageProviderCredentials(account, storeId);
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
    protected abstract void loadMorselQueueFromSource(Queue<T> morselQueue);

    /**
     * @param queue
     */
    protected abstract void nibble(Queue<T> queue);

    /**
     * @return
     */
    protected abstract RunStats createRunStats();

    /**
     * @param incrementalTotals
     */
    protected abstract void logGlobalncrementalStats(RunStats incrementalTotals);

    /**
     * @param account
     * @param stats
     */
    protected abstract void logIncrementalStatsByAccount(String account, RunStats stats);

    /**
     * @param runstats
     * @param cumulativeTotals
     */
    protected abstract void logCumulativeSessionStats(Map<String, RunStats> runstats, RunStats cumulativeTotals);

    /**
     * A short looping producer type identifier for use with state files.
     *
     * @return
     */
    protected abstract String getLoopingProducerTypePrefix();

    /**
     * @param message
     * @param ex
     */
    protected void sendEmail(String message, Exception ex) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement ste : stackTrace) {
            builder.append(ste.toString() + "\n");
        }
        sendEmail(message, builder.toString());
    }

}
