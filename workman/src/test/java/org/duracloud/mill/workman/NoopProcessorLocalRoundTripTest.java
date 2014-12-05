/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.io.File;
import java.util.Arrays;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.local.LocalTaskQueue;
import org.duracloud.common.queue.task.NoopTask;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.noop.NoopTaskProcessorFactory;
import org.duracloud.mill.workman.spring.WorkmanConfigurationManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class tests a round trip for processing Noop Tasks, from task creation
 * to task processing.
 * 
 * @author Daniel Bernstein Date: Oct 24, 2013
 */
public class NoopProcessorLocalRoundTripTest {

    private final static LocalTaskQueue HIGH_PRIORITY_QUEUE = new LocalTaskQueue();
    private final static LocalTaskQueue LOW_PRIORITY_QUEUE = new LocalTaskQueue();

    private final static LocalTaskQueue DEAD_LETTER_QUEUE = new LocalTaskQueue();

    private ApplicationContext context;

    @Configuration
    public static class TestAppConfig  {

 
        @Bean(initMethod="init", destroyMethod="destroy")
        public TaskWorkerManager taskWorkerManager(RootTaskProcessorFactory factory,
                                                    TaskQueue deadLetterQueue) {
            return new TaskWorkerManager(Arrays.asList(new TaskQueue[] {
                    HIGH_PRIORITY_QUEUE, LOW_PRIORITY_QUEUE }),
                    deadLetterQueue, new TaskWorkerFactoryImpl(factory,
                            deadLetterQueue));
        }
        
        @Bean
        public RootTaskProcessorFactory 
                    rootTaskProcessorFactory(File workDir) {

            RootTaskProcessorFactory factory = new RootTaskProcessorFactory();
            factory.addTaskProcessorFactory(new NoopTaskProcessorFactory(null,
                    workDir));

            return factory;
        }
        
        @Bean
        public TaskQueue deadLetterQueue() {
            return DEAD_LETTER_QUEUE;
        }
        
        
        

        @Bean
        public File workDir() {
            WorkmanConfigurationManager configurationManager = new WorkmanConfigurationManager();
            return new File(configurationManager.getWorkDirectoryPath());
        }
    }
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        File testProperties = new File("src/test/resources/workman-test.properties");
        Assert.assertTrue(testProperties.exists());
        System.setProperty(ConfigConstants.WORK_DIRECTORY_PATH, "target");
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
        
        sleep(20*1000);

        Assert.assertEquals(0, HIGH_PRIORITY_QUEUE.getInprocessCount());
        Assert.assertEquals(count, HIGH_PRIORITY_QUEUE.getCompletedCount());
        Assert.assertEquals(0, LOW_PRIORITY_QUEUE.getInprocessCount());
        Assert.assertEquals(count, LOW_PRIORITY_QUEUE.getCompletedCount());
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
