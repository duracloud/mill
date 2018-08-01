/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.notification;

import java.util.Arrays;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: Dec 31, 2013
 */
public class SESNotificationManager implements NotificationManager {
    private static final Logger log =
        LoggerFactory.getLogger(SESNotificationManager.class);

    private String[] recipientEmailAddresses;
    private AmazonSimpleEmailService client;

    public SESNotificationManager(String[] recipientEmailAddresses, AmazonSimpleEmailService client) {
        this.recipientEmailAddresses = recipientEmailAddresses;

        if (ArrayUtils.isEmpty(this.recipientEmailAddresses)) {
            log.warn("There are no recipient emails configured. The notification manager will " +
                     "ignore notification requests.");
        } else {
            log.info("configured with the following recipients: {}",
                     ArrayUtils.toString(recipientEmailAddresses));
        }

        this.client = client;
    }

    /**
     * @param recipients
     */
    public SESNotificationManager(String[] recipients) {
        this(recipients, AmazonSimpleEmailServiceAsyncClientBuilder.defaultClient());
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.notification.NotificationManager#sendEmail(java.lang.String, java.lang.String)
     */
    @Override
    public void sendEmail(String subject, String body) {
        if (ArrayUtils.isEmpty(this.recipientEmailAddresses)) {
            log.warn("No recipients configured - no one to notify: ignoring...");
            return;
        }

        SendEmailRequest email = new SendEmailRequest();
        try {
            Destination destination = new Destination();
            destination.setToAddresses(Arrays.asList(this.recipientEmailAddresses));
            email.setDestination(destination);
            email.setSource(System.getProperty("notification.sender", "no-sender-specified"));
            Message message = new Message(new Content(subject), new Body(new Content(body)));
            email.setMessage(message);
            client.sendEmail(email);
            log.info("email sent: {}", email);
        } catch (Exception e) {
            log.error("failed to send " + email + ": " + e.getMessage(), e);
        }

    }
}
