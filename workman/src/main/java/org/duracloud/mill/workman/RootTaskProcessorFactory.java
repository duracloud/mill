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

import org.duracloud.mill.domain.Task;
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

    @Override
    public TaskProcessor create(Task task) throws TaskProcessorCreationFailedException {
        TaskProcessor p = null;
        for (TaskProcessorFactory factory : factories) {
            try {
                p = factory.create(task);
                break;
            } catch (TaskProcessorCreationFailedException e) {
                continue;
            }
        }

        if (p == null) {
            throw new TaskProcessorCreationFailedException(
                    task
                            + " is not supported: no compatible TaskProcessorFactory found.");
        }
        return p;
    }
    
    public void addTaskProcessorFactory(TaskProcessorFactory factory){
        log.debug("Adding {}", factory);
        this.factories.add(factory);
    }
}
