/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.notification;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.duracloud.mill.bit.BitIntegrityHelper;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.db.model.BitIntegrityReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

/**
 * @author Daniel Bernstein 
 *         Date: Dec 31, 2013
 */
public class SESNotificationManager implements NotificationManager {
    private static final Logger log = 
            LoggerFactory.getLogger(SESNotificationManager.class);
    
    private String[] recipientEmailAddresses;
    private AmazonSimpleEmailService client; 
    public SESNotificationManager(String[] recipientEmailAddresses, AmazonSimpleEmailServiceClient client) {
        this.recipientEmailAddresses = recipientEmailAddresses;
        
        if(ArrayUtils.isEmpty(this.recipientEmailAddresses)){
            log.warn("There are no recipient emails configured. The notification manager will " +
                    "ignore notification requests.");
        }else{
            log.info("configured with the following recipients: {}",
                    ArrayUtils.toString(recipientEmailAddresses));
        }

        this.client = client;
    }

    /**
     * @param recipients
     */
    public SESNotificationManager(String[] recipients) {
        this(recipients, new AmazonSimpleEmailServiceClient());
    }
    
    

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.notification.NotificationManager#newSpace(java.lang
     * .String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public void newSpace(String subdomain,
            String storeId,
            String spaceId,
            String datetime,
            String username) {
        
        if (ArrayUtils.isEmpty(this.recipientEmailAddresses)) {
            log.warn(
                    "New space notification: subdomain: {}, storeId: {}, spaceId: {}: " +
                    "No recipients configured - no one to notify: ignoring...",
                    subdomain, storeId, spaceId);
            return;
        }
        
        String host = subdomain + ".duracloud.org";
        String subject = "New Space on " + host + ", provider " + storeId + ": " + 
                spaceId;
        StringBuilder body = new StringBuilder();
        
        body.append("A new space has been created!\n\n");
        body.append("Subdomain: https://" + host + "\n");
        body.append("Storage Provider Id: " + storeId + "\n");
        body.append("Space: " + spaceId + "\n");
        
        sendEmail(subject, body.toString());
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.notification.NotificationManager#bitIntegrityErrors(java.lang.String, java.lang.String, java.lang.String, java.util.List)
     */
    @Override
    public void bitIntegrityErrors(BitIntegrityReport report,
                                   List<BitLogItem> errors) {
        
        String account = report.getAccount();
        String storeId = report.getStoreId();
        String spaceId = report.getSpaceId();
        if (ArrayUtils.isEmpty(this.recipientEmailAddresses)) {
            log.warn(
                    "Bit integirty errors: subdomain: {}, storeId: {}, spaceId: {}: " +
                    "No recipients configured - no one to notify: ignoring...",
                    account, storeId, spaceId);
            return;
        }
        

        String host = account + ".duracloud.org";

        String subject = "Bit Integrity Report #" + report.getId() + ": errors (count = " +errors.size()+ ")  detected on " + 
                         host + ", providerId=" + storeId + 
                         ", spaceId=" + spaceId;

        StringBuilder body = new StringBuilder();
        
        body.append(BitIntegrityHelper.getHeader());
        for(BitLogItem error : errors){
            body.append(BitIntegrityHelper.formatLogLine(error) + "\n");
        }

        sendEmail(subject,body.toString());
    }
    
    private void sendEmail(String subject, String body) {
        SendEmailRequest email = new SendEmailRequest();
        try {
            Destination destination = new Destination();
            destination.setToAddresses(Arrays.asList(this.recipientEmailAddresses));
            email.setDestination(destination);
            email.setSource("notifications@duracloud.org");
            Message message = new Message(new Content(subject), new Body(new Content(body)));
            email.setMessage(message);
            client.sendEmail(email);
            log.info("email sent: {}", email);
        } catch (Exception e) {
            log.error("failed to send " + email + ": " + e.getMessage(), e);
        }
    }
}
