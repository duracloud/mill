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

import org.duracloud.mill.common.domain.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It is responsible for executing processors.
 * the <code>TaskProcessor</code>. 
 * @author Daniel Bernstein
 *
 */
public class TaskWorker implements Runnable{
	static final long TIMEOUT_BUFFER = 2000;
	private static Logger log = LoggerFactory.getLogger(TaskWorker.class);
	private static Timer timer = new Timer();

	private TaskProcessor processor;
	private TaskQueue queue;
	private boolean done = false;
	private Task task;
	private TimerTask currentTimerTask;
	
	/**
	 * 
	 * @param processor
	 * @param queue
	 * @param taskReceivedTime
	 * @param visibilityTimeout
	 */
	public TaskWorker(TaskProcessor processor, TaskQueue queue, Date taskReceivedTime, long visibilityTimeout){
		if(queue == null) throw new IllegalArgumentException("queue non-null");
		if(processor == null) throw new IllegalArgumentException("processor non-null");
		this.processor = processor;
		this.queue = queue;
		this.task = processor.getTask();
		
		scheduleTimeTask(taskReceivedTime, visibilityTimeout);
		log.debug("new worker created {}", this);

	}
	
	private void scheduleTimeTask(Date timeFrom, final long visibilityTimeout) {
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
						scheduleTimeTask(new Date(), visibilityTimeout);
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

		try {
			this.processor.execute();
			this.queue.deleteTask(task);
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
