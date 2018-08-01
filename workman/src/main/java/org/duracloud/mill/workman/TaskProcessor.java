/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

/**
 * The TaskProcessor implements the concrete functionality of a particular type
 * of task.
 *
 * @author Daniel Bernstein
 */
public interface TaskProcessor {

    /**
     * Performs the processing indicated by the task. The underlying method
     * should execute synchronously.
     */
    void execute() throws TaskExecutionFailedException;
}
