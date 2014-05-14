/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import java.util.Date;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.AuditLogWriteFailedException;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.contentindex.client.ContentIndexItem;
import org.duracloud.contentindex.client.ContentIndexClientValidationException;
import org.duracloud.mill.audit.ContentIndexUpdatingProcessor;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 *	       Date: Mar 20, 2014
 */
@RunWith(EasyMockRunner.class)
public class ContentIndexUpdatingProcessorTest extends EasyMockSupport {

    private ContentIndexUpdatingProcessor processor;
        
    
    @Mock
    private ContentIndexClient contentIndex;
    
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
    public void test() throws TaskExecutionFailedException, AuditLogWriteFailedException, ContentIndexClientValidationException {
        Capture<ContentIndexItem> contentIndexItemCapture = new Capture<ContentIndexItem>();
        
        EasyMock.expect(
                contentIndex.save(EasyMock.capture(contentIndexItemCapture)))
                .andReturn("test");
        
        
        AuditTask task = AuditTestHelper.createTestAuditTask();
        replayAll();
        processor = new ContentIndexUpdatingProcessor(task, contentIndex);
        
        processor.execute();
    }


}
