/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.queue.aws;

import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.util.Retriable;
import org.duracloud.mill.util.Retrier;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * This is an integration test for SQSTaskQueue interfacing with an AWS SQS queue.
 * IMPORTANT: This test will only pass if the SQS queue is empty upon start of
 * the test AND there are no other clients taking/receiving messages off/from the queue.
 * @author Erik Paulsson
 *         Date: 10/25/13
 */
public class TestSQSTaskQueue {

    private TaskQueue queue;

    @Before
    public void setUp() {
        System.setProperty(
            ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
            "src/test/resources/mill-config-test.properties");
        ConfigurationManager confMgr = new ConfigurationManager();
        confMgr.init();
        queue = new SQSTaskQueue(confMgr.getDuplicationQueueName());
    }

    @Test
    public void putTake10() throws Exception {
        putThenTake(10, false);
    }

    @Test
    public void putTake100() throws Exception {
        putThenTake(100, false);
    }

    @Test
    public void putBatchTake18() throws Exception {
        putThenTake(18, true);
    }

    @Test
    public void putBatchTake97() throws Exception {
        putThenTake(97, true);
    }

    public void putThenTake(int numTasks, boolean batchLoad) throws Exception {
        Integer[] taskNums = new Integer[numTasks];
        int emptyArrHashcode = Arrays.hashCode(taskNums);

        if(batchLoad) {
            Set<Task> tasks = new HashSet<>();
            for(int i=1; i<=numTasks; i++) {
                tasks.add(createTask(i));
                taskNums[i-1] = i;
            }
            queue.put(tasks);
        } else {
            for(int i=1; i<=numTasks; i++) {
                queue.put(createTask(i));
                taskNums[i-1] = i;
            }
        }

        assertThat(emptyArrHashcode, not(equalTo(Arrays.hashCode(taskNums))));

        Retrier retrier = new Retrier();
        Retriable retriable = new Retriable() {
            public Object retry() throws Exception {
                return queue.take();
            }
        };

        for(int i=1; i<=numTasks; i++) {
            Task task = retrier.execute(retriable);
            assertNotNull(task);
            assertEquals(Task.Type.NOOP, task.getType());
            taskNums[Integer.parseInt(task.getProperty("taskId")) - 1] = null;

            queue.deleteTask(task);
        }

        assertEquals(emptyArrHashcode, Arrays.hashCode(taskNums));
    }

    private Task createTask(Integer taskId) {
        Task task = new Task();
        task.setType(Task.Type.NOOP);
        task.addProperty("taskId", taskId.toString());
        return task;
    }
}
