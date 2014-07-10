/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import java.io.File;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.cli.CommandLine;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.file.ConfigFileCredentialRepo;
import org.duracloud.mill.credentials.impl.CredentialsRepoLocator;
import org.duracloud.mill.credentials.impl.DefaultCredentialsRepoImpl;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.LocalDuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.S3DuplicationPolicyRepo;
import org.duracloud.mill.ltp.Frequency;
import org.duracloud.mill.ltp.LoopingTaskProducerConfigurationManager;
import org.duracloud.mill.ltp.LoopingTaskProducerDriverSupport;
import org.duracloud.mill.ltp.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A main class responsible for parsing command line arguments and launching the
 * Looping Task Producer.
 * 
 * @author Daniel Bernstein Date: Nov 4, 2013
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

            LoopingDuplicationTaskProducer producer = buildTaskProducer(cmd,
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
            credentialsRepo = CredentialsRepoLocator.get();
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
                config.getOutputQueue());

        CacheManager cacheManager = CacheManager.create();
        Cache cache = new Cache("contentIdCache", 100 * 1000, true, true,
                60 * 5, 60 * 5);
        cacheManager.addCache(cache);

        StateManager<DuplicationMorsel> stateManager = new StateManager<>(
                stateFilePath, DuplicationMorsel.class);

        LoopingDuplicationTaskProducer producer = new LoopingDuplicationTaskProducer(
                credentialsRepo, storageProviderFactory, policyManager,
                taskQueue, cache, stateManager, maxTaskQueueSize, frequency);
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
