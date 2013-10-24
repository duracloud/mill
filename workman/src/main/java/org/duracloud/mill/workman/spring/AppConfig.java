/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman.spring;

import org.duracloud.mill.credentials.CredentialRepo;
import org.duracloud.mill.credentials.file.ConfigFileCredentialRepo;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.local.LocalTaskQueue;
import org.duracloud.mill.workman.RootTaskProcessorFactory;
import org.duracloud.mill.workman.TaskWorkerFactoryImpl;
import org.duracloud.mill.workman.TaskWorkerManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author Daniel Bernstein
 *	       Date: Oct 24, 2013
 */
@ComponentScan(basePackages = { "org.duracloud.mill" })
@Configuration
public class AppConfig {
    
    @Bean
    public RootTaskProcessorFactory rootTaskProcessorFactory() {
        return new RootTaskProcessorFactory();
    }

    @Bean
    public CredentialRepo credentialRepo() {
        return new ConfigFileCredentialRepo();
    }

    @Bean(initMethod="init", destroyMethod="destroy")
    public TaskWorkerManager taskWorkerManager(
            RootTaskProcessorFactory factory, TaskQueue taskQueue) {
        return new TaskWorkerManager(new TaskWorkerFactoryImpl(taskQueue,
                factory));
    }
    
    @Bean
    public TaskQueue taskQueue(){
        //This is just a placeholder implementation.
        //We will likely want to perform queue configuration: namely pass queue name
        //and credentials to the concrete SQSTaskQueue object.
        //return new SQSTaskQueue("url");
        return new LocalTaskQueue();
    }
}
