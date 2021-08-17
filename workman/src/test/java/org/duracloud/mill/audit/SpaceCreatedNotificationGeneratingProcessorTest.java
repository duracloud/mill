/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

package org.duracloud.mill.audit;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 * Date: Oct 30, 2013
 */
@RunWith(EasyMockRunner.class)
public class SpaceCreatedNotificationGeneratingProcessorTest extends EasyMockSupport {

    @Mock
    private NotificationManager notificationManager;

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
        verifyAll();
    }

    @Test
    public void testSpaceCreateDefaultDomain() throws TaskExecutionFailedException {
        AuditTask task = AuditTestHelper.createTestAuditTask();
        task.setAction(AuditTask.ActionType.CREATE_SPACE.name());

        Capture<String> cap = EasyMock.newCapture();
        notificationManager.sendEmail(anyString(), capture(cap));
        EasyMock.expectLastCall().once();
        replayAll();
        new SpaceCreatedNotifcationGeneratingProcessor(task, notificationManager).execute();

        Assert.assertTrue(cap.getValue().contains(".duracloud.org"));
    }

    @Test
    public void testSpaceCreateDifferentDomain() throws TaskExecutionFailedException {
        System.setProperty(ConfigConstants.DURACLOUD_SITE_DOMAIN, "other.domain");

        AuditTask task = AuditTestHelper.createTestAuditTask();
        task.setAction(AuditTask.ActionType.CREATE_SPACE.name());

        Capture<String> cap = EasyMock.newCapture();
        notificationManager.sendEmail(anyString(), capture(cap));
        EasyMock.expectLastCall().once();
        replayAll();
        new SpaceCreatedNotifcationGeneratingProcessor(task, notificationManager).execute();

        Assert.assertTrue(cap.getValue().contains(".other.domain"));
    }

}
