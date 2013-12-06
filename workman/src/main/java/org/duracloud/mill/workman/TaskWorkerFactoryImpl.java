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

    public TaskWorkerFactoryImpl(TaskProcessorFactory factory) {
        if (factory == null)
            throw new IllegalArgumentException(
                    "processorFactory must be non-null");
        this.processorFactory = factory;
    }

    @Override
    public TaskWorkerImpl create(Task task, TaskQueue queue) {
        TaskWorkerImpl taskWorker = new TaskWorkerImpl(task, processorFactory, queue);
        taskWorker.init();
        return taskWorker;
    }

}
