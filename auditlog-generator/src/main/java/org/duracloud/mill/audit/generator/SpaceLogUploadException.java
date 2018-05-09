/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import org.duracloud.common.error.DuraCloudRuntimeException;

/**
 * @author Daniel Bernstein
 * Date: Sep 5, 2014
 */
public class SpaceLogUploadException extends DuraCloudRuntimeException {
    public SpaceLogUploadException(String message, Exception ex) {
        super(message, ex);
    }
}
