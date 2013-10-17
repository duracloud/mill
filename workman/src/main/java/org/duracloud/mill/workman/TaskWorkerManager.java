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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Daniel Bernstein
 *
 */
public class TaskWorkerManager {
	static final int DEFAULT_POOL_SIZE = 5;
	private Logger log = LoggerFactory.getLogger(TaskWorkerManager.class);
	private TaskWorkerFactory factory;
	private ThreadPoolExecutor executor;
	private boolean stop = false;
	private Timer timer = new Timer();
	public TaskWorkerManager(TaskWorkerFactory factory) {
		if (factory == null)
			throw new IllegalArgumentException("factory must be non-null");
		this.factory = factory;
		
		this.executor = new ThreadPoolExecutor(1, 1, 60 * 000,
				TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
	
		setMaxPoolSize(DEFAULT_POOL_SIZE);
	}
	
	public void init(){
		this.executor.execute(new Runnable(){
			@Override
			public void run() {
				while(!stop){
					try{
						executor.execute(factory.create());
					}catch(RejectedExecutionException ex){
						sleep(1000);
					}
				}
			}
		});
		
		timer.schedule(new TimerTask(){
			@Override
			public void run() {
				log.debug(
						"Status: max worker pool size: {}, currently running workers: {}, completed workers {}",
						new Object[] { getMaxPoolSize(),
								executor.getActiveCount(),
								executor.getCompletedTaskCount() });
			}

			
		}, new Date(), 5*60*000);
		
	}

	private int getMaxPoolSize() {
		return executor.getMaximumPoolSize()-1;
	}

	public void setMaxPoolSize(int max) {
		this.executor.setMaximumPoolSize(max+1);
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void destroy(){
		stop = true;
		timer.cancel();
        executor.shutdown();
		log.info("terminating...waiting for threads to complete processing...");
        while (!executor.isTerminated()) {
        	sleep(1000);
        }
		log.info("terminated");
	}
}
