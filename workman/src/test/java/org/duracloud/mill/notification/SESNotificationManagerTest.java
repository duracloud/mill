/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.notification;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;

/**
 * @author Daniel Bernstein
 *	       Date: Jan 2, 2014
 */
public class SESNotificationManagerTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.duracloud.mill.notification.SESNotificationManager#newSpace(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testNewSpace() {
        AmazonSimpleEmailServiceClient client = EasyMock.createMock(AmazonSimpleEmailServiceClient.class);
        SendEmailResult result = new SendEmailResult();
        EasyMock.expect(client.sendEmail(EasyMock.isA(SendEmailRequest.class))).andReturn(result);
        EasyMock.replay(client);
        String[] recipients = new String[]{"test1@duracloud.org", "test2@duracloud.org"};
        SESNotificationManager notification = new SESNotificationManager(recipients, client);
        notification.newSpace("test", "storeId", "spaceId", "datetime", "username");
        EasyMock.verify(client);
    }

}
