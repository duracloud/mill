/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.file.ConfigFileCredentialRepo;
import org.duracloud.mill.credentials.simpledb.SimpleDBCredentialsRepo;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducerCommandLineOptions;
import org.duracloud.mill.ltp.LoopingTaskProducerConfigurationManager;
import org.duracloud.mill.ltp.LoopingTaskProducerDriverSupport;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.ltp.dup.DuplicationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.rackspacecloud.client.cloudfiles.sample.FilesCli.CommandLineOptions;

/**
 * A main class responsible for parsing command line arguments and launching the
 * Looping Task Producer.
 * 
 * @author Daniel Bernstein Date: Nov 4, 2013
 */
public class AppDriver extends LoopingTaskProducerDriverSupport {
    private static Logger log = LoggerFactory.getLogger(AppDriver.class);

    /**
     * 
     */
    public AppDriver() {
        super(new LoopingTaskProducerCommandLineOptions());
    }

    public static void main(String[] args) {
        new AppDriver().execute(args);
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
        super.executeImpl(cmd);

        processLocalDuplicationDirOption(cmd);
        processTaskQueueNameOption(cmd);

        try {

            LoopingBitIntegrityTaskProducer producer = buildTaskProducer(cmd,
                                                                        maxTaskQueueSize, 
                                                                        stateFilePath, 
                                                                        frequency);
            producer.run();

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            System.exit(1);
        }

        log.info("looping task producer completed successfully.");
        System.exit(0);

    }

    /**
     * @param cmd
     * @param maxTaskQueueSize
     * @param stateFilePath
     * @param frequency
     * @return
     */
    private LoopingBitIntegrityTaskProducer buildTaskProducer(CommandLine cmd,
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


        TaskQueue taskQueue = new SQSTaskQueue(
                config.getOutputQueue());


        StateManager<BitIntegrityMorsel> stateManager = new StateManager<>(
                stateFilePath);

        LoopingBitIntegrityTaskProducer producer = new LoopingBitIntegrityTaskProducer(
                credentialsRepo, storageProviderFactory,
                taskQueue, stateManager, maxTaskQueueSize, frequency);
        return producer;
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

}
