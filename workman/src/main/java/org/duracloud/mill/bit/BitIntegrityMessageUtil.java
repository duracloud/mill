/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.storage.domain.StorageProviderType;

/**
 * @author Daniel Bernstein
 *	       Date: May 15, 2014
 */
public class BitIntegrityMessageUtil {

    /**
     * @param message
     * @param bitTask
     * @param storageProviderType
     * @return
     */
    public static String buildFailureMessage(String message,
            BitIntegrityCheckTask bitTask,
            StorageProviderType storageProviderType) {
        StringBuilder builder = new StringBuilder();
        builder.append("Failure to bit-integrity check content item due to: ");
        builder.append(message);
        builder.append(" Account: ");
        builder.append(bitTask.getAccount());
        builder.append(" Source StoreID: ");
        builder.append(bitTask.getStoreId());
        builder.append(" Store Type: ");
        builder.append(storageProviderType);
        builder.append(" SpaceID: ");
        builder.append(bitTask.getSpaceId());
        builder.append(" ContentID: ");
        builder.append(bitTask.getContentId());
        return builder.toString();
    }

}
