/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.mill.bitlog.BitIntegrityResult;

/**
 * @author Daniel Bernstein
 *	       Date: May 15, 2014
 */
public class HandlerResult {
    private String message;
    private BitIntegrityResult result;
    private boolean handled = false;
   
    public HandlerResult() {
    }

    /**
     * @param result
     * @param message
     */
    public HandlerResult(BitIntegrityResult result,String message) {
        super();
        if(result == null) throw new IllegalArgumentException("result must be non-null");

        if (result.equals(BitIntegrityResult.FAILURE)
                || result.equals(BitIntegrityResult.ERROR)) {
            if (message == null) {
                throw new IllegalArgumentException(
                        "message must be non null in cases of error or failure.");
            }
        }
        this.result = result;
        this.message = message;
        handled = true;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * @return the result
     */
    public BitIntegrityResult getResult() {
        return result;
    }
    
    /**
     * @return the handled
     */
    public boolean isHandled() {
        return handled;
    }
}
