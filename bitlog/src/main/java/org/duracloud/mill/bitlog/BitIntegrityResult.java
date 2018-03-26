/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog;

/**
 * @author Daniel Bernstein
 * Date: Apr 25, 2014
 */
public enum BitIntegrityResult {
    SUCCESS,
    FAILURE,
    ERROR,
    // IGNORE will not show up in the bit integrity log. It is used by the BitCheckHandler
    // to detect cases the bit integrity task should ignore.
    IGNORE
}
