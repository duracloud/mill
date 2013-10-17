/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It is responsible for executing a single <code>TaskProcessor</code>. During execution it makes sure to 
 * extend the visibility timeout of the item on the queue.  
 * the <code>TaskProcessor</code>. 
 * @author Daniel Bernstein
 *
 */
public class TaskWorkerImpl implements TaskWorker{
	static final long TIMEOUT_BUFFER = 500;
	private static Logger log = LoggerFactory.getLogger(TaskWorkerImpl.class);
	private static Timer timer = new Timer();

	private TaskProcessorFactory processorFactory;
	private TaskQueue queue;
	private boolean done = false;
	private boolean started = false;
	private TimerTask currentTimerTask;
	
	/**
	 * 
	 * @param processorFactory
	 * @param queue
	 * @param taskReceivedTime
	 * @param visibilityTimeout
	 */
	public TaskWorkerImpl(TaskProcessorFactory processorFactory, TaskQueue queue) {
		if(queue == null) throw new IllegalArgumentException("queue non-null");
		if(processorFactory == null) throw new IllegalArgumentException("processor non-null");
		this.processorFactory = processorFactory;
		this.queue = queue;
		log.debug("new worker created {}", this);

	}
	
	private void scheduleVisibilityTimeoutExtender(final Task task, Date timeFrom, final long visibilityTimeout) {
		//schedule task to run two seconds before the expiration of the task
		//lest the timer task execute late for any reason.
		Date executionTime = new Date(timeFrom.getTime()+visibilityTimeout-TIMEOUT_BUFFER);
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				if(!done){
					try {
						log.debug("extending timeout of {} {} milliseconds", task, visibilityTimeout);
						queue.extendVisibilityTimeout(task, visibilityTimeout);
						log.debug("timeout extended for {}", task);
						//reschedule task
						scheduleVisibilityTimeoutExtender(task, new Date(), visibilityTimeout);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		};
		
		this.currentTimerTask = timerTask;
		timer.schedule(currentTimerTask, executionTime);
	}

	@Override
	public void run() {
		log.debug("taskworker run starting...", this);

		if(done || started) {
			log.warn(
					"task worker {} can only be run once:  started={}, done={}. Ignoring...",
					new Object[] { this, started, done });
			return;
		}

		try {
			Task task = this.queue.take();
			log.debug("{} dequeued {}", this, task);
			scheduleVisibilityTimeoutExtender(task, new Date(), queue.getDefaultVisibilityTimeout());
			TaskProcessor processor = this.processorFactory.create(task);
			processor.execute();
			this.queue.deleteTask(task);
		}catch (TimeoutException e) {
			log.info("queue.take() invocation timed out: no queue items to read.");
			//TODO re-queue at this point or send to error queue? 
		} catch (TaskExecutionFailedException e) {
			log.error("failed to complete task execution: " + e.getMessage(), e);
			//TODO re-queue at this point or send to error queue? 
		} catch (Exception e) {
			log.error("unexpected error: " + e.getMessage(), e);
			e.printStackTrace();
		} finally {
			done = true;
			if(currentTimerTask != null){
				currentTimerTask.cancel();
			}
		}
	}
}
