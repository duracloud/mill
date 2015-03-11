/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

package org.duracloud.mill.audit;

import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.common.queue.TaskException;
import org.duracloud.common.queue.TaskNotFoundException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author Daniel Bernstein
 *	       Date: Oct 30, 2013
 */
@RunWith(EasyMockRunner.class)
public class DuplicationTaskProducingProcessorTest extends EasyMockSupport{

    @Mock
    private DuplicationPolicyManager policyManager;

    @Mock
    private TaskQueue duplicationTaskQueue;

    private DuplicationPolicy policy;

    @Mock
    private NotificationManager notificationManager;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        policy = new DuplicationPolicy();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
    }

    /**
     * Test method for {@link org.duracloud.mill.durastore.ContentMessageListener#onMessage(org.duracloud.storage.aop.ContentMessage)}.
     */
    @Test
    public void testNoPolicies() throws TaskExecutionFailedException {
        AuditTask task = createAuditTask(AuditTask.ActionType.DELETE_CONTENT.name());
        EasyMock.expect(policyManager.getDuplicationPolicy(EasyMock.isA(String.class))).andReturn(policy);

        replayAll();
        
        execute(task);
    }

    private void execute(AuditTask task) throws TaskExecutionFailedException {
        new DuplicationTaskProducingProcessor(task, duplicationTaskQueue,
                policyManager).execute();
    }

    @Test
    public void testOnMessageNonDuplicationAction() throws TaskExecutionFailedException{
        AuditTask task = createAuditTask(AuditTask.ActionType.GET_CONTENT.name());
        replayAll();
        execute(task);
    }

    /**
     * @return
     */
    private AuditTask createAuditTask(String action) {
        AuditTask task = AuditTestHelper.createTestAuditTask();
        task.setAction(action);
        task.setSpaceId("spaceId");
        task.setStoreId("storeId");
       
        return task;
    }

    @Test
    public void testSpaceDelete() {
        testSuccessfulDuplicationTaskCreation(AuditTask.ActionType.DELETE_SPACE);
    }

    
    @Test
    public void testSuccessfulDuplicationTaskOnDelete(){
        testSuccessfulDuplicationTaskCreation(AuditTask.ActionType.DELETE_CONTENT);
    }

    @Test
    public void testSuccessfulDuplicationTaskOnCopy(){
        testSuccessfulDuplicationTaskCreation(AuditTask.ActionType.COPY_CONTENT);
    }
    @Test
    public void testSuccessfulDuplicationTaskOnIngest(){
        testSuccessfulDuplicationTaskCreation(AuditTask.ActionType.ADD_CONTENT);
    }

    @Test
    public void testSuccessfulDuplicationTaskOnUpdate(){
        testSuccessfulDuplicationTaskCreation(AuditTask.ActionType.SET_CONTENT_PROPERTIES);
    }

    private void testSuccessfulDuplicationTaskCreation(AuditTask.ActionType action) {
        
        try {
            AuditTask task = createAuditTask(action.name());
            task.setStoreId("storeId");
            task.setSpaceId("spaceId");
            
            final int count = 10;
            for(int i = 0; i < count; i++){
                DuplicationStorePolicy storePolicy = new DuplicationStorePolicy();
                storePolicy.setSrcStoreId("storeId");
                storePolicy.setDestStoreId("destId"+i);
                policy.addDuplicationStorePolicy("spaceId", storePolicy);
            }
            
            EasyMock.expect(policyManager.getDuplicationPolicy(EasyMock.isA(String.class))).andReturn(policy);

            duplicationTaskQueue.put(EasyMock.isA(Set.class));
            
            EasyMock.expectLastCall().andStubDelegateTo(new TaskQueueAdapter(){
                /* (non-Javadoc)
                 * @see org.duracloud.mill.durastore.ContentMessageListenerTest.TaskQueueAdapter#put(java.util.Set)
                 */
                @Override
                public void put(Set<Task> tasks) {
                    Assert.assertEquals(count, tasks.size());
                }
            });
            
            replayAll();
            
            execute(task);
            
        }catch(TaskExecutionFailedException ex){
            throw new RuntimeException(ex);
        }
        
    }
    
    private static class TaskQueueAdapter implements TaskQueue{
        /* (non-Javadoc)
         * @see org.duracloud.common.queue.TaskQueue#getName()
         */
        @Override
        public String getName() {
           return "test";
        }
        public void put(Task task) {}
        public void put(Task... tasks) {}
        public void put(Set<Task> tasks) {}
        public Task take() throws TimeoutException { return null;}
        public void extendVisibilityTimeout(Task task)
                throws TaskNotFoundException {}
        public void deleteTask(Task task) throws TaskNotFoundException {}
        public Integer size() { return null;}
        public Integer sizeIncludingInvisibleAndDelayed() {
            return null;
        }
        /* (non-Javadoc)
         * @see org.duracloud.common.queue.TaskQueue#requeue(org.duracloud.common.queue.task.Task)
         */
        @Override
        public void requeue(Task task) {}
        /* (non-Javadoc)
         * @see org.duracloud.common.queue.TaskQueue#deleteTasks(java.util.Set)
         */
        @Override
        public void deleteTasks(Set<Task> tasks) throws TaskException {}
        /* (non-Javadoc)
         * @see org.duracloud.common.queue.TaskQueue#take(int)
         */
        @Override
        public Set<Task> take(int maxTasks) throws TimeoutException {
            throw new NotImplementedException();
        }
    }
}
