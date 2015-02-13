/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.notification;

import java.util.List;

import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.db.model.BitIntegrityReport;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 31, 2013
 */
public interface NotificationManager {

    /**
     * Sends a new space notification to the Mill's administrator
     * @param subdomain of the account on which a new space was created.
     * @param storeId of the storage provider in which the space was created.
     * @param spaceId of the space
     * @param datetime time at which create occurred
     * @param username user who created the space
     */
    void newSpace(String subdomain,
            String storeId,
            String spaceId,
            String datetime,
            String username);

    /**
     * sends bit integrity report errors to the Mill's administrator
     * @param report
     * @param errors
     */
    void bitIntegrityErrors(BitIntegrityReport report,
                            List<BitLogItem> errors);
    
}
