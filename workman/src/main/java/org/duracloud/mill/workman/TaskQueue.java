/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.mill.common.domain.Task;
/**
 * 
 * @author Daniel Bernstein
 *
 */
public interface TaskQueue {
	
	/**
	 * Blocks until a task is available
	 * @return
	 */
	Task nextTask();

	/**
	 * 
	 * @return
	 */
	public long getDefaultVisibilityTimeout();
	
	
	/**
	 * Responsible for robustly extending the visibility timeout.
	 * @param task
	 * @param milliseconds
	 * @throws TaskNotFoundException
	 */
	void extendVisibilityTimeout(Task task, long milliseconds) throws TaskNotFoundException;

	/**
	 * 
	 * @param task
	 */
	void deleteTask(Task task);
}
