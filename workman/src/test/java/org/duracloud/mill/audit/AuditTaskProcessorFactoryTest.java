/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.common.queue.task.NoopTask;
import org.duracloud.mill.workman.TaskProcessorCreationFailedException;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 *	       Date: Mar 20, 2014
 */
@RunWith(EasyMockRunner.class)
public class AuditTaskProcessorFactoryTest extends EasyMockSupport{
   
    @TestSubject
    private AuditTaskProcessorFactory factory;
    
    @Mock
    private AuditLogStore logStore;
    
    @Mock
    private ContentIndexClient contentIndex;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        factory = new AuditTaskProcessorFactory(contentIndex,logStore);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
    }

    @Test
    public void testCreate() throws TaskProcessorCreationFailedException {
        AuditTask task = AuditTestHelper.createTestAuditTask();
        replayAll();
        factory.create(task.writeTask());
    }



    @Test
    public void testCreateFail()  {
        NoopTask task = new NoopTask();
        
        replayAll();
        try {
            factory.create(task.writeTask());
            Assert.fail();
        } catch (TaskProcessorCreationFailedException e) {
            //expected do nothing
        }
    }

}
