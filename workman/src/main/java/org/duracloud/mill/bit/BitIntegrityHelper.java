/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.common.util.DateUtil;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.storage.domain.StorageProviderType;

/**
 * @author Daniel Bernstein
 * Date: May 15, 2014
 */
public class BitIntegrityHelper {
    private static DateFormat dateFormat =
        new SimpleDateFormat(DateUtil.DateFormat.DEFAULT_FORMAT.getPattern());

    private BitIntegrityHelper() {
        // Ensures no instances are made of this class, as there are only static members.
    }

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

    /**
     * @param next
     * @return
     */
    public static String formatLogLine(BitLogItem item) {

        String[] values = {
            dateFormat.format(item.getModified()),
            item.getAccount(),
            item.getStoreId(),
            item.getStoreType().name(),
            item.getSpaceId(),
            item.getContentId(),
            item.getResult().name(),
            item.getContentChecksum(),
            item.getStorageProviderChecksum(),
            item.getManifestChecksum(),
            item.getDetails()};

        return StringUtils.join(values, "\t");

    }

    /**
     * @return
     */
    public static String getHeader() {
        String[] values = {
            "date-checked",
            "account",
            "store-id",
            "store-type",
            "space-id",
            "content-id",
            "result",
            "content-checksum",
            "provider-checksum",
            "manifest-checksum",
            "details"};

        return StringUtils.join(values, "\t") + "\n";

    }

}
