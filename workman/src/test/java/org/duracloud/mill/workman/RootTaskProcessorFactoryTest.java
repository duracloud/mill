/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.common.queue.task.Task;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *	       Date: Oct 25, 2013
 */
public class RootTaskProcessorFactoryTest {


    /**
     * Test method for {@link org.duracloud.mill.workman.RootTaskProcessorFactory#create(org.duracloud.common.queue.task.Task)}.
     * @throws TaskProcessorCreationFailedException 
     */
    @Test
    public void test() throws TaskProcessorCreationFailedException {
        TaskProcessor taskProcessor = EasyMock.createMock(TaskProcessor.class);
        TaskProcessorFactory bad = EasyMock.createMock(TaskProcessorFactory.class);
        EasyMock.expect(bad.isSupported(EasyMock.isA(Task.class))).andReturn(false);
        TaskProcessorFactory good = EasyMock.createMock(TaskProcessorFactory.class);
        EasyMock.expect(good.isSupported(EasyMock.isA(Task.class))).andReturn(true);
        EasyMock.expect(good.create(EasyMock.isA(Task.class))).andReturn(taskProcessor);
        EasyMock.replay(taskProcessor, bad, good);
        RootTaskProcessorFactory taskProcessorFactory = new RootTaskProcessorFactory();
        taskProcessorFactory.addTaskProcessorFactory(bad);
        taskProcessorFactory.addTaskProcessorFactory(good);
        TaskProcessor p2 = taskProcessorFactory.create(new Task());
        Assert.assertEquals(taskProcessor,p2);
        EasyMock.verify(taskProcessor, bad, good);
    }

}
