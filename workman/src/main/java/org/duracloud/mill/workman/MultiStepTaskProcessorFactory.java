/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.ArrayList;
import java.util.List;

import org.duracloud.common.queue.task.Task;

/**
 * A {@link TaskProcessorFactory} that can build a {@link MultiStepTaskProcessor}.  Instances of 
 * this class can be configured with an arbitrary number of TaskProcessorFactories which will
 * be invoked when building the MultiStepTaskProcessor.
 * @author Daniel Bernstein 
 *         Date: Apr 10, 2014
 */
public class MultiStepTaskProcessorFactory implements TaskProcessorFactory {

    private List<TaskProcessorFactory> factories = new ArrayList<>();

    /**
     * @param processor
     */
    public void addFactory(TaskProcessorFactory processor) {
        this.factories.add(processor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.workman.TaskProcessorFactory#create(org.duracloud.
     * common.queue.task.Task)
     */
    @Override
    public TaskProcessor create(Task task)
            throws TaskProcessorCreationFailedException {
        
        MultiStepTaskProcessor processor = new MultiStepTaskProcessor();
        
        for (TaskProcessorFactory factory : factories) {
            processor.addTaskProcessor(factory.create(task));
        }
        
        return processor;
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessorFactory#isSupported(org.duracloud.common.queue.task.Task)
     */
    @Override
    public boolean isSupported(Task task) {
        for (TaskProcessorFactory factory : factories) {
            if(factory.isSupported(task)){
                return true;
            }
        }
        return false;
    }

}
