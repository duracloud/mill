/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.mill.queue.TaskQueue;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerFactoryImpl implements TaskWorkerFactory {
    private TaskQueue queue;
    private TaskProcessorFactory processorFactory;

    public TaskWorkerFactoryImpl(TaskQueue queue, TaskProcessorFactory factory) {
        if (queue == null)
            throw new IllegalArgumentException("queue must be non-null");
        if (factory == null)
            throw new IllegalArgumentException(
                    "processorFactory must be non-null");
        this.queue = queue;
        this.processorFactory = factory;

    }

    @Override
    public TaskWorkerImpl create() {
        return new TaskWorkerImpl(processorFactory, queue);
    }

}
