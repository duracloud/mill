/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman.spring;

import java.io.File;

import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.file.ConfigFileCredentialRepo;
import org.duracloud.mill.credentials.simpledb.SimpleDBCredentialsRepo;
import org.duracloud.mill.dup.DuplicationTaskProcessorFactory;
import org.duracloud.mill.noop.NoopTaskProcessorFactory;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.aws.SQSTaskQueue;
import org.duracloud.mill.workman.RootTaskProcessorFactory;
import org.duracloud.mill.workman.TaskWorkerFactoryImpl;
import org.duracloud.mill.workman.TaskWorkerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;

/**
 * 
 * @author Daniel Bernstein
 *	       Date: Oct 24, 2013
 */
@ComponentScan(basePackages = { "org.duracloud.mill" })
@Configuration
public class AppConfig {
    
    private static Logger log = LoggerFactory.getLogger(AppConfig.class);
    
    @Bean
    public RootTaskProcessorFactory rootTaskProcessorFactory(CredentialsRepo repo,
                                                             File workDir) {
        RootTaskProcessorFactory factory =  new RootTaskProcessorFactory();
        factory.addTaskProcessorFactory(
            new DuplicationTaskProcessorFactory(repo, workDir));
        factory.addTaskProcessorFactory(
            new NoopTaskProcessorFactory(repo, workDir));
        log.info("RootTaskProcessorFactory created.");
        return factory;
    }

    @Bean
    public CredentialsRepo credentialRepo(ConfigurationManager configurationManager) {
        String path = configurationManager.getCredentialsFilePath();
        if(path != null){
            log.info(
                    "found credentials file path ({}): using config file based credential repo...",
                    path);
            return new ConfigFileCredentialRepo();
        }else{
            log.info("no credentials file path: using simpledb based credential repo...");
            return new SimpleDBCredentialsRepo(new AmazonSimpleDBClient());
        }
    }

    @Bean
    public File workDir(WorkmanConfigurationManager configurationManager) {
        log.info("creating work dir for path: "
                + configurationManager.getWorkDirectoryPath());
        return new File(configurationManager.getWorkDirectoryPath());
    }

    @Bean(initMethod="init", destroyMethod="destroy")
    public TaskWorkerManager taskWorkerManager(RootTaskProcessorFactory factory,
                                                TaskQueue lowPriorityQueue,
                                                TaskQueue highPriorityQueue,
                                                TaskQueue deadLetterQueue) {
        return new TaskWorkerManager(lowPriorityQueue, 
                                     highPriorityQueue,
                                     new TaskWorkerFactoryImpl(factory, 
                                                               deadLetterQueue));
    }
    
    @Bean
    public TaskQueue lowPriorityQueue(WorkmanConfigurationManager configurationManager){
        TaskQueue queue =  new SQSTaskQueue(configurationManager.getLowPriorityDuplicationQueue());
        log.info("created low priority queue {}", queue);
        return queue;
    }

    @Bean
    public TaskQueue highPriorityQueue(WorkmanConfigurationManager configurationManager){
        TaskQueue queue =  new SQSTaskQueue(configurationManager.getHighPriorityDuplicationQueueName());
        log.info("created high priority queue {}", queue);
        return queue;
    }

    @Bean
    public TaskQueue deadLetterQueue(WorkmanConfigurationManager configurationManager){
        TaskQueue queue =  new SQSTaskQueue(configurationManager.getDeadLetterQueueName());
        log.info("created dead letter  queue {}", queue);
        return queue;
    }

    @Bean(initMethod="init")
    public WorkmanConfigurationManager configurationManager(){
        log.info("creating the workman configuration manager...");
        return new WorkmanConfigurationManager();
    }
    
    
}
