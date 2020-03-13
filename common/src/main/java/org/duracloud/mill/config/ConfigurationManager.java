/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.config;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.common.model.EmailerType;
import org.duracloud.common.queue.QueueType;

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

    public String[] getSwiftConfig() {
        String[] config = new String[] {
            System.getProperty(ConfigConstants.SWIFT_ACCESS_KEY),
            System.getProperty(ConfigConstants.SWIFT_SECRET_KEY),
            System.getProperty(ConfigConstants.SWIFT_ENDPOINT),
            System.getProperty(ConfigConstants.SWIFT_SIGNER_TYPE)
        };
        return config;
    }

    public QueueType getQueueType() {
        String queueType = System.getProperty(ConfigConstants.QUEUE_TYPE);
        if (queueType != null &&
            queueType.trim().equalsIgnoreCase(QueueType.RABBITMQ.toString())) {
            return QueueType.RABBITMQ;
        } else {
            return QueueType.SQS;
        }
    }

    public String[] getRabbitMQConfig() {
        if (getQueueType().equals(QueueType.RABBITMQ)) {
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

    public EmailerType getEmailerType() {
        String emailerType = System.getProperty(ConfigConstants.EMAILER_TYPE);
        if (emailerType != null &&
            emailerType.trim().equalsIgnoreCase(EmailerType.SMTP.toString())) {
            return EmailerType.SMTP;
        } else {
            return EmailerType.SES;
        }
    }

    public String[] getSMTPConfig() {
        if (getEmailerType().equals(EmailerType.SMTP)) {
            String[] config = new String[] {
                    System.getProperty(ConfigConstants.NOTIFICATION_HOST),
                    System.getProperty(ConfigConstants.NOTIFICATION_PORT),
                    System.getProperty(ConfigConstants.NOTIFICATION_USER),
                    System.getProperty(ConfigConstants.NOTIFICATION_PASS),
                    System.getProperty(ConfigConstants.NOTIFICATION_SENDER)
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
