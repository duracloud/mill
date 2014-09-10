/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import org.duracloud.mill.db.model.JpaAuditLogItem;

/**
 * @author Daniel Bernstein
 *         Date: Sep 5, 2014
 */
public interface LogManager {

    /**
     * @param item
     */
    void write(JpaAuditLogItem item);

    /**
     * 
     */
    void flushLogs();

}
