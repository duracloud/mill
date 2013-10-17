/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.List;

import org.duracloud.mill.domain.Task;

/**
 * This class delegates TaskProcessor creation to a list of possible factories.
 * If none of the underlying factories supports the task, an exception is
 * thrown.
 * 
 * @author Daniel Bernstein
 * 
 */
public class RootTaskProcessorFactory implements TaskProcessorFactory {
    private List<TaskProcessorFactory> factories;

    public RootTaskProcessorFactory(List<TaskProcessorFactory> factories) {
        if (factories == null || factories.isEmpty()) {
            throw new IllegalArgumentException(
                    "you must specify at least one TaskProcessorFactory");
        }

        this.factories = factories;
    }

    @Override
    public TaskProcessor create(Task task) throws TaskNotSupportedException {
        TaskProcessor p = null;
        for (TaskProcessorFactory factory : factories) {
            try {
                p = factory.create(task);
                break;
            } catch (TaskNotSupportedException e) {
                continue;
            }
        }

        if (p == null) {
            throw new TaskNotSupportedException(
                    task
                            + " is not supported: no compatible TaskProcessorFactory found.");
        }
        return p;
    }

}
