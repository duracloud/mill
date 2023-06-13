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

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.duracloud.account.db.repo.DuracloudAccountRepo;
import org.duracloud.common.model.EmailerType;
import org.duracloud.common.queue.QueueType;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.common.queue.rabbitmq.RabbitmqTaskQueue;
import org.duracloud.mill.audit.AuditLogWritingProcessorFactory;
import org.duracloud.mill.audit.DuplicationTaskProducingProcessorFactory;
import org.duracloud.mill.audit.SpaceCreatedNotifcationGeneratingProcessorFactory;
import org.duracloud.mill.auditor.AuditLogStore;
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
import org.duracloud.mill.dup.repo.SwiftDuplicationPolicyRepo;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.manifest.ManifestWritingProcessorFactory;
import org.duracloud.mill.manifest.jpa.JpaManifestStore;
import org.duracloud.mill.noop.NoopTaskProcessorFactory;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.notification.SESNotificationManager;
import org.duracloud.mill.notification.SMTPNotificationManager;
import org.duracloud.mill.storagestats.SpaceStatsManager;
import org.duracloud.mill.storagestats.StorageStatsTaskProcessorFactory;
import org.duracloud.mill.workman.MultiStepTaskProcessorFactory;
import org.duracloud.mill.workman.RootTaskProcessorFactory;
import org.duracloud.mill.workman.TaskWorkerFactory;
import org.duracloud.mill.workman.TaskWorkerFactoryImpl;
import org.duracloud.mill.workman.TaskWorkerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Daniel Bernstein
 * Date: Oct 24, 2013
 */
@ComponentScan(basePackages = {"org.duracloud.mill", "org.duracloud.account.db.config"})
@Configuration
public class AppConfig {

    private static Logger log = LoggerFactory.getLogger(AppConfig.class);
    private Connection rabbitMqConnection = null;

    @Bean
    public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
        return configurer;
    }

    @Bean
    public RootTaskProcessorFactory rootTaskProcessorFactory(
        @Qualifier("credentialsRepo") CredentialsRepo repo,
        StorageProviderFactory storageProviderFactory,
        File workDir,
        BitIntegrityCheckTaskProcessorFactory bitCheckTaskProcessorFactory,
        @Qualifier("bitReportProcessorFactory") MultiStepTaskProcessorFactory bitReportTaskProcessorFactory,
        @Qualifier("auditTaskProcessorFactory") MultiStepTaskProcessorFactory auditTaskProcessorFactory,
        StorageStatsTaskProcessorFactory storageStatsTaskProcessorFactory,
        TaskProducerConfigurationManager configurationManager,
        ManifestStore manifestStore) {

        RootTaskProcessorFactory factory = new RootTaskProcessorFactory();
        factory.addTaskProcessorFactory(new DuplicationTaskProcessorFactory(repo,
                                                                            storageProviderFactory,
                                                                            workDir,
                                                                            auditQueue(configurationManager),
                                                                            manifestStore));
        factory.addTaskProcessorFactory(auditTaskProcessorFactory);
        factory.addTaskProcessorFactory(bitCheckTaskProcessorFactory);
        factory.addTaskProcessorFactory(bitReportTaskProcessorFactory);
        factory.addTaskProcessorFactory(storageStatsTaskProcessorFactory);
        factory.addTaskProcessorFactory(new NoopTaskProcessorFactory(repo,
                                                                     workDir));
        log.info("RootTaskProcessorFactory created.");
        return factory;
    }

    @Bean
    public BitIntegrityCheckTaskProcessorFactory bitIntegrityCheckTaskProcessorFactory(
        @Qualifier("credentialsRepo") CredentialsRepo credentialRepo,
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

    @Bean
    public StorageStatsTaskProcessorFactory storageStatsTaskProcessorFactory(
        @Qualifier("credentialsRepo") CredentialsRepo credentialRepo,
        StorageProviderFactory storageProviderFactory,
        SpaceStatsManager spaceStatsManager,
        JpaManifestItemRepo manifestItemRepo) {

        return new StorageStatsTaskProcessorFactory(credentialRepo,
                                                    storageProviderFactory,
                                                    spaceStatsManager,
                                                    manifestItemRepo);
    }

    @Bean(name = "bitReportProcessorFactory")
    public MultiStepTaskProcessorFactory bitReportProcessorFactory(
        @Qualifier("credentialsRepo") CredentialsRepo credentialRepo,
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
    public MultiStepTaskProcessorFactory auditTaskProcessorFactory(
        AuditLogStore auditLogStore,
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
    public CredentialsRepo credentialRepo(ConfigurationManager configurationManager,
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
    public TaskWorkerManager taskWorkerManager(WorkmanConfigurationManager config,
                                               RootTaskProcessorFactory factory,
                                               TaskQueue deadLetterQueue,
                                               TaskWorkerFactory taskWorkerFactory) {

        return new TaskWorkerManager(createTaskQueues(config),
                                     deadLetterQueue,
                                     taskWorkerFactory);
    }

    @Bean(destroyMethod = "destroy")
    public TaskWorkerFactory taskWorkerFactory(RootTaskProcessorFactory factory,
                                               TaskQueue deadLetterQueue) {
        return new TaskWorkerFactoryImpl(factory, deadLetterQueue);
    }

    private Boolean isRabbitmq(QueueType queueType) {
        return queueType == QueueType.RABBITMQ;
    }

    protected List<TaskQueue> createTaskQueues(WorkmanConfigurationManager configurationManager) {
        List<String> taskQueuesNames = configurationManager.getTaskQueueNames();
        List<TaskQueue> taskQueues = new LinkedList<>();
        QueueType queueType = configurationManager.getQueueType();
        String[] queueConfig = null;
        Connection mqConn = null;
        String rmqHost;
        Integer rmqPort;
        String rmqVhost;
        String rmqExchange;
        String rmqUser;
        String rmqPass;

        if (isRabbitmq(queueType)) {
            queueConfig = configurationManager.getRabbitmqConfig();
            rmqHost = queueConfig[0];
            rmqPort = Integer.parseInt(queueConfig[1]);
            rmqVhost = queueConfig[2];
            rmqUser = queueConfig[4];
            rmqPass = queueConfig[5];
            mqConn = getRabbitmqConnection(rmqHost, rmqPort, rmqVhost, rmqUser, rmqPass);
        }

        for (String taskQueueName : taskQueuesNames) {
            TaskQueue taskQueue;
            if (isRabbitmq(queueType)) {
                if (mqConn != null) {
                    rmqExchange = queueConfig[3];
                    taskQueue = new RabbitmqTaskQueue(mqConn, rmqExchange, taskQueueName.trim());
                } else {
                    break;
                }
            } else {
                taskQueue = new SQSTaskQueue(taskQueueName.trim());
            }
            taskQueues.add(taskQueue);
            log.info("created queue {}: priority = {}",
                     taskQueue.getName(),
                     taskQueues.size());
        }
        return taskQueues;
    }

    protected TaskQueue createTaskQueue(QueueType queueType,
                                        TaskProducerConfigurationManager configurationManager,
                                        String queueName) {
        TaskQueue taskQueue;
        if (isRabbitmq(queueType)) {
            String[] queueConfig = configurationManager.getRabbitmqConfig();
            String rmqHost = queueConfig[0];
            Integer rmqPort = Integer.parseInt(queueConfig[1]);
            String rmqVhost = queueConfig[2];
            String rmqExchange = queueConfig[3];
            String rmqUser = queueConfig[4];
            String rmqPass = queueConfig[5];
            Connection mqConn = getRabbitmqConnection(rmqHost, rmqPort, rmqVhost, rmqUser, rmqPass);
            if (mqConn != null) {
                taskQueue = new RabbitmqTaskQueue(mqConn, rmqExchange, queueName.trim());
            } else {
                return null;
            }
        } else {
            taskQueue = new SQSTaskQueue(queueName);
        }
        return taskQueue;
    }

    protected Connection getRabbitmqConnection(String host,
                                               Integer port,
                                               String vhost,
                                               String username,
                                               String password) {
        if ( rabbitMqConnection == null ) {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUsername(username);
                factory.setPassword(password);
                factory.setVirtualHost(vhost);
                factory.setHost(host);
                factory.setPort(port);
                Connection conn = factory.newConnection();
                rabbitMqConnection = conn;
                return conn;
            } catch (Exception e) {
                log.error("Not able to establish connection with RabbitMQ");
                return null;
            }
        } else {
            return rabbitMqConnection;
        }
    }

    @Bean
    public TaskQueue auditQueue(TaskProducerConfigurationManager configurationManager) {
        QueueType queueType = configurationManager.getQueueType();
        TaskQueue queue = createTaskQueue(queueType,
                                          configurationManager,
                                          configurationManager.getAuditQueueName());
        log.info("created audit queue {} of type: {}", queue.getName(), queueType.toString());
        return queue;
    }

    @Bean
    public TaskQueue bitErrorQueue(WorkmanConfigurationManager configurationManager) {
        QueueType queueType = configurationManager.getQueueType();
        TaskQueue queue = createTaskQueue(queueType,
                                          configurationManager,
                                          configurationManager.getBitErrorQueueName());
        log.info("created bit error queue {} of type: {}", queue.getName(), queueType.toString());
        return queue;
    }

    @Bean
    public TaskQueue bitIntegrityQueue(WorkmanConfigurationManager configurationManager) {
        QueueType queueType = configurationManager.getQueueType();
        TaskQueue queue = createTaskQueue(queueType,
                                          configurationManager,
                                          configurationManager.getBitIntegrityQueue());
        log.info("created bit integrity queue {} of type: {}", queue.getName(), queueType.toString());
        return queue;
    }

    @Bean
    public TaskQueue bitReportQueue(TaskProducerConfigurationManager configurationManager) {
        QueueType queueType = configurationManager.getQueueType();
        TaskQueue queue = createTaskQueue(queueType,
                                          configurationManager,
                                          configurationManager.getBitReportQueueName());
        log.info("created bit report queue {} of type: {}", queue.getName(), queueType.toString());
        return queue;
    }

    @Bean
    public TaskQueue duplicationQueue(WorkmanConfigurationManager configurationManager) {
        QueueType queueType = configurationManager.getQueueType();
        TaskQueue queue = createTaskQueue(queueType,
                                          configurationManager,
                                          configurationManager.getHighPriorityDuplicationQueueName());
        log.info("created duplication queue {} of type: {}", queue.getName(), queueType.toString());
        return queue;
    }

    @Bean
    public TaskQueue deadLetterQueue(WorkmanConfigurationManager configurationManager) {
        QueueType queueType = configurationManager.getQueueType();
        TaskQueue queue = createTaskQueue(queueType,
                                          configurationManager,
                                          configurationManager.getDeadLetterQueueName());
        log.info("created dead letter  queue {} of type: {}", queue.getName(), queueType.toString());
        return queue;
    }

    @Bean
    public TaskProducerConfigurationManager configurationManager() {
        log.info("creating the workman configuration manager...");
        return new WorkmanConfigurationManager();
    }

    @Bean
    public DuplicationPolicyManager duplicationPolicyManager(WorkmanConfigurationManager configurationManager) {

        DuplicationPolicyRepo policyRepo;
        String policyDir = configurationManager.getDuplicationPolicyDir();

        if ( policyDir != null ) {
            policyRepo = new LocalDuplicationPolicyRepo(policyDir);
        } else {
            String suffix = configurationManager.getPolicyBucketSuffix();
            String[] swiftConfig = configurationManager.getSwiftConfig();
            String swiftAccessKey = swiftConfig[0];
            String swiftSecretKey = swiftConfig[1];
            String swiftEndpoint = swiftConfig[2];
            String swiftSigner = swiftConfig[3];
            if ( suffix != null ) {
                if (swiftEndpoint != null && !swiftEndpoint.isEmpty()) {
                    policyRepo = new SwiftDuplicationPolicyRepo(
                        swiftAccessKey, swiftSecretKey, swiftEndpoint, swiftSigner, suffix
                    );
                } else {
                    policyRepo = new S3DuplicationPolicyRepo(suffix);
                }
            } else {
                if (swiftEndpoint != null && !swiftEndpoint.isEmpty()) {
                    policyRepo = new SwiftDuplicationPolicyRepo(
                        swiftAccessKey, swiftSecretKey, swiftEndpoint, swiftSigner
                    );
                } else {
                    policyRepo = new S3DuplicationPolicyRepo();
                }
            }
        }

        return new DuplicationPolicyManager(policyRepo);

    }

    @Bean(initMethod = "init", destroyMethod = "destroy")
    public DuplicationPolicyRefresher duplicationPolicyRefresh(WorkmanConfigurationManager workmanConfigurationManager,
                                                               DuplicationPolicyManager policyManager) {
        return new DuplicationPolicyRefresher(workmanConfigurationManager.getPolicyManagerRefreshFrequencyMs(),
                                              policyManager);
    }

    @Bean
    public NotificationManager notificationManager(TaskProducerConfigurationManager configurationManager) {
        String[] recipients = configurationManager.getNotificationRecipients();
        EmailerType emailerType = configurationManager.getEmailerType();
        NotificationManager manager;

        if (emailerType == EmailerType.SMTP) {
            manager = new SMTPNotificationManager(recipients, configurationManager);
        } else {
            manager = new SESNotificationManager(recipients);
        }

        return manager;
    }

}
