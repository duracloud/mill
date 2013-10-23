/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskExecutionFailedException extends Exception {

    public TaskExecutionFailedException() {
        super();
    }

    public TaskExecutionFailedException(String message) {
        super(message);
    }

    public TaskExecutionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
