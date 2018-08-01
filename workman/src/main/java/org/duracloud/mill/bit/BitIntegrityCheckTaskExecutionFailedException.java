/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.mill.workman.TaskExecutionFailedException;

/**
 * @author Erik Paulsson
 * Date: 4/29/14
 */
public class BitIntegrityCheckTaskExecutionFailedException
    extends TaskExecutionFailedException {

    public BitIntegrityCheckTaskExecutionFailedException(String message) {
        super(message);
    }

    public BitIntegrityCheckTaskExecutionFailedException(String message,
                                                         Throwable cause) {
        super(message, cause);
    }
}
