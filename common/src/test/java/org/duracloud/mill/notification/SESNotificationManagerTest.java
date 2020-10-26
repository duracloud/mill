/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.notification;

import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import org.duracloud.mill.test.AbstractTestBase;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 * Date: Jan 2, 2014
 */
public class SESNotificationManagerTest extends AbstractTestBase {

    private String[] recipients = new String[] {"test1@duracloud.org",
                                                "test2@duracloud.org"};

    @Mock
    private AmazonSimpleEmailServiceClient client;

    private SESNotificationManager notificationManager;

    private void setupSubject() {
        notificationManager = new SESNotificationManager(recipients, client);
    }

    @Test
    public void testSendEmail() {
        setupSubject();
        Capture<SendEmailRequest> capture = Capture.newInstance(CaptureType.FIRST);
        SendEmailResult result = new SendEmailResult();
        EasyMock.expect(client.sendEmail(capture(capture))).andReturn(result);

        replayAll();
        notificationManager.sendEmail("subject", "body");
        SendEmailRequest request = capture.getValue();

        assertEquals("subject", request.getMessage().getSubject().getData());
        assertEquals(new Body(new Content("body")), request.getMessage().getBody());
        assertEquals(Arrays.asList(recipients), request.getDestination().getToAddresses());
    }

}
