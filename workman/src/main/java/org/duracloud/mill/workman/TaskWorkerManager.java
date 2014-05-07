/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerManager {
    private Logger log = LoggerFactory.getLogger(TaskWorkerManager.class);

    public static final int DEFAULT_MAX_WORKERS = 5;
    public static final String MAX_WORKER_PROPERTY_KEY = "duracloud.maxWorkers";
    public static final String MIN_WAIT_BEFORE_TAKE_KEY = "duracloud.minWaitBeforeTake";
    public static final long DEFAULT_MIN_WAIT_BEFORE_TAKE = 15*1000;
    private static final long DEFAULT_MAX_WAIT_BEFORE_TAKE = 8*60*1000;
    private Long defaultMinWaitTime = DEFAULT_MIN_WAIT_BEFORE_TAKE;
    private TaskWorkerFactory factory;
    private ThreadPoolExecutor executor;
    private boolean stop = false;
    private Timer timer = new Timer();
    private List<TaskQueueExecutor> taskQueueExecutors;
    private TaskQueue deadLetterQueue = null;
    private List<TaskQueue> taskQueues;

    public TaskWorkerManager(List<TaskQueue> taskQueues,
                             TaskQueue deadLetterQueue,
                             TaskWorkerFactory factory) {
        if (factory == null){
            throw new IllegalArgumentException("factory must be non-null");
        }
 
        if (taskQueues == null || taskQueues.isEmpty()){
            throw new IllegalArgumentException("at least one taskQueue must be specified in the taskQueues list.");
        }

        if (deadLetterQueue == null){
            throw new IllegalArgumentException("deadLetterQueue must be non-null");
        }

        this.factory = factory;
        this.taskQueues = taskQueues;
        int size = taskQueues.size();
        this.taskQueueExecutors = new ArrayList<TaskQueueExecutor>(size);
        for(int i = 0; i < size; i++){
            boolean lowestPriority =  i == size - 1; //last task queue in the list is lowest priority
            long minWait = this.defaultMinWaitTime, 
                 maxWait = DEFAULT_MAX_WAIT_BEFORE_TAKE;
            
            if(!lowestPriority){
                minWait = 1000;
                maxWait = 30*1000;
            }
            
            this.taskQueueExecutors.add(new TaskQueueExecutor(
                    taskQueues.get(i), minWait, maxWait));
       }
        this.deadLetterQueue = deadLetterQueue;

    }

    public void init() {
        
        this.defaultMinWaitTime = new Long(System.getProperty(MIN_WAIT_BEFORE_TAKE_KEY,
                DEFAULT_MIN_WAIT_BEFORE_TAKE+""));
        
        Integer maxThreadCount = new Integer(System.getProperty(
                MAX_WORKER_PROPERTY_KEY, String.valueOf(DEFAULT_MAX_WORKERS)));

        //With a bound pool and unbounded queue, rejection should never occur.
        this.executor = new ThreadPoolExecutor(maxThreadCount, 
                                               maxThreadCount,
                                               0L, TimeUnit.MILLISECONDS,
                                               new LinkedBlockingQueue<Runnable>());
                
        this.executor
                .setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
       
        new Thread(new Runnable() {
            @Override
            public void run() {
                runManager();
            }
        }).start();
        
        

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                
                List<String> queueStats = new LinkedList<String>();
                    
                for(TaskQueue queue: taskQueues){
                    queueStats.add(formatQueueStat(queue));
                }
                
                queueStats.add(formatQueueStat(deadLetterQueue));
                
                
                
                log.info(
                        "Status: max_workers={} running_workers={}" +
                            " completed_workers={}" +
                            queueStats,
                        new Object[] { getMaxWorkers(),
                            executor.getActiveCount(),
                            executor.getCompletedTaskCount(),
                            StringUtils.join(queueStats, " ")
                        });
            }

            private String formatQueueStat(TaskQueue queue) {
                 return queue.getName() + "_q_size=" + queue.size();
            }

        }, new Date(), 5 * 60 * 1000);
    }
    
    private void runManager() {
        
        while(!stop){
            try {
                if(isManagerTooBusy()){
                    log.debug("manager is too busy, sleeping for 1 sec");
                    //wait only a moment before trying again since 
                    //the worker pool is expected to move relatively quickly.
                    sleep(1000);
                }else{
                    //loop through queues attempting to 
                    //execute a task off the highest priority queue
                    boolean executedOne = false;
                    for(TaskQueueExecutor taskQueueExecutor : this.taskQueueExecutors){
                        if(taskQueueExecutor.execute()){
                            executedOne = true;
                           break; 
                        }
                    }
                    
                    if(!executedOne){
                        sleep(defaultMinWaitTime);
                    }
                }
            }catch(Exception ex){
                log.error(
                        "unexpected failure in outer run manager while loop: " 
                        + ex.getMessage() + ". Ignoring...", ex);
            }

        }
    }

    private  class TaskQueueExecutor {
        private TaskQueue taskQueue;
        private long currentWaitBeforeTaskMs;
        private Date nextAttempt = null;
        private long minWaitTime;
        private long maxWaitTime;
        
        public TaskQueueExecutor(TaskQueue taskQueue, long minWaitTime, long maxWaitTime){
            this.taskQueue = taskQueue;
            this.minWaitTime = minWaitTime;
            this.maxWaitTime = maxWaitTime;
            this.currentWaitBeforeTaskMs = minWaitTime;
        }

        /**
         * @param taskQueue
         * @return true if a task was executed.
         */
        public boolean execute() {
            if(nextAttempt != null && 
                    nextAttempt.getTime() > System.currentTimeMillis()){
                return false;
            }
            
            try{
                TaskWorker worker = factory.create(taskQueue.take(), taskQueue);
                executor.execute(worker);
                nextAttempt = null;
                currentWaitBeforeTaskMs = minWaitTime;
                return true;
            }catch(TimeoutException e){
                log.debug("Timeout: {} queue is empty:  message={}", taskQueue.getName(), e.getMessage());
                nextAttempt = new Date(
                        System.currentTimeMillis()
                                + currentWaitBeforeTaskMs);
                currentWaitBeforeTaskMs = 
                        Math.min(currentWaitBeforeTaskMs*2,maxWaitTime);
                return false;
            }
        }
    
    }

    /**
     * @return
     */
    private boolean isManagerTooBusy() {
        int active = this.executor.getActiveCount();
        int maxPoolSize = this.executor.getMaximumPoolSize();
        int queueSize =  this.executor.getQueue().size();

        boolean tooBusy =  active + queueSize >= maxPoolSize;
        
        if(tooBusy){
            log.info(
                    "manager is too busy: active worker count = {}; workers awaiting execution (thread pool queue size) =  {}",
                    active, queueSize);

        }
        return tooBusy;
    }


    public int getMaxWorkers() {
        return executor.getMaximumPoolSize();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        stop = true;
        timer.cancel();
        executor.shutdown();
        log.info("terminating...waiting for threads to complete processing...");
        while (!executor.isTerminated()) {
            sleep(1000);
        }
        log.info("terminated");
    }
}
