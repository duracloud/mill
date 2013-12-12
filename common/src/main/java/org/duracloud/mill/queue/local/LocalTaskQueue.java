/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.queue.local;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskNotFoundException;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a local, in-memory task queue. (It is intended to use
 * for testing purposes.)
 * 
 * @author Daniel Bernstein Date: Oct 24, 2013
 */
public class LocalTaskQueue implements TaskQueue {
    private Queue<Task> queue;
    private Logger log = LoggerFactory.getLogger(LocalTaskQueue.class);
    private List<Task> inprocess; 
    private long completedCount = 0;
    /**
     * 
     */
    public LocalTaskQueue() {
        queue = new LinkedBlockingQueue<>();
        inprocess = new LinkedList<>();
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.queue.TaskQueue#put(org.duracloud.mill.domain.Task)
     */
    @Override
    public synchronized void put(Task task) {
        queue.add(task);
    }

    @Override
    public synchronized void put(Task... tasks) {
        Set<Task> taskSet = new HashSet<>();
        taskSet.addAll(Arrays.asList(tasks));
        this.put(taskSet);
    }

    @Override
    public synchronized void put(Set<Task> tasks) {
        queue.addAll(tasks);
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.queue.TaskQueue#take()
     */
    @Override
    public synchronized Task take() throws TimeoutException {
        try{
            Task task =  queue.remove();
            inprocess.add(task);
            return task;
        }catch(NoSuchElementException ex){
            throw new TimeoutException(ex);
        }
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.queue.TaskQueue#extendVisibilityTimeout(org.duracloud.mill.domain.Task)
     */
    @Override
    public void extendVisibilityTimeout(Task task) throws TaskNotFoundException {
        log.info("extending visibility time on {}", task);
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.queue.TaskQueue#deleteTask(org.duracloud.mill.domain.Task)
     */
    @Override
    public synchronized void deleteTask(Task task) throws TaskNotFoundException {
        if(!this.inprocess.contains(task)){
            log.error("{} not found.", task);

            throw new TaskNotFoundException("task not found:" + task.toString());
        }
        
        this.inprocess.remove(task);
        this.completedCount++;
        log.info("{} complete", task);
    }

    @Override
    public Integer size() {
        return queue.size();
    }
    
    public int getInprocessCount(){
        return this.inprocess.size();
    }
    
    public long getCompletedCount(){
        return completedCount;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.queue.TaskQueue#requeue(org.duracloud.mill.domain.Task)
     */
    @Override
    public void requeue(Task task) {
        this.inprocess.remove(task);
        task.incrementAttempts();
        this.queue.add(task);
    }
}
