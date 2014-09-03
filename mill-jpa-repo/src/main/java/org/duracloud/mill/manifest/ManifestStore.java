/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest;

import java.util.Iterator;

import org.duracloud.error.NotFoundException;
import org.duracloud.mill.db.model.ManifestItem;

/**
 * @author Daniel Bernstein
 *         Date: Sep 2, 2014
 */
public interface ManifestStore {

    /**
     * @param account
     * @param storeId
     * @param spaceId
     * @param contentId
     * @param contentChecksum
     * @throws ManifestItemWriteException
     */
    void write(String account,
               String storeId,
               String spaceId,
               String contentId,
               String contentChecksum) throws ManifestItemWriteException;

    /**
     * @param account
     * @param storeId
     * @param spaceId
     * @return
     */
    Iterator<ManifestItem> getItems(String account,
                                    String storeId,
                                    String spaceId);


    /**
     * 
     * @param account
     * @param storeId
     * @param spaceId
     * @param contentId
     * @return
     * @throws NotFoundException
     */
    ManifestItem getItem(String account,
                         String storeId,
                         String spaceId,
                         String contentId) throws NotFoundException;

}
