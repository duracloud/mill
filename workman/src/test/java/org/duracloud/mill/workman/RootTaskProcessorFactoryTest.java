/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.mill.domain.Task;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *	       Date: Oct 25, 2013
 */
public class RootTaskProcessorFactoryTest {


    /**
     * Test method for {@link org.duracloud.mill.workman.RootTaskProcessorFactory#create(org.duracloud.mill.domain.Task)}.
     * @throws TaskNotSupportedException 
     */
    @Test
    public void test() throws TaskNotSupportedException {
        TaskProcessor p = EasyMock.createMock(TaskProcessor.class);
        TaskProcessorFactory bad = EasyMock.createMock(TaskProcessorFactory.class);
        EasyMock.expect(bad.create(EasyMock.isA(Task.class))).andThrow(new TaskNotSupportedException("test"));
        TaskProcessorFactory good = EasyMock.createMock(TaskProcessorFactory.class);
        EasyMock.expect(good.create(EasyMock.isA(Task.class))).andReturn(p);
        EasyMock.replay(p, bad, good);
        RootTaskProcessorFactory f = new RootTaskProcessorFactory();
        f.addTaskProcessorFactory(bad);
        f.addTaskProcessorFactory(good);
        TaskProcessor p2 = f.create(new Task());
        Assert.assertEquals(p,p2);
        EasyMock.verify(p, bad, good);
    }

}
