/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.TimeoutException;
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
    public static final long DEFAULT_MIN_WAIT_BEFORE_TAKE = 60*1000;
    private static final long DEFAULT_MAX_WAIT_BEFORE_TAKE = 8*60*1000;
    private long minWaitTime = DEFAULT_MIN_WAIT_BEFORE_TAKE;
    private long maxWaitTime = DEFAULT_MAX_WAIT_BEFORE_TAKE;

    private TaskWorkerFactory factory;
    private ThreadPoolExecutor executor;
    private boolean stop = false;
    private Timer timer = new Timer();
    private TaskQueue lowPriorityQueue = null;
    private TaskQueue highPriorityQueue = null;
    private TaskQueue deadLetterQueue = null;

    public TaskWorkerManager(TaskQueue lowPriorityQueue,
                             TaskQueue highPriorityQueue,
                             TaskQueue deadLetterQueue,
                             TaskWorkerFactory factory) {
        if (factory == null){
            throw new IllegalArgumentException("factory must be non-null");
        }
 
        if (lowPriorityQueue == null){
            throw new IllegalArgumentException("lowPriorityQueue must be non-null");
        }

        if (highPriorityQueue == null){
            throw new IllegalArgumentException("highPriorityQueue must be non-null");
        }

        if (deadLetterQueue == null){
            throw new IllegalArgumentException("deadLetterQueue must be non-null");
        }

        this.factory = factory;
        this.lowPriorityQueue = lowPriorityQueue;
        this.highPriorityQueue = highPriorityQueue;
        this.deadLetterQueue = deadLetterQueue;

    }

    public void init() {
        
        this.minWaitTime = new Long(System.getProperty(MIN_WAIT_BEFORE_TAKE_KEY,
                DEFAULT_MIN_WAIT_BEFORE_TAKE+""));
        
        Integer maxThreadCount = new Integer(System.getProperty(
                MAX_WORKER_PROPERTY_KEY, String.valueOf(DEFAULT_MAX_WORKERS)));

        //With a bound pool and unbounded queue, rejection should never occur.
        this.executor = new ThreadPoolExecutor(maxThreadCount, 
                                               maxThreadCount,
                                               0L, TimeUnit.MILLISECONDS,
                                               new LinkedBlockingQueue<Runnable>());
                
        new Thread(new Runnable() {
            @Override
            public void run() {
                runManager();
            }
        }).start();
        
        

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info(
                        "Status: max_workers={} running_workers={}" +
                            " completed_workers={} dup_lp_qsize={}" +
                            " dup_hp_qsize={} dup_dl_qsize={}",
                        new Object[] { getMaxWorkers(),
                            executor.getActiveCount(),
                            executor.getCompletedTaskCount(),
                            lowPriorityQueue.size(),
                            highPriorityQueue.size(),
                            deadLetterQueue.size()
                        });
            }

        }, new Date(), 5 * 60 * 1000);
    }
    
    private void runManager() {
        
        long currentWaitBeforeTaskMs = minWaitTime;
        //high priority tasks should wait only a short time
        //and should not wait more than say a minute at the most
        //to prevent perceived delays for the duradmin and durastore
        //api users.
        long currentWaitBeforeHighPriortyTaskMs = 1000;
        long maxHighPriorityWaitTime = 30*1000;
        
        Date nextHighPriorityAttempt = null;
        
        while(!stop){
            int active = this.executor.getActiveCount();
            int maxPoolSize = this.executor.getMaximumPoolSize();
            int queueSize =  this.executor.getQueue().size();
            log.debug(
                    "active worker count = {}; workers awaiting execution (thread pool queue size) =  {}",
                    active, queueSize);
            
            if(active < maxPoolSize && queueSize < maxPoolSize){
                //pull up to 10 items off queue
                //(that is when takeMany() is implemented).

                //always try high priority queue first: if nothing in it set time for next 
                //attempt using exponential back-off to ensure high priority queue is not
                //called on every round
                if(nextHighPriorityAttempt == null || 
                          nextHighPriorityAttempt.getTime() < System.currentTimeMillis()){
                    try{
                        executeTask(highPriorityQueue.take(), highPriorityQueue);
                        //reset exponential backoff
                        nextHighPriorityAttempt = null;
                        currentWaitBeforeHighPriortyTaskMs = minWaitTime;
                        //skip low priority take
                        continue;
                    }catch(TimeoutException e){
                        log.debug("high priority queue is empty - trying low priority queue");
                        nextHighPriorityAttempt = new Date(
                                System.currentTimeMillis()
                                        + currentWaitBeforeHighPriortyTaskMs);
                        currentWaitBeforeHighPriortyTaskMs = 
                                Math.min(currentWaitBeforeHighPriortyTaskMs*2,maxHighPriorityWaitTime);
                    }
                }

                try {
                    executeTask(lowPriorityQueue.take(), lowPriorityQueue);
                    currentWaitBeforeTaskMs = minWaitTime;
                } catch (TimeoutException e) {
                    currentWaitBeforeTaskMs = 
                            Math.min(currentWaitBeforeTaskMs, maxWaitTime);
                    log.warn(
                            "timeout while taking tasks: no tasks currently available " +
                            "for the take on {}, waiting {} ms...",
                            lowPriorityQueue, currentWaitBeforeTaskMs);
                    sleep(currentWaitBeforeTaskMs);
                    currentWaitBeforeTaskMs = 
                            Math.min(currentWaitBeforeTaskMs*2, maxWaitTime);

                }
                
            }else{
                //wait only a moment before trying again since 
                //the worker pool is expected to move relatively quickly.
                sleep(1000);
            }
        }
    }


    /**
     * @param take
     * @param queue
     */
    private void executeTask(Task task, TaskQueue queue) {
        TaskWorker worker = factory.create(task, queue);
        this.executor.execute(worker);
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
