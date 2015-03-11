/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.LinkedList;
import java.util.List;

import org.duracloud.common.queue.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class delegates TaskProcessor creation to a list of possible factories.
 * If none of the underlying factories supports the task, an exception is
 * thrown.
 * 
 * @author Daniel Bernstein
 * 
 */
public class RootTaskProcessorFactory implements TaskProcessorFactory {
    private static Logger log = LoggerFactory.getLogger(RootTaskProcessorFactory.class);
    private List<TaskProcessorFactory> factories;

    public RootTaskProcessorFactory() {
        log.debug("creating new...");
        this.factories = new LinkedList<TaskProcessorFactory>();
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessorFactory#isSupported(org.duracloud.common.queue.task.Task)
     */
    @Override
    public boolean isSupported(Task task) {
        for(TaskProcessorFactory factory : factories){
            if(factory.isSupported(task)){
                return true;
            }
        }
        
        return false;
    }

    @Override
    public TaskProcessor create(Task task) throws TaskProcessorCreationFailedException {
        TaskProcessor p = null;
        for (TaskProcessorFactory factory : factories) {
            if(factory.isSupported(task)){
                p = factory.create(task);
                break;
            }else{
                log.debug(
                        "task processor {} does not support {}, trying next processor...",
                        p, task);
            }
        }

        if (p == null) {
            throw new TaskProcessorCreationFailedException(
                    task + " is not supported: no compatible TaskProcessorFactory found.");
        }
        return p;
    }
    
    public void addTaskProcessorFactory(TaskProcessorFactory factory){
        log.debug("Adding {}", factory);
        this.factories.add(factory);
    }
}
