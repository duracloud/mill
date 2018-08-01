/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog;

import org.duracloud.common.error.DuraCloudRuntimeException;

/**
 * @author Daniel Bernstein
 * Date: Aug 29, 2014
 */
public class ItemWriteFailedException extends DuraCloudRuntimeException {

    /**
     * @param ex
     */
    public ItemWriteFailedException(Exception ex) {
        super(ex);
    }

}
