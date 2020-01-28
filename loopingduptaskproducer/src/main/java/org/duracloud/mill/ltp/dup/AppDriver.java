/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import java.io.File;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.common.queue.rabbitmq.RabbitMQTaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.impl.ApplicationContextLocator;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.LocalDuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.S3DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.SwiftDuplicationPolicyRepo;
import org.duracloud.mill.ltp.LoopingTaskProducer;
import org.duracloud.mill.ltp.LoopingTaskProducerConfigurationManager;
import org.duracloud.mill.ltp.LoopingTaskProducerDriverSupport;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.notification.SESNotificationManager;
import org.duracloud.mill.notification.SpringNotificationManager;
import org.duracloud.mill.util.PropertyDefinition;
import org.duracloud.mill.util.PropertyDefinitionListBuilder;
import org.duracloud.mill.util.PropertyVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A main class responsible for parsing command line arguments and launching the
 * Looping Task Producer.
 *
 * @author Daniel Bernstein
 * Date: Nov 4, 2013
 */
public class AppDriver extends LoopingTaskProducerDriverSupport {
    public static Logger log = LoggerFactory.getLogger(AppDriver.class);

    /**
     *
     */
    public AppDriver() {
        super(new DuplicationOptions());
    }

    public static void main(String[] args) {
        new AppDriver().execute(args);
    }

    @Override
    protected LoopingTaskProducer buildTaskProducer() {

        List<PropertyDefinition> defintions =
            new PropertyDefinitionListBuilder().addAws()
                                               .addNotifications()
                                               .addMcDb()
                                               .addDuplicationLowPriorityQueue()
                                               .addLoopingDupFrequency()
                                               .addLoopingDupMaxQueueSize()
                                               .addDuplicationPolicyBucketSuffix()
                                               .addLocalDuplicationDir()
                                               .addWorkDir()
                                               .build();
        PropertyVerifier verifier = new PropertyVerifier(defintions);
        verifier.verify(System.getProperties());

        LoopingTaskProducerConfigurationManager config = new LoopingTaskProducerConfigurationManager();
        processLocalDuplicationDirOption(config);

        CredentialsRepo credentialsRepo = ApplicationContextLocator.get().getBean(CredentialsRepo.class);

        StorageProviderFactory storageProviderFactory = new StorageProviderFactory();

        DuplicationPolicyManager policyManager;
        String policyDir = config.getDuplicationPolicyDir();
        if (policyDir != null) {
            policyManager = new DuplicationPolicyManager(
                new LocalDuplicationPolicyRepo(policyDir));
        } else {
            DuplicationPolicyRepo policyRepo;
            String bucketSuffix = config.getDuplicationPolicyBucketSuffix();
            if (bucketSuffix != null) {
                if (config.getAWSType() == "SWIFT") {
                    String[] swiftConfig = config.getSwiftConfig();
                    policyRepo = new SwiftDuplicationPolicyRepo(swiftConfig[0], swiftConfig[1],
                        swiftConfig[2], swiftConfig[3], swiftConfig[4], bucketSuffix);
                } else {
                    policyRepo = new S3DuplicationPolicyRepo(bucketSuffix);
                }
            } else {
                if (config.getAWSType() == "SWIFT") {
                    String[] swiftConfig = config.getSwiftConfig();
                    policyRepo = new SwiftDuplicationPolicyRepo(swiftConfig[0], swiftConfig[1],
                        swiftConfig[2], swiftConfig[3], swiftConfig[4]);
                } else {
                    policyRepo = new S3DuplicationPolicyRepo();
                }
            }
            policyManager = new DuplicationPolicyManager(policyRepo);
        }

        String queueType = config.getQueueType();
        TaskQueue taskQueue = null;
        if (config.getQueueType() == "RABBITMQ") {
            String[] queueConfig = config.getRabbitMQConfig();
            taskQueue = new RabbitMQTaskQueue(queueConfig[0], Integer.parseInt(queueConfig[1]), queueConfig[2],
                    queueConfig[3], queueConfig[4], queueConfig[5], getTaskQueueName(ConfigConstants.QUEUE_NAME_DUP_LOW_PRIORITY));
        } else {
            taskQueue = new SQSTaskQueue(getTaskQueueName(ConfigConstants.QUEUE_NAME_DUP_LOW_PRIORITY));
        }

        CacheManager cacheManager = CacheManager.create();
        CacheConfiguration cacheConfig = new CacheConfiguration();
        cacheConfig.setName("contentIdCache");
        cacheConfig.addPersistence(
            new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP));
        cacheConfig.setEternal(true);
        Cache cache = new Cache(cacheConfig);
        cacheManager.addCache(cache);

        String stateFilePath = new File(config.getWorkDirectoryPath(), "dup-producer-state.json").getAbsolutePath();
        StateManager<DuplicationMorsel> stateManager = new StateManager<>(stateFilePath, DuplicationMorsel.class);
        NotificationManager notificationMananger = null;
        String notificationType = config.getNotificationType();
        if (notificationType == "AWS") {
            notificationMananger =
                    new SESNotificationManager(config.getNotificationRecipients());
        } else if (notificationType == "SPRING") {
            notificationMananger =
                    new SpringNotificationManager(config.getNotificationRecipients(), config);
        }

        LoopingDuplicationTaskProducer producer =
            new LoopingDuplicationTaskProducer(credentialsRepo,
                                               storageProviderFactory,
                                               policyManager,
                                               taskQueue,
                                               cache,
                                               stateManager,
                                               getMaxQueueSize(ConfigConstants.LOOPING_DUP_MAX_TASK_QUEUE_SIZE),
                                               getFrequency(ConfigConstants.LOOPING_DUP_FREQUENCY),
                                               notificationMananger,
                                               config);
        return producer;
    }

    /**
     * @param config
     */
    private void processLocalDuplicationDirOption(TaskProducerConfigurationManager config) {
        String localDuplicationPolicyDirPath = config.getDuplicationPolicyDir();
        if (localDuplicationPolicyDirPath != null) {
            if (!new File(localDuplicationPolicyDirPath).exists()) {
                System.err.print("The local duplication policy directory "
                                 + "path you specified, "
                                 + localDuplicationPolicyDirPath + " does not exist: ");
                die();
            }
            log.info("local duplication policy directory: {}",
                     localDuplicationPolicyDirPath);
        }
    }
}
