/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman.spring;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.dynamodb.DynamoDBAuditLogStore;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.contentindex.client.ESContentIndexClient;
import org.duracloud.mill.audit.AuditTaskProcessorFactory;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.file.ConfigFileCredentialRepo;
import org.duracloud.mill.credentials.simpledb.SimpleDBCredentialsRepo;
import org.duracloud.mill.dup.DuplicationTaskProcessorFactory;
import org.duracloud.mill.noop.NoopTaskProcessorFactory;
import org.duracloud.mill.workman.RootTaskProcessorFactory;
import org.duracloud.mill.workman.TaskWorkerFactoryImpl;
import org.duracloud.mill.workman.TaskWorkerManager;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
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
    public RootTaskProcessorFactory 
                rootTaskProcessorFactory(CredentialsRepo repo,
                                         File workDir,
                                         ContentIndexClient contentIndexClient,
                                         AuditLogStore auditLogStore) {

        RootTaskProcessorFactory factory = new RootTaskProcessorFactory();
        factory.addTaskProcessorFactory(
            new DuplicationTaskProcessorFactory(repo,
                                                workDir,
                                                auditQueue(configurationManager())));
        factory.addTaskProcessorFactory(new AuditTaskProcessorFactory(
                contentIndexClient, auditLogStore));
        
        factory.addTaskProcessorFactory(new NoopTaskProcessorFactory(repo,
                workDir));

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
    public ContentIndexClient contentIndexClient(WorkmanConfigurationManager config){
        Client client = new TransportClient()
                .addTransportAddress(new InetSocketTransportAddress(
                        config.getContentIndexHost(), config.getContentIndexPort()));
        ElasticsearchOperations ops = new ElasticsearchTemplate(client);
        return new ESContentIndexClient(ops,client);
    }

    @Bean
    public AuditLogStore auditLogStore(){
        DynamoDBAuditLogStore store =  new DynamoDBAuditLogStore();
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.setRegion(Region.getRegion(Regions.US_EAST_1));
        store.initialize(client);
        return store;
    }
    @Bean
    public File workDir(WorkmanConfigurationManager configurationManager) {
        log.info("creating work dir for path: "
                + configurationManager.getWorkDirectoryPath());
        return new File(configurationManager.getWorkDirectoryPath());
    }

    @Bean(initMethod="init", destroyMethod="destroy")
    public TaskWorkerManager taskWorkerManager(WorkmanConfigurationManager config, RootTaskProcessorFactory factory,
                                                TaskQueue deadLetterQueue) {
        return new TaskWorkerManager(createTaskQueues(config),
                                     deadLetterQueue,
                                     new TaskWorkerFactoryImpl(factory, 
                                                               deadLetterQueue));
    }
    
    protected List<TaskQueue> createTaskQueues(WorkmanConfigurationManager configurationManager){
        List<String> taskQueuesNames = configurationManager.getTaskQueueNames();
        List<TaskQueue> taskQueues = new LinkedList<>();
        for(String taskQueueName : taskQueuesNames){
            TaskQueue taskQueue = new SQSTaskQueue(taskQueueName.trim());
            taskQueues.add(taskQueue);
            log.info("created queue {}: priority = {}",
                    taskQueue.getName(),taskQueues.size());
        }
        return taskQueues;
    }

    @Bean
    public TaskQueue auditQueue(WorkmanConfigurationManager configurationManager){
        TaskQueue queue =  new SQSTaskQueue(configurationManager.getAuditQueueName());
        log.info("created audit queue {}", queue);
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
