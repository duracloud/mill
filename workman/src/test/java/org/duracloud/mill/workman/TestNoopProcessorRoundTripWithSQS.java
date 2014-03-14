/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import junit.framework.Assert;

import org.duracloud.common.queue.task.NoopTask;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.noop.NoopTaskProcessor;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.mill.workman.spring.AppConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This class tests a round trip for processing Noop Tasks, from task creation
 * to task processing using an sqs queue.
 * 
 * @author Daniel Bernstein Date: Oct 24, 2013
 */

public class TestNoopProcessorRoundTripWithSQS {

    private ApplicationContext context;
    private TaskQueue queue;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        queue = (TaskQueue)context.getBean("taskQueue");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        context = null;
    }

    @Test
    public void test() {
        
        int count = 10;
        for(int i = 0; i < count; i++){
            NoopTask noopTask = new NoopTask();
            noopTask.setAccount("foobar");
            noopTask.setContentId("foobar");
            noopTask.setSpaceId("foobar");
            noopTask.setStoreId("foobar");
            Task task = noopTask.writeTask();
            task.setVisibilityTimeout(600);
            queue.put(task);
        }

        sleep(3000);
        Assert.assertEquals(count, NoopTaskProcessor.getCompletedCount());
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
