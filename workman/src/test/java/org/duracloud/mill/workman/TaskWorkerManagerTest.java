/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.duracloud.common.queue.task.Task;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 */
@RunWith(EasyMockRunner.class)
public class TaskWorkerManagerTest extends EasyMockSupport {

    @Mock
    private TaskQueue lowPriorityQueue;

    @Mock
    private TaskQueue highPriorityQueue;

    @Mock
    private TaskQueue deadLetterQueue;

    @Mock
    private TaskWorkerFactory factory;

    @Before
    public void setup() {

    }

    @After
    public void tearDown() {
        verifyAll();
    }

    @Test
    public void testInit() throws Exception {
        int times = 4;
        final CountDownLatch latch = new CountDownLatch((times + 1) * 2);
        Task task = new Task();

        configureQueue(times, latch, task, lowPriorityQueue);
        configureQueue(times, latch, task, highPriorityQueue);
        EasyMock.expect(deadLetterQueue.getName()).andReturn("dead").anyTimes();
        EasyMock.expect(deadLetterQueue.size()).andReturn(0).anyTimes();

        replayAll();

        TaskWorkerManager manager;

        manager = new TaskWorkerManager(Arrays.asList(highPriorityQueue, lowPriorityQueue),
                                        deadLetterQueue,
                                        factory);

        //set max workers
        System.setProperty(TaskWorkerManager.MAX_WORKER_PROPERTY_KEY, times + "");

        manager.init();

        Assert.assertEquals(times, manager.getMaxWorkers());

        Assert.assertTrue(latch.await(6000, TimeUnit.MILLISECONDS));

        manager.destroy();
    }

    private void configureQueue(int times,
                                final CountDownLatch latch,
                                Task task,
                                TaskQueue queue) throws TimeoutException {

        EasyMock.expect(queue.size()).andReturn(0).anyTimes();
        EasyMock.expect(queue.take()).andReturn(task).times(times);
        EasyMock.expect(queue.getName()).andReturn("test").anyTimes();

        EasyMock.expect(factory.create(task, queue)).andReturn(new TaskWorker() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    // Exit sleep on interruption
                }
                latch.countDown();
            }
        }).times(times);
        EasyMock.expect(queue.take()).andStubAnswer(new IAnswer<Task>() {
            /* (non-Javadoc)
             * @see org.easymock.IAnswer#answer()
             */
            @Override
            public Task answer() throws Throwable {
                latch.countDown();
                throw new TimeoutException();
            }
        });
    }

}
