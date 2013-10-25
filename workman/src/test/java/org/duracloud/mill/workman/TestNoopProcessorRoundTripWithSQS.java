/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.io.File;

import junit.framework.Assert;

import org.duracloud.common.util.ApplicationConfig;
import org.duracloud.mill.domain.NoopTask;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.local.LocalTaskQueue;
import org.duracloud.mill.workman.spring.AppConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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
    //@Before
    public void setUp() throws Exception {
        File testCredFile = new File("../common/src/test/resources/test.credentials.json");
        Assert.assertTrue(testCredFile.exists());
        System.setProperty("credentials.file.path", testCredFile.getAbsolutePath());
        System.setProperty("duracloud.sqsQueueUrl", "{your queue url}");
        System.setProperty("aws.accessKeyId", "{your access key here}");
        System.setProperty("aws.secretKey", "{your secret key}");
        
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

    //@Test
    public void test() {
        
        int count = 10000;
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

        sleep(60*60*1000);

        
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
