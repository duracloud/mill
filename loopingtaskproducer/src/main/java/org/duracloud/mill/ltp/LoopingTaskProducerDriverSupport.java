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
    public LoopingTaskProducerDriverSupport(LoopingTaskProducerCommandLineOptions options) {
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
     * @return
     */
    private String processStateFilePathOption(CommandLine cmd) {
        String stateFilePath = cmd
                .getOptionValue(LoopingTaskProducerCommandLineOptions.STATE_FILE_PATH);
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
                .getOptionValue(LoopingTaskProducerCommandLineOptions.MAX_TASK_QUEUE_SIZE_OPTION);
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
    protected void processConfigFileOption(CommandLine cmd) {
        String configPath = cmd
                .getOptionValue(LoopingTaskProducerCommandLineOptions.CONFIG_FILE_OPTION);

        if (configPath != null) {
            setSystemProperty(
                    ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
                    configPath);
        }
    }

    /**
     * @param cmd
     */
    protected void processTaskQueueNameOption(CommandLine cmd) {
        String outputQueueName = cmd
                .getOptionValue(LoopingTaskProducerCommandLineOptions.OUTPUT_QUEUE_OPTION);
        if (outputQueueName != null) {
            setSystemProperty(
                    LoopingTaskProducerConfigurationManager.OUTPUT_QUEUE_KEY,
                    outputQueueName);
        }
    }

    /**
     * @param cmd
     * @return
     */
    protected Frequency processFrequencyOption(CommandLine cmd) {
        String frequencyStr = cmd
                .getOptionValue(LoopingTaskProducerCommandLineOptions.FREQUENCY_OPTION);
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





}
