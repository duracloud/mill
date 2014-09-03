/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest;

import org.duracloud.mill.db.model.ManifestItem;

/**
 * @author Daniel Bernstein
 *         Date: Sep 2, 2014
 */
public class ManifestItemWriteException extends Exception {
    private ManifestItem item;
    /**
     * @param ex
     * @param item
     */
    public ManifestItemWriteException(Exception ex, ManifestItem item) {
        super(ex);
        this.item = item;
    }
    
    /**
     * @return the item
     */
    public ManifestItem getItem() {
        return item;
    }

}
