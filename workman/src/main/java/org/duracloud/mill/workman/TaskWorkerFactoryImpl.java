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

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerFactoryImpl implements TaskWorkerFactory {
    private TaskProcessorFactory processorFactory;
    private TaskQueue deadLetterQueue;
    public TaskWorkerFactoryImpl(TaskProcessorFactory factory, TaskQueue deadLetterQueue) {
        if (factory == null)
            throw new IllegalArgumentException(
                    "processorFactory must be non-null");
        this.processorFactory = factory;

        if (deadLetterQueue == null)
            throw new IllegalArgumentException(
                    "deadLetterQueue must be non-null");
        this.deadLetterQueue = deadLetterQueue;

    }

    @Override
    public TaskWorkerImpl create(Task task, TaskQueue queue) {
        TaskWorkerImpl taskWorker = new TaskWorkerImpl(task, processorFactory, queue, deadLetterQueue);
        taskWorker.init();
        return taskWorker;
    }

}
