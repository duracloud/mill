/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.aws.SQSTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * A main class responsible for parsing command line arguments and launching the
 * Looping Task Producer.
 
 * @author Daniel Bernstein
 *	       Date: Nov 4, 2013
 */
public class AppDriver {
    /**
     * 
     */
    private static final String DUPLICATION_QUEUE_OPTION = "d";
    private static Logger log = LoggerFactory.getLogger(AppDriver.class);
    private static final String CONFIG_FILE_OPTION = "c";
    private static final String LOCAL_DUPLICATION_DIR_OPTION = "l";
    private static final String MAX_TASK_QUEUE_SIZE_OPTION = "m";
    private static final String STATE_FILE_PATH = "s";
    private static final String POLICY_BUCKET_SUFFIX = "p";

    private static void usage() {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(80);
        help.printHelp(AppDriver.class.getCanonicalName(), getOptions());
    }
    
    private static CommandLine parseArgs(String[] args) {
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (ParseException e) {
            System.err.println(e);
            die();
        }
        return cmd;
    }
    
    private static void die() {
        usage();
        System.exit(1);
    }
    
    private static Options getOptions() {
        Options options = new Options();

        Option configFile = new Option(CONFIG_FILE_OPTION, "config-file", true,
                "A properties file containing configuration info");
        configFile.setArgs(1);
        configFile.setArgName("file");
        options.addOption(configFile);

        Option help = new Option("h", "help");
        options.addOption(help);

        Option duplicationQueueName = new Option(DUPLICATION_QUEUE_OPTION, "duplication-queue", true,
                "Name of the duplication queue.");
        duplicationQueueName.setArgs(1);
        duplicationQueueName.setArgName("name");
        options.addOption(duplicationQueueName);

        Option localDuplicationDir =
            new Option(LOCAL_DUPLICATION_DIR_OPTION, "local-duplication-dir",
                       true, "Indicates that a local duplication policy " +
                             "directory should be used.");
        localDuplicationDir.setArgs(1);
        localDuplicationDir.setArgName("file");
        options.addOption(localDuplicationDir);

        Option stateFile =
                new Option(STATE_FILE_PATH, "state-file-path",
                           true, "Indicates the path of file containing state info");
        stateFile.setArgs(1);
        stateFile.setRequired(true);
        stateFile.setArgName("file");
        options.addOption(stateFile);
        
        Option maxTaskQueueSize = new Option(
                MAX_TASK_QUEUE_SIZE_OPTION,
                "max-task-queue-size",
                true,
                "Indicates how large the task queue should be allowed to grow before the Looping Task Producer quits."
                        + "directory should be used.");
        maxTaskQueueSize.setArgs(1);
        maxTaskQueueSize.setArgName("integer");
        options.addOption(maxTaskQueueSize);

        Option policyBucketSuffix =
            new Option(POLICY_BUCKET_SUFFIX,
                       "policy-bucket-suffix",
                       true,
                       "The last portion of the name of the S3 bucket where " +
                       "duplication policies can be found.");
        policyBucketSuffix.setRequired(false);
        options.addOption(policyBucketSuffix);

        return options;
    }
    
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        
        if(cmd.hasOption("h")){
            die();
        }
        
        String configPath = cmd.getOptionValue(CONFIG_FILE_OPTION);

        if(configPath != null){
            System.setProperty(
                    ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
                    configPath);
        }

        String localDuplicationPolicyDirPath =
            cmd.getOptionValue(LOCAL_DUPLICATION_DIR_OPTION);
        if(localDuplicationPolicyDirPath != null){
            if(!new File(localDuplicationPolicyDirPath).exists()){
                System.err.print("The local duplication policy directory " +
                                 "path you specified, " +
                                 localDuplicationPolicyDirPath +
                                 " does not exist: ");
                die();
            } else {
                System.setProperty(
                    TaskProducerConfigurationManager.
                        DUPLICATION_POLICY_DIR_KEY,
                    localDuplicationPolicyDirPath);
            }
            
            log.info("local duplication policy directory: {}", localDuplicationPolicyDirPath);
        }
        
        String maxTaskQueueSizeOption = cmd.getOptionValue(MAX_TASK_QUEUE_SIZE_OPTION);
        int maxTaskQueueSize = 10*1000;
        
        if(maxTaskQueueSizeOption != null){
            maxTaskQueueSize = Integer.valueOf(maxTaskQueueSizeOption);
        }
        
        log.info("max task queue size: {}", maxTaskQueueSize);
        
        String stateFilePath = cmd.getOptionValue(STATE_FILE_PATH);
        if (stateFilePath != null) {
            File stateFile = new File(stateFilePath);
            if(!stateFile.exists()){
                File parent = stateFile.getParentFile();
                parent.mkdirs();
                if(!parent.exists()){
                    System.err.print("The state file's parent directory, \"" + stateFilePath + "\", does not exist.");
                    die();
                }
            }
        }

        log.info("state file path = {}", stateFilePath);
        
        String duplicationQueueName = cmd.getOptionValue(DUPLICATION_QUEUE_OPTION);
        if(duplicationQueueName != null){
            System.setProperty(
                    LoopingTaskProducerConfigurationManager.LOW_PRIORITY_DUPLICATION_QUEUE_KEY,
                    duplicationQueueName);
        }
        
        try{
            
            TaskProducerConfigurationManager config = new TaskProducerConfigurationManager();
            config.init();
            
            CredentialsRepo credentialsRepo;
            
            if(config.getCredentialsFilePath() != null){
                credentialsRepo = 
                        new ConfigFileCredentialRepo(config.getCredentialsFilePath());
            }else{
                credentialsRepo = 
                        new SimpleDBCredentialsRepo(new AmazonSimpleDBClient());
            }
            
            StorageProviderFactory storageProviderFactory = new StorageProviderFactory();
            
            DuplicationPolicyManager policyManager;
            if(config.getDuplicationPolicyDir() != null) {
                policyManager = new DuplicationPolicyManager(
                        new LocalDuplicationPolicyRepo(
                                config.getDuplicationPolicyDir()));
            }else{
                DuplicationPolicyRepo policyRepo;
                if(cmd.hasOption(POLICY_BUCKET_SUFFIX)) {
                    policyRepo = new S3DuplicationPolicyRepo(
                        cmd.getOptionValue(POLICY_BUCKET_SUFFIX));
                } else {
                    policyRepo = new S3DuplicationPolicyRepo();
                }
                policyManager = new DuplicationPolicyManager(policyRepo);
            }
            
            TaskQueue taskQueue = new SQSTaskQueue(config.getLowPriorityDuplicationQueue());
            
            CacheManager cacheManager = CacheManager.create();
            Cache cache = new Cache("contentIdCache", 100*1000, true, true, 60*5,60*5);
            cacheManager.addCache(cache);
            
            StateManager stateManager = new StateManager(stateFilePath);
            
            
            LoopingTaskProducer producer = new LoopingTaskProducer(credentialsRepo, 
                                                                   storageProviderFactory, 
                                                                   policyManager,
                                                                   taskQueue, 
                                                                   cache, 
                                                                   stateManager, 
                                                                   maxTaskQueueSize);
            producer.run();
            
        }catch(Exception ex){
            log.error(ex.getMessage(), ex);
            System.exit(1);
        }
        
        log.info("looping task producer completed successfully.");
        System.exit(0);

    }
}
