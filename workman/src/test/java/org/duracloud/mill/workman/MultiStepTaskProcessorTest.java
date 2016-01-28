/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.duracloud.common.util.WaitUtil;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 *	       Date: May 7, 2014
 */
@RunWith(EasyMockRunner.class)
public class MultiStepTaskProcessorTest extends EasyMockSupport{

    @Mock
    private TaskProcessor step1;
    
    @Mock
    private TaskProcessor step2;

    @Mock
    private TaskProcessor step1a;
    
    @Mock
    private TaskProcessor step2a;

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
     * @throws TaskExecutionFailedException 
     */
    @Test
    public void testExecute() throws TaskExecutionFailedException {
        step1.execute();
        EasyMock.expectLastCall();
        step2.execute();
        EasyMock.expectLastCall();
        
        replayAll();
        
        processor.addTaskProcessor(step1);
        processor.addTaskProcessor(step2);
        
        processor.execute();
        
    }
    
    @Test
    public void testExecuteWithIgoreInConcurrentThreads() throws Exception{
        
        //execute the multistep processor in two separate threads. In one thread
        //ignore after the first step; in the second thread do not ignore
        step1.execute();
        EasyMock.expectLastCall().andStubAnswer(new IAnswer<Object>() {
            /* (non-Javadoc)
             * @see org.easymock.IAnswer#answer()
             */
            @Override
            public Object answer() throws Throwable {
                TransProcessorState.ignore();
                return null;
            }
        });

        step1.execute();
        EasyMock.expectLastCall();

        step2.execute();
        EasyMock.expectLastCall();

        replayAll();

        processor.addTaskProcessor(step1);
        processor.addTaskProcessor(step2);

        Future<Boolean> result1 = executeProcessorInThread();
        Future<Boolean> result2 = executeProcessorInThread();

        assertTrue(result1.get(1000, TimeUnit.MILLISECONDS));
        assertTrue(result2.get(1000, TimeUnit.MILLISECONDS));

    }

    
    @Test
    public void testExecuteWithIgoreInSameThread() throws Exception{
        
        //execute the two processors successively in one thread.  First time around
        //ignore after the first step; in the second time around do not ignore
        step1.execute();
        EasyMock.expectLastCall().andStubAnswer(new IAnswer<Object>() {
            /* (non-Javadoc)
             * @see org.easymock.IAnswer#answer()
             */
            @Override
            public Object answer() throws Throwable {
                TransProcessorState.ignore();
                return null;
            }
        });

        step1a.execute();
        EasyMock.expectLastCall();


        step2a.execute();
        EasyMock.expectLastCall();

        replayAll();

        processor.addTaskProcessor(step1);
        processor.addTaskProcessor(step2);

        processor.execute();

        processor = new MultiStepTaskProcessor();
        processor.addTaskProcessor(step1a);
        processor.addTaskProcessor(step2a);
        
        processor.execute();

        
    }

    private Future<Boolean> executeProcessorInThread() {
        FutureTask<Boolean> future = new FutureTask<>(new Callable<Boolean>() {
            private boolean result = false;
            @Override
            public Boolean call() throws Exception {
                try {
                    processor.execute();
                    result = true;
                } catch (Exception e) {}
                return result;
            }
        });
        new Thread(future).start();
        return future;
    }

}
