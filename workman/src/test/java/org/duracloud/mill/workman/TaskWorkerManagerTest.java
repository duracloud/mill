/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.easymock.EasyMock;
import org.junit.Test;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerManagerTest {

    @Test
    public void test() throws Exception {

        TaskWorkerFactory factory = EasyMock
                .createMock(TaskWorkerFactory.class);

        EasyMock.expect(factory.create()).andReturn(new TaskWorker() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                }
            }
        }).times(9);

        TaskWorkerManager manager = new TaskWorkerManager(factory);

        manager.setMaxPoolSize(4);

        Thread.sleep(3000);

    }
}
