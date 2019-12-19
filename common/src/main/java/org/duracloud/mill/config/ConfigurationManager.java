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

    /**
     * @return
     */
    public String getQueueType() {
        String queueType = System.getProperty(ConfigConstants.QUEUE_TYPE).trim();
        if ( queueType.equalsIgnoreCase("aws") ) {
            return "AWS";
        } else if ( queueType.equalsIgnoreCase("rabbitmq") ) {
            return "RABBITMQ";
        } else {
            return "Unknown";
        }
    }

    /**
     * @return
     */
    public String[] getRabbitMQConfig() {

        if ( getQueueType() == "RABBITMQ" ) {
            String[] config = new String[] {
                System.getProperty(ConfigConstants.RABBITMQ_HOST),
                System.getProperty(ConfigConstants.RABBITMQ_PORT),
                System.getProperty(ConfigConstants.RABBITMQ_VHOST),
                System.getProperty(ConfigConstants.RABBITMQ_EXCHANGE),
                System.getProperty(ConfigConstants.RABBITMQ_USERNAME),
                System.getProperty(ConfigConstants.RABBITMQ_PASSWORD)
            };
            return config;
        } else {
            return new String[0];
        }
    }

    /**
     * @return
     */
    public String getNotificationType() {
        String notificationType = System.getProperty(ConfigConstants.NOTIFICATION_TYPE).trim();
        if ( notificationType.equalsIgnoreCase("aws") ) {
            return "AWS";
        } else if ( notificationType.equalsIgnoreCase("spring") ) {
            return "SPRING";
        } else {
            return "Unknown";
        }
    }

    /**
     * @return
     */
    public String[] getSpringConfig() {
        if ( getNotificationType() == "SPRING" ) {
            String[] config = new String[] {
                    System.getProperty(ConfigConstants.NOTIFICATION_HOST),
                    System.getProperty(ConfigConstants.NOTIFICATION_PORT),
                    System.getProperty(ConfigConstants.NOTIFICATION_USER),
                    System.getProperty(ConfigConstants.NOTIFICATION_PASS),
                    System.getProperty(ConfigConstants.NOTIFICATION_FROM_ADDRESS)
            };
            return config;
        } else {
            return new String[0];
        }
    }

    private String[] getCommaSeparatedListToArray(String prop) {
        String values = System.getProperty(prop);
        if (StringUtils.isBlank(values) ) {
            return new String[0];
        } else {
            return values.split(",");
        }
    }

    public String[] getNotificationRecipientsNonTech() {
        return getCommaSeparatedListToArray(ConfigConstants.NOTIFICATION_RECIPIENTS_NON_TECH);
    }
}
