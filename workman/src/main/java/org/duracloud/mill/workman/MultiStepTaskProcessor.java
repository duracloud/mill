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

/**
 * @author Daniel Bernstein
 *	       Date: Apr 10, 2014
 */
public class MultiStepTaskProcessor implements TaskProcessor{
    
    
    private List<TaskProcessor> processors = new ArrayList<>();

    
    /**
     * @param processor
     */
    public void addTaskProcessor(TaskProcessor processor){
        this.processors.add(processor);
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessor#execute()
     */
    @Override
    public void execute() throws TaskExecutionFailedException {
        for(TaskProcessor processor : processors){
            if(!TransProcessorState.isIgnore()){
                processor.execute();
            }
        }
    }
}
