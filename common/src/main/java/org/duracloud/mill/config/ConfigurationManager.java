/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.config;


/**
 * @author Daniel Bernstein
 *	       Date: Oct 25, 2013
 */
public class ConfigurationManager {
    
    public String getAuditQueueName() {
        return System.getProperty(ConfigConstants.QUEUE_NAME_AUDIT);
    }
    
    public String getBitIntegrityQueue() {
        return System.getProperty(ConfigConstants.QUEUE_NAME_BIT_INTEGRITY);
    }

    public String getWorkDirectoryPath() {
        return System.getProperty(ConfigConstants.WORK_DIRECTORY_PATH);
    }
}
