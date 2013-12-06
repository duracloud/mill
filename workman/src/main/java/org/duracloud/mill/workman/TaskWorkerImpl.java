/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * It is responsible for executing a single <code>TaskProcessor</code>. During
 * execution it makes sure to extend the visibility timeout of the item on the
 * queue. the <code>TaskProcessor</code>.
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerImpl implements TaskWorker {
    private static Logger log = LoggerFactory.getLogger(TaskWorkerImpl.class);
    private static Timer timer = new Timer();

    private TaskProcessorFactory processorFactory;
    private TaskQueue queue;
    private boolean done = false;
    private boolean started = false;
    private TimerTask currentTimerTask;
    private Task task;
    private boolean initialized = false;

    /**
     * @param task
     * @param processorFactory
     * @param queue
     */
    public TaskWorkerImpl(Task task, TaskProcessorFactory processorFactory, TaskQueue queue) {
        if (task == null)
            throw new IllegalArgumentException("task must be non-null");
        if (queue == null)
            throw new IllegalArgumentException("queue must be non-null");
        if (processorFactory == null)
            throw new IllegalArgumentException("processor must be non-null");
        this.task = task;
        this.processorFactory = processorFactory;
        this.queue = queue;
        log.debug("new worker created {}", this);

    }

    private void scheduleVisibilityTimeoutExtender(final Task task,
                                                   Date timeFrom,
                                                   final Integer visibilityTimeout) {
        // schedule task to run two seconds before the expiration of the task
        // lest the timer task execute late for any reason.
        Date executionTime =
            new Date(timeFrom.getTime() + ((long)(visibilityTimeout*1000*0.5)));
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!done) {
                    try {
                        log.debug("extending timeout of {} {} seconds",
                                task, visibilityTimeout);
                        queue.extendVisibilityTimeout(task);
                        log.debug("timeout extended for {}", task);
                        // reschedule task
                        scheduleVisibilityTimeoutExtender(task, new Date(),
                                visibilityTimeout);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        };

        this.currentTimerTask = timerTask;
        timer.schedule(currentTimerTask, executionTime);
    }
    
    /**
     * This method be called before run since it is possible that there may be significant
     * delay between when the TaskWorker is initialized and when it is executed. 
     */
    protected void init(){
        log.debug("taskworker {} initializing...", this);
        
        scheduleVisibilityTimeoutExtender(task, new Date(),
                task.getVisibilityTimeout());

        log.debug("taskworker {} initialized", this);
        initialized  = true;

    }

    @Override
    public void run() {
        log.debug("taskworker run starting...", this);
        
        if(!initialized) {
            String error = "The taskworker must be initialized before it can be run";
            log.error(error);
            throw new RuntimeException(error);
        }
        
        if (done || started) {
            log.warn(
                    "task worker {} can only be run once:  started={}, done={}. Ignoring...",
                    new Object[] { this, started, done });
            return;
        }
        
        started = true;

        try {
            log.debug("{} dequeued {}", this, this.task);
            TaskProcessor processor = this.processorFactory.create(task);
            processor.execute();
            this.queue.deleteTask(task);
        }  catch (TaskExecutionFailedException e) {
            log.error("failed to complete task execution: " + e.getMessage(), e);
            // TODO re-queue at this point or send to error queue?
        } catch (Exception e) {
            log.error("unexpected error: " + e.getMessage(), e);
            e.printStackTrace();
        } finally {
            done = true;
            if (currentTimerTask != null) {
                currentTimerTask.cancel();
            }
        }
    }
}
