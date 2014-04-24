/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.io.File;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.cli.CommandLine;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.file.ConfigFileCredentialRepo;
import org.duracloud.mill.credentials.simpledb.SimpleDBCredentialsRepo;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.LocalDuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.S3DuplicationPolicyRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;

/**
 * A main class responsible for parsing command line arguments and launching the
 * Looping Task Producer.
 * 
 * @author Daniel Bernstein Date: Nov 4, 2013
 */
public class LoopingTaskProducerDriverSupport extends DriverSupport {
    private static Logger log = LoggerFactory.getLogger(LoopingTaskProducerDriverSupport.class);

    protected int maxTaskQueueSize;
    protected String stateFilePath;
    protected Frequency frequency;
    
    /**
     * 
     */
    public LoopingTaskProducerDriverSupport(CommandLineOptions options) {
        super(options);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.ltp.DriverSupport#executeImpl(org.apache.commons.cli
     * .CommandLine)
     */
    @Override
    protected void executeImpl(CommandLine cmd) {
        processConfigFileOption(cmd);
        maxTaskQueueSize = processMaxQueueSizeOption(cmd);
        stateFilePath = processStateFilePathOption(cmd);
        frequency = processFrequencyOption(cmd);
    }

    /**
     * @param cmd
     * @param maxTaskQueueSize
     * @param stateFilePath
     * @param frequency
     * @return
     */
    private LoopingDuplicationTaskProducer buildTaskProducer(CommandLine cmd,
            int maxTaskQueueSize,
            String stateFilePath,
            Frequency frequency) {
        LoopingTaskProducerConfigurationManager config = new LoopingTaskProducerConfigurationManager();
        config.init();

        CredentialsRepo credentialsRepo;

        if (config.getCredentialsFilePath() != null) {
            credentialsRepo = new ConfigFileCredentialRepo(
                    config.getCredentialsFilePath());
        } else {
            credentialsRepo = new SimpleDBCredentialsRepo(
                    new AmazonSimpleDBClient());
        }

        StorageProviderFactory storageProviderFactory = new StorageProviderFactory();

        DuplicationPolicyManager policyManager;
        if (config.getDuplicationPolicyDir() != null) {
            policyManager = new DuplicationPolicyManager(
                    new LocalDuplicationPolicyRepo(
                            config.getDuplicationPolicyDir()));
        } else {
            DuplicationPolicyRepo policyRepo;
            if (cmd.hasOption(DuplicationOptions.POLICY_BUCKET_SUFFIX)) {
                policyRepo = new S3DuplicationPolicyRepo(
                        cmd.getOptionValue(DuplicationOptions.POLICY_BUCKET_SUFFIX));
            } else {
                policyRepo = new S3DuplicationPolicyRepo();
            }
            policyManager = new DuplicationPolicyManager(policyRepo);
        }

        TaskQueue taskQueue = new SQSTaskQueue(
                config.getLowPriorityDuplicationQueue());

        CacheManager cacheManager = CacheManager.create();
        Cache cache = new Cache("contentIdCache", 100 * 1000, true, true,
                60 * 5, 60 * 5);
        cacheManager.addCache(cache);

        StateManager<DuplicationMorsel> stateManager = new StateManager<>(
                stateFilePath);

        LoopingDuplicationTaskProducer producer = new LoopingDuplicationTaskProducer(
                credentialsRepo, storageProviderFactory, policyManager,
                taskQueue, cache, stateManager, maxTaskQueueSize, frequency);
        return producer;
    }

    /**
     * @param cmd
     */
    private void processTaskQueueNameOption(CommandLine cmd) {
        String duplicationQueueName = cmd
                .getOptionValue(DuplicationOptions.DUPLICATION_QUEUE_OPTION);
        if (duplicationQueueName != null) {
            setSystemProperty(
                    LoopingTaskProducerConfigurationManager.LOW_PRIORITY_DUPLICATION_QUEUE_KEY,
                    duplicationQueueName);
        }
    }

    /**
     * @param cmd
     * @return
     */
    private Frequency processFrequencyOption(CommandLine cmd) {
        String frequencyStr = cmd
                .getOptionValue(CommandLineOptions.FREQUENCY_OPTION);
        if (frequencyStr == null) {
            frequencyStr = "1m";
        }

        Frequency frequency = null;
        try {
            frequency = new Frequency(frequencyStr);
            log.info("frequency = {}{}", frequency.getValue(),
                    frequency.getTimeUnitAsString());
        } catch (java.text.ParseException ex) {
            System.out.println("Frequency parameter is invalid: " + frequency
                    + " Please refer to usage for valid examples.");
            die();
        }
        return frequency;
    }

    /**
     * @param cmd
     * @return
     */
    private String processStateFilePathOption(CommandLine cmd) {
        String stateFilePath = cmd
                .getOptionValue(CommandLineOptions.STATE_FILE_PATH);
        if (stateFilePath != null) {
            File stateFile = new File(stateFilePath);
            if (!stateFile.exists()) {
                File parent = stateFile.getParentFile();
                parent.mkdirs();
                if (!parent.exists()) {
                    System.err.print("The state file's parent directory, \""
                            + stateFilePath + "\", does not exist.");
                    die();
                }
            }
        }

        log.info("state file path = {}", stateFilePath);
        return stateFilePath;
    }

    /**
     * @param cmd
     * @return
     */
    private int processMaxQueueSizeOption(CommandLine cmd) {
        String maxTaskQueueSizeOption = cmd
                .getOptionValue(CommandLineOptions.MAX_TASK_QUEUE_SIZE_OPTION);
        int maxTaskQueueSize = 10 * 1000;

        if (maxTaskQueueSizeOption != null) {
            maxTaskQueueSize = Integer.valueOf(maxTaskQueueSizeOption);
        }

        log.info("max task queue size: {}", maxTaskQueueSize);

        return maxTaskQueueSize;
    }

    /**
     * @param cmd
     */
    private void processLocalDuplicationDirOption(CommandLine cmd) {
        String localDuplicationPolicyDirPath = cmd
                .getOptionValue(DuplicationOptions.LOCAL_DUPLICATION_DIR_OPTION);
        if (localDuplicationPolicyDirPath != null) {
            if (!new File(localDuplicationPolicyDirPath).exists()) {
                System.err.print("The local duplication policy directory "
                        + "path you specified, "
                        + localDuplicationPolicyDirPath + " does not exist: ");
                die();
            } else {
                setSystemProperty(
                        TaskProducerConfigurationManager.DUPLICATION_POLICY_DIR_KEY,
                        localDuplicationPolicyDirPath);
            }

            log.info("local duplication policy directory: {}",
                    localDuplicationPolicyDirPath);
        }
    }

    /**
     * @param cmd
     */
    private void processConfigFileOption(CommandLine cmd) {
        String configPath = cmd
                .getOptionValue(CommandLineOptions.CONFIG_FILE_OPTION);

        if (configPath != null) {
            setSystemProperty(
                    ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
                    configPath);
        }
    }
}
