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

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerManagerTest {

    @Test
    public void testInit() throws Exception {
        
        final CountDownLatch latch = new CountDownLatch(4);
        
        TaskWorkerFactory factory = EasyMock
                .createMock(TaskWorkerFactory.class);

        EasyMock.expect(factory.create()).andReturn(new TaskWorker() {
            @Override
            public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    latch.countDown();

            }
        }).times(5,6);

        EasyMock.replay(factory);
        
        TaskWorkerManager manager = new TaskWorkerManager(factory);

        //set max workers
        System.setProperty(TaskWorkerManager.MAX_WORKER_PROPERTY_KEY, "4");

        manager.init();

        Assert.assertEquals(4, manager.getMaxPoolSize());

        Assert.assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        
        manager.destroy();
        
        EasyMock.verify(factory);
        
    }
}
