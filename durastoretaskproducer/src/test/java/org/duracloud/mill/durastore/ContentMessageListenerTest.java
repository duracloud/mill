/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

package org.duracloud.mill.durastore;

import java.util.Set;

import org.duracloud.mill.domain.Task;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.queue.TaskNotFoundException;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.TimeoutException;
import org.duracloud.storage.aop.ContentMessage;
import org.duracloud.storage.aop.ContentMessage.ACTION;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Daniel Bernstein
 *	       Date: Oct 30, 2013
 */
public class ContentMessageListenerTest {

    private DuplicationPolicyManager policyManager;
    private TaskQueue duplicationTaskQueue;
    private String subdomain = "subdomain";
    private DuplicationPolicy policy;
    private NotificationManager notificationManager;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        duplicationTaskQueue = EasyMock.createMock(TaskQueue.class);
        policyManager = EasyMock.createMock(DuplicationPolicyManager.class);

        policy = new DuplicationPolicy();
        notificationManager = EasyMock.createMock(NotificationManager.class);
    }

    private void replay(){
        EasyMock.replay(duplicationTaskQueue, policyManager,
                notificationManager);
    }
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        EasyMock.verify(duplicationTaskQueue, policyManager,
                notificationManager);
    }

    /**
     * Test method for {@link org.duracloud.mill.durastore.ContentMessageListener#onMessage(org.duracloud.storage.aop.ContentMessage)}.
     */
    @Test
    public void testOnMessageNoPolicies() {
        ContentMessage message = createMessage("DELETE");
        EasyMock.expect(policyManager.getDuplicationPolicy(EasyMock.isA(String.class))).andReturn(policy);
        
        replay();
        ContentMessageListener listener = new ContentMessageListener(duplicationTaskQueue, policyManager, subdomain);
        listener.onMessage(message);
    }

    @Test
    public void testOnMessageNonDuplicationAction() {
        ContentMessage message = createMessage("ERROR");
        replay();
        ContentMessageListener listener = new ContentMessageListener(
                duplicationTaskQueue, policyManager, subdomain);
        listener.onMessage(message);
    }

    /**
     * @return
     */
    private ContentMessage createMessage(String action) {
        ContentMessage message = new ContentMessage();
        message.setAction(action);
        message.setContentId("contentId");
        message.setSpaceId("spaceId");
        message.setStoreId("storeId");
        return message;
    }

    @Test
    public void testInvalidAction() {
        ContentMessage message = createMessage("INVALIDACTION");
        replay();
        ContentMessageListener listener = new ContentMessageListener(
                duplicationTaskQueue, policyManager, subdomain);
        listener.onMessage(message);
    }

    @Test
    public void testSpaceCreate() {
        ContentMessage message = new ContentMessage();
        message.setSpaceId("spaceId");
        message.setStoreId("storeId");
        message.setUsername("username");
        message.setDatetime("date");
        notificationManager.newSpace(subdomain, "storeId", "spaceId", "date",
                "username");
        EasyMock.expectLastCall();
        replay();
        SpaceCreateMessageListener listener = new SpaceCreateMessageListener(subdomain,
                notificationManager);
        listener.onMessage(message);
    }

    @Test
    public void testSpaceDelete() {
        ContentMessage message = new ContentMessage();
        message.setSpaceId("spaceId");
        message.setStoreId("storeId");
        message.setUsername("username");
        message.setDatetime("date");
        SpaceDeleteMessageListener listener = new SpaceDeleteMessageListener(duplicationTaskQueue, policyManager, subdomain);
        testSuccessfulDuplicationTaskCreation(message, listener);
    }

    
    @Test
    public void testSuccessfulDuplicationTaskOnDelete(){
        testSuccessfulDuplicationTaskCreation(ACTION.DELETE);
    }

    @Test
    public void testSuccessfulDuplicationTaskOnCopy(){
        testSuccessfulDuplicationTaskCreation(ACTION.COPY);
    }
    @Test
    public void testSuccessfulDuplicationTaskOnIngest(){
        testSuccessfulDuplicationTaskCreation(ACTION.INGEST);
    }
    @Test
    public void testSuccessfulDuplicationTaskOnUpdate(){
        testSuccessfulDuplicationTaskCreation(ACTION.UPDATE);
    }

    private void testSuccessfulDuplicationTaskCreation(ACTION action){
        ContentMessageListener listener = new ContentMessageListener(
                duplicationTaskQueue, policyManager, subdomain);
        testSuccessfulDuplicationTaskCreation(createMessage(action.name()), listener);
    }
    private void testSuccessfulDuplicationTaskCreation(ContentMessage message, MessageListener listener){
        
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
        
        replay();
        listener.onMessage(message);
        
        
    }
    
    private static class TaskQueueAdapter implements TaskQueue{
        public void put(Task task) {}
        public void put(Task... tasks) {}
        public void put(Set<Task> tasks) {}
        public Task take() throws TimeoutException { return null;}
        public void extendVisibilityTimeout(Task task)
                throws TaskNotFoundException {}
        public void deleteTask(Task task) throws TaskNotFoundException {}
        public Integer size() { return null;}
        /* (non-Javadoc)
         * @see org.duracloud.mill.queue.TaskQueue#requeue(org.duracloud.mill.domain.Task)
         */
        @Override
        public void requeue(Task task) {}
        
    }
}
