/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

/**
 * This class defines  thread bound state to facilitate 
 * communication between TaskProcessors when MultiStepTaskProcessor
 * are working a task.
 * @author Daniel Bernstein
 *         Date: Jan 8, 2016
 */
public class TransProcessorState {
    private static ThreadLocal<Boolean> IGNORE = new ThreadLocal<Boolean>(){
        protected Boolean initialValue() {
            return Boolean.FALSE;
        };
    };
    
    public static boolean isIgnore() {
        return IGNORE.get();
    }
    
    public static void ignore(){
        IGNORE.set(true);
    }
}
