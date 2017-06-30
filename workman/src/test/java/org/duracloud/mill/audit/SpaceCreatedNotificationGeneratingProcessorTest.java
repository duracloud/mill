/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

package org.duracloud.mill.audit;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.easymock.EasyMock.*;
/**
 * @author Daniel Bernstein
 *	       Date: Oct 30, 2013
 */
@RunWith(EasyMockRunner.class)
public class SpaceCreatedNotificationGeneratingProcessorTest extends EasyMockSupport{

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
    public void testSpaceCreate() throws TaskExecutionFailedException  {
        AuditTask task = AuditTestHelper.createTestAuditTask();
        task.setAction(AuditTask.ActionType.CREATE_SPACE.name());

        notificationManager.sendEmail(isA(String.class), isA(String.class));
        EasyMock.expectLastCall().once();
        replayAll();
        new SpaceCreatedNotifcationGeneratingProcessor(task,notificationManager).execute();
    }

 
}
