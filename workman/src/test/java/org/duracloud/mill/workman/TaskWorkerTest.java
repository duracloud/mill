/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.Date;

import org.duracloud.mill.common.domain.Task;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 * 
 * @author Daniel Bernstein
 *
 */
public class TaskWorkerTest {
	private Task task;
	private TaskQueue queue;
	private TaskProcessor processor;
	@Before
	public void setUp() throws Exception {
		task = EasyMock.createMock(Task.class);
		processor = EasyMock.createMock(TaskProcessor.class);
		queue = EasyMock.createMock(TaskQueue.class);

		EasyMock.expect(processor.getTask()).andReturn(task);
	}

	@After
	public void tearDown() throws Exception {
		EasyMock.verify(processor, queue, task);
	}

	private void replay() throws Exception {
		EasyMock.replay(processor, queue, task);
	}

	@Test
	public void testRun() throws Exception{
		processor.execute();
		final long timeout = 500;
		final int times = 2;
		EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
			@Override
			public Object answer() throws Throwable {
				Thread.sleep(timeout*times);
				return null;
			}
		});
		
		queue.extendVisibilityTimeout(EasyMock.isA(Task.class), EasyMock.anyLong());
		EasyMock.expectLastCall().times(times);
		queue.deleteTask(EasyMock.isA(Task.class));
		EasyMock.expectLastCall();

		replay();
		Date received = new Date();
		TaskWorker w = new TaskWorker(processor, queue, received, TaskWorker.TIMEOUT_BUFFER + timeout);
		w.run();
		//sleep to make sure that the internal timer task is being cancelled.
		Thread.sleep(2000);
	}

	@Test
	public void testRunWithProcessorException() throws Exception{
		processor.execute();
		EasyMock.expectLastCall().andThrow(new TaskExecutionFailedException());
		
		replay();
		Date received = new Date();
		TaskWorker w = new TaskWorker(processor, queue, received, TaskWorker.TIMEOUT_BUFFER + 500);
		w.run();
		//sleep to make sure that the internal timer task is being cancelled.
		Thread.sleep(3000);
	}

}
