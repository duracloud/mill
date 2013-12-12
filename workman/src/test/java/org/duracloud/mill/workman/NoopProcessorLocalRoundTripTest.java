/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.io.File;

import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.domain.NoopTask;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.local.LocalTaskQueue;
import org.duracloud.mill.workman.spring.AppConfig;
import org.duracloud.mill.workman.spring.WorkmanConfigurationManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * This class tests a round trip for processing Noop Tasks, from task creation
 * to task processing.
 * 
 * @author Daniel Bernstein Date: Oct 24, 2013
 */
public class NoopProcessorLocalRoundTripTest {
    private static LocalTaskQueue LOW_PRIORITY_QUEUE = new LocalTaskQueue();
    private static LocalTaskQueue HIGH_PRIORITY_QUEUE = new LocalTaskQueue();
    private static LocalTaskQueue DEAD_LETTER_QUEUE = new LocalTaskQueue();

    private ApplicationContext context;

    @Configuration
    @ComponentScan(basePackages={"org.duracloud.mill"})
    public static class TestAppConfig extends AppConfig {

        @Bean
        @Override
        public TaskQueue lowPriorityQueue(WorkmanConfigurationManager configurationManager) {
            return LOW_PRIORITY_QUEUE;
        }

        @Bean
        @Override
        public TaskQueue highPriorityQueue(WorkmanConfigurationManager configurationManager) {
            return HIGH_PRIORITY_QUEUE;
        }

        @Bean
        @Override
        public TaskQueue deadLetterQueue(WorkmanConfigurationManager configurationManager) {
            return HIGH_PRIORITY_QUEUE;
        }

    }
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        File testProperties = new File("src/test/resources/workman-test.properties");
        Assert.assertTrue(testProperties.exists());
        System.setProperty(ConfigurationManager.WORK_DIRECTORY_PATH_KEY, "target");
        System.setProperty(ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY, testProperties.getAbsolutePath());
        System.setProperty(TaskWorkerManager.MIN_WAIT_BEFORE_TAKE_KEY, "1");

        context = new AnnotationConfigApplicationContext(TestAppConfig.class);
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
            Task task = noopTask.writeTask();
            task.setVisibilityTimeout(600);
            LOW_PRIORITY_QUEUE.put(task);
            HIGH_PRIORITY_QUEUE.put(task);
            
        }

        
        sleep(4000);
        
        Assert.assertEquals(0, this.LOW_PRIORITY_QUEUE.getInprocessCount());
        Assert.assertEquals(count, this.LOW_PRIORITY_QUEUE.getCompletedCount());
        Assert.assertEquals(0, this.HIGH_PRIORITY_QUEUE.getInprocessCount());
        Assert.assertEquals(count, this.HIGH_PRIORITY_QUEUE.getCompletedCount());
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
