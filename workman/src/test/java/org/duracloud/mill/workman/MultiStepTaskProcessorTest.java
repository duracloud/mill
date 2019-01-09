/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

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
 * Date: May 7, 2014
 */
@RunWith(EasyMockRunner.class)
public class MultiStepTaskProcessorTest extends EasyMockSupport {

    @Mock
    private TaskProcessor step1;

    @Mock
    private TaskProcessor step2;

    private MultiStepTaskProcessor processor;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        processor = new MultiStepTaskProcessor();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
    }

    /**
     * Test method for {@link org.duracloud.mill.workman.MultiStepTaskProcessor#execute()}.
     *
     * @throws TaskExecutionFailedException
     */
    @Test
    public void testExecute() throws TaskExecutionFailedException {
        step1.execute();
        EasyMock.expectLastCall().once();
        step2.execute();
        EasyMock.expectLastCall().once();

        replayAll();

        processor.addTaskProcessor(step1);
        processor.addTaskProcessor(step2);

        processor.execute();

    }
}
