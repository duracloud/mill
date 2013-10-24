/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerImplTest {
    private Task task;
    private TaskQueue queue;
    private TaskProcessor processor;
    private TaskProcessorFactory factory;
    private Integer visibilityTimeout = Integer.valueOf(10001);

    @Before
    public void setUp() throws Exception {
        task = EasyMock.createMock(Task.class);
        processor = EasyMock.createMock(TaskProcessor.class);
        queue = EasyMock.createMock(TaskQueue.class);
        factory = EasyMock.createMock(TaskProcessorFactory.class);
        EasyMock.expect(task.getVisibilityTimeout()).andReturn(
                visibilityTimeout);
        EasyMock.expect(queue.take()).andReturn(task);
        EasyMock.expect(factory.create(EasyMock.isA(Task.class))).andReturn(
                processor);

    }

    @After
    public void tearDown() throws Exception {
        EasyMock.verify(processor, queue, task, factory);
    }

    private void replay() throws Exception {
        EasyMock.replay(processor, queue, task, factory);
    }

    @Test
    public void testRun() throws Exception {
        processor.execute();
        final int times = 2;
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                Thread.sleep(visibilityTimeout * times);
                return null;
            }
        });

        queue.extendVisibilityTimeout(EasyMock.isA(Task.class));
        EasyMock.expectLastCall().times(2, 4);
        queue.deleteTask(EasyMock.isA(Task.class));
        EasyMock.expectLastCall();

        replay();
        TaskWorkerImpl w = new TaskWorkerImpl(factory, queue);
        w.run();
        // sleep to make sure that the internal timer task is being cancelled.
        Thread.sleep(2000);
    }

    @Test
    public void testRunWithProcessorException() throws Exception {
        processor.execute();
        EasyMock.expectLastCall().andThrow(new TaskExecutionFailedException());

        replay();
        TaskWorkerImpl w = new TaskWorkerImpl(factory, queue);
        w.run();
        // sleep to make sure that the internal timer task is being cancelled.
        Thread.sleep(3000);
    }

}
