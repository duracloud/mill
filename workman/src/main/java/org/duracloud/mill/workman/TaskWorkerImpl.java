/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.common.queue.task.Task;
import org.duracloud.common.queue.TaskNotFoundException;
import org.duracloud.common.queue.TaskQueue;
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
    private static final Timer TIMER;

    private TaskProcessorFactory processorFactory;
    private TaskQueue queue;
    private TaskQueue deadLetterQueue;
    private boolean done = false;
    private boolean started = false;
    private TimerTask currentTimerTask;
    private Task task;
    private boolean initialized = false;

    static {
        TIMER = new Timer();
    }
    /**
     * @param task
     * @param processorFactory
     * @param queue
     */
    public TaskWorkerImpl(Task task, TaskProcessorFactory processorFactory, TaskQueue queue, TaskQueue deadLetterQueue) {
        if (task == null)
            throw new IllegalArgumentException("task must be non-null");
        if (queue == null)
            throw new IllegalArgumentException("queue must be non-null");
        if (deadLetterQueue == null)
            throw new IllegalArgumentException("deadLetterQueue must be non-null");

        if (processorFactory == null)
            throw new IllegalArgumentException("processor must be non-null");
        this.task = task;
        this.processorFactory = processorFactory;
        this.queue = queue;
        this.deadLetterQueue = deadLetterQueue;
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
        TIMER.schedule(currentTimerTask, executionTime);
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
        long startTime = System.currentTimeMillis();

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
            
            log.info("completed task:  task_type={} task_class{} attempts={} result={} elapsed_time={}",
                     task.getType(),
                     task.getClass().getSimpleName(),
                     task.getAttempts(),
                     "success",
                     System.currentTimeMillis()-startTime);

        }  catch (Exception e) {
            int attempts = task.getAttempts();

            log.error("failed to complete:  task_type=" + task.getType()
                    + " attempts=" + attempts + " result=failure elapsed_time="
                    + (System.currentTimeMillis() - startTime) + " message=\""
                    + e.getMessage() + "\"", e);
            
            if(attempts < TaskWorker.MAX_ATTEMPTS){
                this.queue.requeue(task);
            }else{
                task.addProperty("error", e.getMessage());
                sendToDeadLetterQueue(task);
            }
            
        } finally {
            done = true;
            if (currentTimerTask != null) {
                currentTimerTask.cancel();
            }
            
            log.debug("task worker finished {}", this.task);
        }
    }

    /**
     * @param task
     */
    private void sendToDeadLetterQueue(Task task) {
        log.info("putting {} on dead letter queue", task);

        try {
            log.debug("deleting {} from {}", task, this.queue);
            this.queue.deleteTask(task);
        } catch (TaskNotFoundException e) {
            log.error("Error deleting "+task+". This should never happen: "+ e.getMessage(), e);
        }

        this.deadLetterQueue.put(task);
        log.info("sent {} to dead-letter-queue={}", task, deadLetterQueue.getName());
        
    }
}
