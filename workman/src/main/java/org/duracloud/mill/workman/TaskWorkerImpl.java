/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.duracloud.common.queue.TaskNotFoundException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It is responsible for executing a single <code>TaskProcessor</code>. During
 * execution it makes sure to extend the visibility timeout of the item on the
 * queue. the <code>TaskProcessor</code>.
 *
 * @author Daniel Bernstein
 */
public class TaskWorkerImpl implements TaskWorker {
    private static Logger log = LoggerFactory.getLogger(TaskWorkerImpl.class);
    private ScheduledThreadPoolExecutor timer;

    private TaskProcessorFactory processorFactory;
    private TaskQueue queue;
    private TaskQueue deadLetterQueue;
    private boolean done = false;
    private boolean started = false;
    private Runnable currentTimerTask;
    private Task task;
    private boolean initialized = false;

    /**
     * @param task
     * @param processorFactory
     * @param queue
     */
    public TaskWorkerImpl(Task task,
                          TaskProcessorFactory processorFactory,
                          TaskQueue queue,
                          TaskQueue deadLetterQueue,
                          ScheduledThreadPoolExecutor timer) {
        if (task == null) {
            throw new IllegalArgumentException("task must be non-null");
        }
        if (queue == null) {
            throw new IllegalArgumentException("queue must be non-null");
        }
        if (deadLetterQueue == null) {
            throw new IllegalArgumentException("deadLetterQueue must be non-null");
        }

        if (processorFactory == null) {
            throw new IllegalArgumentException("processor must be non-null");
        }

        if (timer == null) {
            throw new IllegalArgumentException("timer must be non-null");
        }

        this.task = task;
        this.processorFactory = processorFactory;
        this.queue = queue;
        this.deadLetterQueue = deadLetterQueue;
        this.timer = timer;
        log.debug("new worker created {}", this);

    }

    private void scheduleVisibilityTimeoutExtender(final Task task,
                                                   Date timeFrom,
                                                   final Integer visibilityTimeout) {
        // schedule task to run in half the visibility timeout to help ensure that the time runs
        //before the visibility timeout expires.
        long delay = ((long) (visibilityTimeout * 1000 * 0.5));
        Runnable timerTask = new Runnable() {
            @Override
            public void run() {
                if (!done) {
                    try {
                        log.debug("extending timeout of {} {} seconds", task, visibilityTimeout);
                        queue.extendVisibilityTimeout(task);
                        log.debug("timeout extended for {}", task);
                        // reschedule task
                        scheduleVisibilityTimeoutExtender(task, new Date(), visibilityTimeout);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        };

        this.currentTimerTask = timerTask;
        timer.schedule(currentTimerTask, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * This method be called before run since it is possible that there may be significant
     * delay between when the TaskWorker is initialized and when it is executed.
     */
    protected void init() {
        log.debug("taskworker {} initializing...", this);

        scheduleVisibilityTimeoutExtender(task, new Date(), task.getVisibilityTimeout());

        log.debug("taskworker {} initialized", this);
        initialized = true;

    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        log.debug("taskworker run starting...", this);
        if (!initialized) {
            String error = "The taskworker must be initialized before it can be run";
            log.error(error);
            throw new RuntimeException(error);
        }

        if (done || started) {
            log.warn("task worker {} can only be run once:  started={}, done={}. Ignoring...",
                     new Object[] {this, started, done});
            return;
        }

        started = true;

        try {
            log.debug("{} dequeued {}", this, this.task);
            TaskProcessor processor = this.processorFactory.create(task);
            processor.execute();
            deleteTaskFromQueue(task);

            log.info("completed task:  task_type={} task_class={} attempts={} result={} elapsed_time={}",
                     task.getType(),
                     task.getClass().getSimpleName(),
                     task.getAttempts(),
                     "success",
                     System.currentTimeMillis() - startTime);

        } catch (Throwable t) {
            int attempts = task.getAttempts();
            log.error(MessageFormat.format("failed to complete:  task_type={0} attempts={1} "
                                           + "result=failure elapsed_time={2} properties=\"{3}\" "
                                           + "message=\"{4}\"",
                                           task.getType().name(),
                                           attempts,
                                           System.currentTimeMillis() - startTime,
                                           task.getProperties(),
                                           t.getMessage()), t);

            if (attempts < TaskWorker.MAX_ATTEMPTS) {
                requeueTask(this.task);
            } else {
                task.addProperty("error", t.getClass().getName() + ":" + t.getMessage());
                sendToDeadLetterQueue(task);
            }

        } finally {
            done = true;
            if (this.currentTimerTask != null) {
                this.timer.remove(this.currentTimerTask);
            }

            log.debug("task worker finished {}", this.task);
        }
    }

    private void requeueTask(Task task) {
        try {
            this.queue.requeue(task);
        } catch (Throwable e) {
            log.error(MessageFormat.format("failed to requeue task: task_type={0} "
                                           + "properties=\"{1}\" "
                                           + "message=\"{2}\"",
                                           task.getType().name(),
                                           task.getProperties(),
                                           e.getMessage()), e);
        }
    }

    /**
     * @param task
     */
    private void sendToDeadLetterQueue(Task task) {
        log.info("putting {} on dead letter queue", task);

        try {
            deleteTaskFromQueue(task);

            this.deadLetterQueue.put(task);
            log.info("sent {} to dead-letter-queue={}", task, deadLetterQueue.getName());

        } catch (Throwable e) {
            log.error(MessageFormat.format("failed to send to dead letter queue:  task_type={0} "
                                           + "properties=\"{1}\" "
                                           + "message=\"{2}\"",
                                           task.getType().name(),
                                           task.getProperties(),
                                           e.getMessage()), e);
        }

    }

    private void deleteTaskFromQueue(Task task) {
        try {
            log.debug("deleting {} from {}", task, this.queue);
            this.queue.deleteTask(task);
        } catch (TaskNotFoundException e) {
            log.error("Error deleting " + task + ". This should never happen: " + e.getMessage(), e);
        }
    }
}
