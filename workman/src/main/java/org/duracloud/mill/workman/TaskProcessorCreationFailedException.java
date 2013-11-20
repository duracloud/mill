/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;


/**
 * An exception that's thrown when a <code>TaskProcessor</code> cannot be created for any reason.
 * @author Daniel Bernstein
 *
 */
public class TaskProcessorCreationFailedException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public TaskProcessorCreationFailedException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param e
     */
    public TaskProcessorCreationFailedException(String message, Exception e) {
        super(message,e);
    }
}
