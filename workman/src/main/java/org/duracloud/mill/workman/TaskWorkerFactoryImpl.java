/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;

/**
 * @author Daniel Bernstein
 */
public class TaskWorkerFactoryImpl implements TaskWorkerFactory {

    private TaskProcessorFactory processorFactory;
    private TaskQueue deadLetterQueue;
    private ScheduledThreadPoolExecutor timer;

    public TaskWorkerFactoryImpl(TaskProcessorFactory factory, TaskQueue deadLetterQueue) {
        if (factory == null) {
            throw new IllegalArgumentException("processorFactory must be non-null");
        }
        this.processorFactory = factory;

        if (deadLetterQueue == null) {
            throw new IllegalArgumentException("deadLetterQueue must be non-null");
        }
        this.deadLetterQueue = deadLetterQueue;

        this.timer = new ScheduledThreadPoolExecutor(5);

    }

    @Override
    public TaskWorkerImpl create(Task task, TaskQueue queue) {
        TaskWorkerImpl taskWorker = new TaskWorkerImpl(task,
                                                       processorFactory,
                                                       queue,
                                                       deadLetterQueue,
                                                       this.timer);
        taskWorker.init();
        return taskWorker;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskWorkerFactory#destroy()
     */
    @Override
    public void destroy() {
        this.timer.shutdown();
    }

}
