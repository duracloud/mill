/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.TimeoutException;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerManagerTest {
    private TaskQueue  lowPriorityQueue, highPriorityQueue;

    private TaskWorkerFactory factory;
    
    @Before
    public void setup(){
        lowPriorityQueue = EasyMock
                .createMock(TaskQueue.class);

        highPriorityQueue = EasyMock
                .createMock(TaskQueue.class);

        factory = EasyMock
                .createMock(TaskWorkerFactory.class);
        
    }
    
    @After
    public void tearDown(){
        EasyMock.verify(lowPriorityQueue,highPriorityQueue,factory);

    }
    
    @Test
    public void testInit() throws Exception {
        int times = 4;
        final CountDownLatch latch = new CountDownLatch((times+1)*2);
        Task task = new Task();
        
        configureQueue(times, latch, task, lowPriorityQueue);
        configureQueue(times, latch, task, highPriorityQueue);


        EasyMock.replay(factory,lowPriorityQueue, highPriorityQueue);
        
        TaskWorkerManager manager = new TaskWorkerManager(lowPriorityQueue,
                                                          highPriorityQueue, 
                                                          factory);

        //set max workers
        System.setProperty(TaskWorkerManager.MAX_WORKER_PROPERTY_KEY, times+"");

        manager.init();

        Assert.assertEquals(times, manager.getMaxWorkers());

        Assert.assertTrue(latch.await(6000, TimeUnit.MILLISECONDS));
        
        manager.destroy();
        
        
    }


    private void configureQueue(int times,
            final CountDownLatch latch,
            Task task,
            TaskQueue queue)            throws TimeoutException {
        EasyMock.expect(queue.take()).andReturn(task).times(times);

        EasyMock.expect(factory.create(task, queue)).andReturn(new TaskWorker() {
            @Override
            public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {}
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
