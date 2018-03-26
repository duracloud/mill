/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.duracloud.mill.workman.TaskExecutionFailedException;

/**
 * @author Bill Branan
 * Date: 10/22/13
 */
public class DuplicationTaskExecutionFailedException
    extends TaskExecutionFailedException {

    public DuplicationTaskExecutionFailedException(String message) {
        super(message);
    }

    public DuplicationTaskExecutionFailedException(String message,
                                                   Throwable cause) {
        super(message, cause);
    }

}
