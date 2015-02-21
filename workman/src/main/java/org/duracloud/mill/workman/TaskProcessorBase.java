/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.text.MessageFormat;

import org.duracloud.common.queue.task.SpaceCentricTypedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein Date: Feb 20, 2015
 */
public abstract class TaskProcessorBase implements
                                       TaskProcessor {
    private static Logger log = LoggerFactory
            .getLogger(TaskProcessorBase.class);
    private SpaceCentricTypedTask task;

    public TaskProcessorBase(SpaceCentricTypedTask task) {
        this.task = task;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.workman.TaskProcessor#execute()
     */
    @Override
    public final void execute() throws TaskExecutionFailedException {
        String message = MessageFormat.format("processor completed: processor_class={0} account={1} store_id={2} space_id={3} attempts={4} result=",
                this.getClass().getSimpleName(),
                task.getAccount(),
                task.getStoreId(),
                task.getSpaceId(),
                task.getAttempts());
        try {
            
            executeImpl();
            log.info(message + "{}", 
                    "success");

        }catch(TaskExecutionFailedException ex){
            log.error(message + "{}", 
                     "failure");
            throw ex;
        }
    }

    /**
     * @return the task
     */
    protected SpaceCentricTypedTask getTask() {
        return task;
    }
    /**
     * 
     */
    protected abstract void executeImpl() throws TaskExecutionFailedException;
}
