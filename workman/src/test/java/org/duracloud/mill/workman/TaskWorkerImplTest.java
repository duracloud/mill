/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerImplTest {
    private Task task;
    private TaskQueue queue;
    private TaskQueue deadLetterQueue;
    private TaskProcessor processor;
    private TaskProcessorFactory factory;

    @Before
    public void setUp() throws Exception {
        task = EasyMock.createMock(Task.class);
        expect(task.getType()).andReturn(Task.Type.NOOP);
        
        processor = EasyMock.createMock(TaskProcessor.class);
        queue = EasyMock.createMock(TaskQueue.class);
        deadLetterQueue = EasyMock.createMock(TaskQueue.class);

        factory = EasyMock.createMock(TaskProcessorFactory.class);
        EasyMock.expect(task.getVisibilityTimeout()).andReturn(
                1);
        EasyMock.expect(factory.create(EasyMock.isA(Task.class))).andReturn(
                processor);

    }

    @After
    public void tearDown() throws Exception {
        EasyMock.verify(processor, queue, deadLetterQueue, task, factory);
    }

    private void replay() throws Exception {
        EasyMock.replay(processor, queue, deadLetterQueue, task, factory);
    }

    @Test
    public void testRun() throws Exception {
        processor.execute();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                Thread.sleep(2000);
                return null;
            }
        });

        
        queue.extendVisibilityTimeout(EasyMock.isA(Task.class));
        EasyMock.expectLastCall().times(2, 4);
        queue.deleteTask(EasyMock.isA(Task.class));
        EasyMock.expectLastCall().once();
        expect(task.getAttempts()).andReturn(1);

        replay();
        TaskWorkerImpl w = createTaskWorkerImpl();
        w.init();
        w.run();
        // sleep to make sure that the internal timer task is being cancelled.
        Thread.sleep(2000);
    }

    private ScheduledThreadPoolExecutor createScheduledThreadPool() {
        return new ScheduledThreadPoolExecutor(5);
    }

    private void runWithProcessorException() throws Exception {
        processor.execute();
        EasyMock.expectLastCall().andThrow(new TaskExecutionFailedException());
        EasyMock.expect(task.getProperties())
                .andReturn(new HashMap<String, String>());
        replay();
        TaskWorkerImpl w = createTaskWorkerImpl();
        w.init();
        w.run();
        // sleep to make sure that the internal timer task is being cancelled.
        Thread.sleep(3000);
    }

    private TaskWorkerImpl createTaskWorkerImpl() {
        TaskWorkerImpl w = new TaskWorkerImpl(task,
                                              factory,
                                              queue,
                                              deadLetterQueue,
                                              createScheduledThreadPool());
        return w;
    }

    @Test
    public void testRunWithProcessorExceptionFirstAttempt() throws Exception {
        EasyMock.expect(task.getAttempts()).andReturn(0).times(1);
        queue.requeue(EasyMock.isA(Task.class));
        EasyMock.expectLastCall().once();
        runWithProcessorException();
    }

    @Test
    public void testRunWithProcessorExceptionLastAttempt() throws Exception {
        expect(task.getAttempts()).andReturn(4).times(1);
        queue.deleteTask(EasyMock.isA(Task.class));
        expect(deadLetterQueue.getName()).andReturn("queue");

        expectLastCall().once();
        deadLetterQueue.put(EasyMock.isA(Task.class));
        expectLastCall().once();
        task.addProperty(eq("error"), isA(String.class));
        expectLastCall().once();
        runWithProcessorException();
    }

}
