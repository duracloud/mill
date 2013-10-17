/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import org.duracloud.mill.common.domain.Task;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;


/**
 * This class perfomrs the Duplication Task
 * @author Daniel Bernstein
 *
 */
public class DuplicationTaskProcessor implements TaskProcessor {
	private Task task;
	
	public DuplicationTaskProcessor(Task task){
		this.task = task;
	}

	@Override
	public Task getTask() {
		//TODO implement me.
		return null;
	}

	@Override
	public void execute() throws TaskExecutionFailedException{
		//TODO implement me.
	}
}
