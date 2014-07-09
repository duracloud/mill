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

import org.duracloud.account.db.repo.DuracloudAccountRepo;
import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.dynamodb.DynamoDBAuditLogStore;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.contentindex.client.ESContentIndexClientUtil;
import org.duracloud.mill.audit.AuditLogWritingProcessorFactory;
import org.duracloud.mill.audit.ContentIndexUpdatingProcessorFactory;
import org.duracloud.mill.audit.DuplicationTaskProducingProcessorFactory;
import org.duracloud.mill.audit.SpaceCreatedNotifcationGeneratingProcessorFactory;
import org.duracloud.mill.bit.BitIntegrityCheckTaskProcessorFactory;
import org.duracloud.mill.bit.BitIntegrityReportTaskProcessorFactory;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.bitlog.dynamodb.DynamoDBBitLogStore;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.file.ConfigFileCredentialRepo;
import org.duracloud.mill.credentials.impl.DefaultCredentialsRepoImpl;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationPolicyRefresher;
import org.duracloud.mill.dup.DuplicationTaskProcessorFactory;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.LocalDuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.S3DuplicationPolicyRepo;
import org.duracloud.mill.noop.NoopTaskProcessorFactory;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.notification.SESNotificationManager;
import org.duracloud.mill.workman.MultiStepTaskProcessorFactory;
import org.duracloud.mill.workman.RootTaskProcessorFactory;
import org.duracloud.mill.workman.TaskWorkerFactoryImpl;
import org.duracloud.mill.workman.TaskWorkerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

/**
 * 
 * @author Daniel Bernstein
 *	       Date: Oct 24, 2013
 */
@ComponentScan(basePackages = { "org.duracloud.mill" })
@Configuration
@ImportResource("classpath:/jpa-config.xml")
public class AppConfig {
    
    private static Logger log = LoggerFactory.getLogger(AppConfig.class);
    
    @Bean
    public RootTaskProcessorFactory 
                rootTaskProcessorFactory(@Qualifier("credentialsRepo") CredentialsRepo repo,
                                         StorageProviderFactory storageProviderFactory,
                                         File workDir,
                                         BitIntegrityCheckTaskProcessorFactory bitIntegrityCheckTaskProcessorFactory,
                                         BitIntegrityReportTaskProcessorFactory bitIntegrityReportTaskProcessorFactory,
                                         MultiStepTaskProcessorFactory auditTaskProcessorFactory,
                                         WorkmanConfigurationManager configurationManager) {

        RootTaskProcessorFactory factory = new RootTaskProcessorFactory();
        factory.addTaskProcessorFactory(
            new DuplicationTaskProcessorFactory(repo,
                                                storageProviderFactory,
                                                workDir,
                                                auditQueue(configurationManager)));
        factory.addTaskProcessorFactory(auditTaskProcessorFactory);
        factory.addTaskProcessorFactory(bitIntegrityCheckTaskProcessorFactory);
        factory.addTaskProcessorFactory(bitIntegrityReportTaskProcessorFactory);
        factory.addTaskProcessorFactory(new NoopTaskProcessorFactory(repo,
                workDir));

        log.info("RootTaskProcessorFactory created.");
        return factory;
    }

    @Bean
    public BitIntegrityCheckTaskProcessorFactory bitIntegrityCheckTaskProcessorFactory(
        @Qualifier("credentialsRepo") CredentialsRepo credentialRepo,
        StorageProviderFactory storageProviderFactory,
        ContentIndexClient contentIndexClient,
        AuditLogStore auditLogStore,
        BitLogStore bitLogStore,
        TaskQueue bitErrorQueue,
        TaskQueue auditQueue) {

        return new BitIntegrityCheckTaskProcessorFactory(credentialRepo,
                                                         storageProviderFactory,
                                                         contentIndexClient,
                                                         auditLogStore,
                                                         bitLogStore,
                                                         bitErrorQueue,
                                                         auditQueue);
    }

    @Bean
    public BitIntegrityReportTaskProcessorFactory bitIntegrityReportProcessorFactory(
        @Qualifier("credentialsRepo") CredentialsRepo credentialRepo,
        BitLogStore bitLogStore) {

        return new BitIntegrityReportTaskProcessorFactory(credentialRepo,
                                                         bitLogStore);
    }

    @Bean 
    public MultiStepTaskProcessorFactory auditTaskProcessorFactory(ContentIndexClient contentIndexClient,
                                                                    AuditLogStore auditLogStore,
                                                                    TaskQueue duplicationQueue, 
                                                                    DuplicationPolicyManager policyManager,
                                                                    NotificationManager notificationManager){

        MultiStepTaskProcessorFactory factory = new MultiStepTaskProcessorFactory();
        factory.addFactory(new AuditLogWritingProcessorFactory(auditLogStore));
        factory.addFactory(new ContentIndexUpdatingProcessorFactory(
                contentIndexClient));
        factory.addFactory(new DuplicationTaskProducingProcessorFactory(duplicationQueue, 
                                                                        policyManager));
        factory.addFactory(new SpaceCreatedNotifcationGeneratingProcessorFactory(
                                notificationManager));
        return factory;
    }

    @Bean(name="credentialsRepo")
    public CredentialsRepo credentialRepo(ConfigurationManager configurationManager, DuracloudAccountRepo accountRepo) {
        String path = configurationManager.getCredentialsFilePath();
        if(path != null){
            log.info(
                    "found credentials file path ({}): using config file based credential repo...",
                    path);
            return new ConfigFileCredentialRepo();
        }else{
            log.info("no credentials file path: using simpledb based credential repo...");
            return new DefaultCredentialsRepoImpl(accountRepo);
        }
    }

    @Bean
    StorageProviderFactory storageProviderFactory() {
        return new StorageProviderFactory();
    }

    @Bean 
    public ContentIndexClient contentIndexClient(WorkmanConfigurationManager config){
        return ESContentIndexClientUtil.createContentIndexClient();
    }

    @Bean
    public AmazonDynamoDBClient dynamoDBClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.setRegion(Region.getRegion(Regions.US_EAST_1));
        return client;
    }

    @Bean
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDBClient dynamoDBClient) {
        return new DynamoDBMapper(dynamoDBClient);
    }

    @Bean
    public AuditLogStore auditLogStore(AmazonDynamoDBClient dynamoDBClient,
                                       DynamoDBMapper dynamoDBMapper){
        DynamoDBAuditLogStore store =  new DynamoDBAuditLogStore();
        store.initialize(dynamoDBClient, dynamoDBMapper);
        return store;
    }

    @Bean
    public BitLogStore bitLogStore(AmazonDynamoDBClient dynamoDBClient,
                                   DynamoDBMapper dynamoDBMapper) {
        DynamoDBBitLogStore store = new DynamoDBBitLogStore();
        store.initialize(dynamoDBClient, dynamoDBMapper);
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
    public TaskQueue bitErrorQueue(WorkmanConfigurationManager configurationManager){
        TaskQueue queue =  new SQSTaskQueue(configurationManager.getBitErrorQueueName());
        log.info("created bit error queue {}", queue);
        return queue;
    }

    
    @Bean
    public TaskQueue duplicationQueue(WorkmanConfigurationManager configurationManager){
        TaskQueue queue =  new SQSTaskQueue(configurationManager.getHighPriorityDuplicationQueueName());
        log.info("created duplication queue {}", queue);
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
    
    @Bean
    public DuplicationPolicyManager duplicationPolicyManager(
            WorkmanConfigurationManager configurationManager){

        DuplicationPolicyRepo policyRepo;
        String policyDir = configurationManager.getDuplicationPolicyDir();
        
        if(policyDir != null) {
            policyRepo = new LocalDuplicationPolicyRepo(
                            policyDir);
        }else{
            String suffix = configurationManager.getPolicyBucketSuffix();
            if(suffix != null){
                policyRepo = new S3DuplicationPolicyRepo(
                    suffix);
            } else {
                policyRepo = new S3DuplicationPolicyRepo();
            }
        }        

        return new DuplicationPolicyManager(policyRepo);

    }

    @Bean (initMethod="init", destroyMethod="destroy")
    public DuplicationPolicyRefresher duplicationPolicyRefresh(WorkmanConfigurationManager workmanConfigurationManager, DuplicationPolicyManager policyManager){
        return new DuplicationPolicyRefresher(
                Long.valueOf(workmanConfigurationManager
                        .getPolicyManagerRefreshFrequencyMs()), policyManager);
    }
    @Bean
    public NotificationManager notificationManager(WorkmanConfigurationManager configurationManager) {
        String[] recipients = configurationManager.getNotificationRecipients();
        SESNotificationManager manager = new SESNotificationManager(recipients);
        return manager;
    }

    
}
