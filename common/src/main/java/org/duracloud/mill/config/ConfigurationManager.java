/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.config;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Daniel Bernstein
 * Date: Oct 25, 2013
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

    public String[] getNotificationRecipients() {
        return getCommaSeparatedListToArray(ConfigConstants.NOTIFICATION_RECIPIENTS);
    }

    private String[] getCommaSeparatedListToArray(String prop) {
        String values = System.getProperty(prop);
        if (StringUtils.isBlank(values)) {
            return new String[0];
        } else {
            return values.split(",");
        }
    }

    public String[] getNotificationRecipientsNonTech() {
        return getCommaSeparatedListToArray(ConfigConstants.NOTIFICATION_RECIPIENTS_NON_TECH);
    }
}
