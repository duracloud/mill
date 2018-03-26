/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

/**
 * @author Daniel Bernstein
 * Date: Oct 14, 2014
 */
public class ChecksumsDoNotMatchException extends Exception {
    public ChecksumsDoNotMatchException(String message) {
        super(message);
    }
}
