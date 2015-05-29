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
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.mill.audit.AuditLogWritingProcessorFactory;
import org.duracloud.mill.audit.DuplicationTaskProducingProcessorFactory;
import org.duracloud.mill.audit.SpaceCreatedNotifcationGeneratingProcessorFactory;
import org.duracloud.mill.auditor.jpa.JpaAuditLogStore;
import org.duracloud.mill.bit.BitIntegrityCheckTaskProcessorFactory;
import org.duracloud.mill.bit.BitIntegrityReportTaskProcessorFactory;
import org.duracloud.mill.bit.SpaceComparisonTaskProcessorFactory;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.bitlog.jpa.JpaBitLogItemRepo;
import org.duracloud.mill.bitlog.jpa.JpaBitLogStore;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.impl.DefaultCredentialsRepoImpl;
import org.duracloud.mill.db.repo.JpaAuditLogItemRepo;
import org.duracloud.mill.db.repo.JpaBitIntegrityReportRepo;
import org.duracloud.mill.db.repo.JpaManifestItemRepo;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationPolicyRefresher;
import org.duracloud.mill.dup.DuplicationTaskProcessorFactory;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.LocalDuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.S3DuplicationPolicyRepo;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.manifest.ManifestWritingProcessorFactory;
import org.duracloud.mill.manifest.jpa.JpaManifestStore;
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
/**
 * 
 * @author Daniel Bernstein Date: Oct 24, 2013
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
                                     BitIntegrityCheckTaskProcessorFactory bitCheckTaskProcessorFactory,
                                     @Qualifier("bitReportProcessorFactory") 
                                         MultiStepTaskProcessorFactory bitReportTaskProcessorFactory,
                                     @Qualifier("auditTaskProcessorFactory") 
                                         MultiStepTaskProcessorFactory auditTaskProcessorFactory,
                                     TaskProducerConfigurationManager configurationManager) {

        RootTaskProcessorFactory factory = new RootTaskProcessorFactory();
        factory.addTaskProcessorFactory(new DuplicationTaskProcessorFactory(repo,
                                                                            storageProviderFactory,
                                                                            workDir,
                                                                            auditQueue(configurationManager)));
        factory.addTaskProcessorFactory(auditTaskProcessorFactory);
        factory.addTaskProcessorFactory(bitCheckTaskProcessorFactory);
        factory.addTaskProcessorFactory(bitReportTaskProcessorFactory);
        factory.addTaskProcessorFactory(new NoopTaskProcessorFactory(repo,
                                                                     workDir));
        log.info("RootTaskProcessorFactory created.");
        return factory;
    }

    @Bean
    public BitIntegrityCheckTaskProcessorFactory
            bitIntegrityCheckTaskProcessorFactory(@Qualifier("credentialsRepo") CredentialsRepo credentialRepo,
                                                  StorageProviderFactory storageProviderFactory,
                                                  BitLogStore bitLogStore,
                                                  TaskQueue bitErrorQueue,
                                                  TaskQueue auditQueue,
                                                  ManifestStore manifestStore) {

        return new BitIntegrityCheckTaskProcessorFactory(credentialRepo,
                                                         storageProviderFactory,
                                                         bitLogStore,
                                                         bitErrorQueue,
                                                         auditQueue,
                                                         manifestStore);
    }

    @Bean(name="bitReportProcessorFactory")
    public MultiStepTaskProcessorFactory
            bitReportProcessorFactory(@Qualifier("credentialsRepo") CredentialsRepo credentialRepo,
                                      ManifestStore manifestStore,
                                      StorageProviderFactory storageProviderFactory,
                                      BitLogStore bitLogStore,
                                      TaskQueue bitErrorQueue,
                                      TaskProducerConfigurationManager config,
                                      NotificationManager notificationManager) {

        MultiStepTaskProcessorFactory factory = new MultiStepTaskProcessorFactory();
        factory.addFactory(new SpaceComparisonTaskProcessorFactory(credentialRepo,
                                                                   storageProviderFactory,
                                                                   bitLogStore,
                                                                   bitErrorQueue,
                                                                   manifestStore));
        factory.addFactory(new BitIntegrityReportTaskProcessorFactory(credentialRepo,
                                                                      bitLogStore,
                                                                      storageProviderFactory,
                                                                      config, 
                                                                      notificationManager));
        return factory;
    }

    @Bean
    public MultiStepTaskProcessorFactory
            auditTaskProcessorFactory(AuditLogStore auditLogStore,
                                      TaskQueue duplicationQueue,
                                      DuplicationPolicyManager policyManager,
                                      NotificationManager notificationManager,
                                      ManifestStore manifestStore) {

        MultiStepTaskProcessorFactory factory = new MultiStepTaskProcessorFactory();
        factory.addFactory(new AuditLogWritingProcessorFactory(auditLogStore));
        factory.addFactory(new ManifestWritingProcessorFactory(manifestStore));
        factory.addFactory(new DuplicationTaskProducingProcessorFactory(duplicationQueue,
                                                                        policyManager));
        factory.addFactory(new SpaceCreatedNotifcationGeneratingProcessorFactory(notificationManager));
        return factory;
    }

    @Bean(name = "credentialsRepo")
    public CredentialsRepo
            credentialRepo(ConfigurationManager configurationManager,
                           DuracloudAccountRepo accountRepo) {
        return new DefaultCredentialsRepoImpl(accountRepo);
    }

    @Bean
    StorageProviderFactory storageProviderFactory() {
        return new StorageProviderFactory();
    }

    @Bean
    public AuditLogStore auditLogStore(JpaAuditLogItemRepo auditLogItemRepo) {
        return new JpaAuditLogStore(auditLogItemRepo);
    }

    @Bean
    public ManifestStore manifestStore(JpaManifestItemRepo manifestItemRepo) {
        return new JpaManifestStore(manifestItemRepo);
    }

    @Bean
    public BitLogStore bitLogStore(JpaBitLogItemRepo bitLogRepo, JpaBitIntegrityReportRepo reportRep) {
        return new JpaBitLogStore(bitLogRepo, reportRep);
    }

    @Bean
    public File workDir(TaskProducerConfigurationManager configurationManager) {
        log.info("creating work dir for path: "
                + configurationManager.getWorkDirectoryPath());
        return new File(configurationManager.getWorkDirectoryPath());
    }

    @Bean(initMethod = "init", destroyMethod = "destroy")
    public TaskWorkerManager
            taskWorkerManager(WorkmanConfigurationManager config,
                              RootTaskProcessorFactory factory,
                              TaskQueue deadLetterQueue) {
        return new TaskWorkerManager(createTaskQueues(config),
                                     deadLetterQueue,
                                     new TaskWorkerFactoryImpl(factory,
                                                               deadLetterQueue));
    }

    protected List<TaskQueue>
            createTaskQueues(WorkmanConfigurationManager configurationManager) {
        List<String> taskQueuesNames = configurationManager.getTaskQueueNames();
        List<TaskQueue> taskQueues = new LinkedList<>();
        for (String taskQueueName : taskQueuesNames) {
            TaskQueue taskQueue = new SQSTaskQueue(taskQueueName.trim());
            taskQueues.add(taskQueue);
            log.info("created queue {}: priority = {}",
                     taskQueue.getName(),
                     taskQueues.size());
        }
        return taskQueues;
    }

    @Bean
    public TaskQueue
            auditQueue(TaskProducerConfigurationManager configurationManager) {
        TaskQueue queue = new SQSTaskQueue(configurationManager.getAuditQueueName());
        log.info("created audit queue {}", queue);
        return queue;
    }

    @Bean
    public TaskQueue
            bitErrorQueue(WorkmanConfigurationManager configurationManager) {
        TaskQueue queue = new SQSTaskQueue(configurationManager.getBitErrorQueueName());
        log.info("created bit error queue {}", queue);
        return queue;
    }

    @Bean
    public TaskQueue
            bitReportQueue(TaskProducerConfigurationManager configurationManager) {
        TaskQueue queue = new SQSTaskQueue(configurationManager.getBitReportQueueName());
        log.info("created bit report queue {}", queue);
        return queue;
    }

    @Bean
    public TaskQueue
            duplicationQueue(WorkmanConfigurationManager configurationManager) {
        TaskQueue queue = new SQSTaskQueue(configurationManager.getHighPriorityDuplicationQueueName());
        log.info("created duplication queue {}", queue);
        return queue;
    }

    @Bean
    public TaskQueue
            deadLetterQueue(WorkmanConfigurationManager configurationManager) {
        TaskQueue queue = new SQSTaskQueue(configurationManager.getDeadLetterQueueName());
        log.info("created dead letter  queue {}", queue);
        return queue;
    }

    @Bean
    public TaskProducerConfigurationManager configurationManager() {
        log.info("creating the workman configuration manager...");
        return new WorkmanConfigurationManager();
    }

    @Bean
    public DuplicationPolicyManager
            duplicationPolicyManager(WorkmanConfigurationManager configurationManager) {

        DuplicationPolicyRepo policyRepo;
        String policyDir = configurationManager.getDuplicationPolicyDir();

        if (policyDir != null) {
            policyRepo = new LocalDuplicationPolicyRepo(policyDir);
        } else {
            String suffix = configurationManager.getPolicyBucketSuffix();
            if (suffix != null) {
                policyRepo = new S3DuplicationPolicyRepo(suffix);
            } else {
                policyRepo = new S3DuplicationPolicyRepo();
            }
        }

        return new DuplicationPolicyManager(policyRepo);

    }

    @Bean(initMethod = "init", destroyMethod = "destroy")
    public DuplicationPolicyRefresher
            duplicationPolicyRefresh(WorkmanConfigurationManager workmanConfigurationManager,
                                     DuplicationPolicyManager policyManager) {
        return new DuplicationPolicyRefresher(Long.valueOf(workmanConfigurationManager
                                                      .getPolicyManagerRefreshFrequencyMs()),
                                              policyManager);
    }

    @Bean
    public NotificationManager
            notificationManager(TaskProducerConfigurationManager configurationManager) {
        String[] recipients = configurationManager.getNotificationRecipients();
        SESNotificationManager manager = new SESNotificationManager(recipients);
        return manager;
    }

}
