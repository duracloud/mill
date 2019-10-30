/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.notification;

import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.ArrayUtils;
import org.duracloud.mill.config.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * @author Andrew Hong
 * Date: October 1, 2019
 */
public class SpringNotificationManager implements NotificationManager {
    private static final Logger log =
            LoggerFactory.getLogger(SpringNotificationManager.class);

    private JavaMailSenderImpl emailService;
    private String[] recipientEmailAddresses;
    private String fromAddress;

    /**
     * @param
     */
    public SpringNotificationManager(String[] recipientEmailAddresses, ConfigurationManager configurationManager) {
        this.recipientEmailAddresses = recipientEmailAddresses;
        if (ArrayUtils.isEmpty(this.recipientEmailAddresses)) {
            log.warn("There are no recipient emails configured. The notification manager will " +
                    "ignore notification requests.");
            return;
        } else {
            log.info("configured with the following recipients: {}",
                    ArrayUtils.toString(recipientEmailAddresses));
        }

        String[] emailConfig = configurationManager.getSpringConfig();
        String host = emailConfig[0].trim();
        Integer port = Integer.parseInt(emailConfig[1].trim());
        String username = emailConfig[2].trim();
        String password = emailConfig[3].trim();
        fromAddress = emailConfig[4].trim();

        emailService = new JavaMailSenderImpl();
        Properties mailProperties = new Properties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");
        mailProperties.put("mail.smtp.starttls.required", "true");
        emailService.setJavaMailProperties(mailProperties);
        emailService.setProtocol("smtp");
        emailService.setHost(host);
        emailService.setPort(port);
        emailService.setUsername(username);
        emailService.setPassword(password);

        try {
            //Test the connection
            emailService.testConnection();
            log.debug(
                    "Email connection test passed: email service with Sprint email client connected to {}, Port: {}, " +
                            "User: {}.",
                    host, port, username);

        } catch (MessagingException ex) {
            log.error("Email connection test failed when connecting to {}, Port: {}, User: {}, because {}", host, port,
                    username, ex.getMessage());
        }
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

        MimeMessage message = emailService.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message);

        try {
            messageHelper.setFrom(fromAddress);
            messageHelper.setSubject(subject);
            messageHelper.setTo(recipientEmailAddresses);
            messageHelper.setText(body, false);
        } catch (MessagingException ex) {
            log.error("Failed to prepare email message {}", ex.getMessage());
        }

        try {
            emailService.send(message);
        } catch (MailException ex) {
            log.error("Failed to send email because: {}", ex.getMessage());
        }
    }
}
