package org.duracloud.mill.workman.spring;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.util.SystemPropertyLoader;
import org.duracloud.mill.workman.TaskWorkerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 
 * @author Daniel Bernstein
 *
 */

public class AppDriver {

    /**
     * 
     */
    private static final String CONFIG_FILE_OPTION = "c";
    /**
     * 
     */
    private static final String WORK_DIR_PATH_OPTION = "d";
    private static final Logger log = LoggerFactory.getLogger(AppDriver.class);
    private static final String MAX_WORKERS_OPTION = "w";
    private static final String TASK_QUEUES_OPTION = "q";
    private static final String DEAD_LETTER_QUEUE_OPTION = "e";
    private static final String AUDIT_QUEUE_OPTION = "a";
    private static final String DUPLICATION_QUEUE_OPTION = "D";
    private static final String LOCAL_DUPLICATION_DIR_OPTION = "l";
    private static final String POLICY_BUCKET_SUFFIX = "p";
    private static final String NOTIFICATION_RECIPIENTS_OPTION = "n";
    private static final String REFRESH_OPTION = "r";
    private static final String BIT_ERROR_QUEUE_OPTION = "b";
    
    public static final long DEFAULT_POLICY_UPDATE_FREQUENCY_MS = 5*60*1000;


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

        Option maxWorkers = new Option(
                MAX_WORKERS_OPTION,
                "max-workers",
                true,
                "The max number of worker threads that can run at a time. " +
                "The default value is " + TaskWorkerManager.DEFAULT_MAX_WORKERS +
                ". Setting with value will override the " +
                TaskWorkerManager.MAX_WORKER_PROPERTY_KEY +
                " if set in the configuration file.");
        maxWorkers.setArgs(1);
        maxWorkers.setArgName("count");
        options.addOption(maxWorkers);

        Option taskQueues = new Option(
                TASK_QUEUES_OPTION,
                "task-queue-names",
                true,
                "A comma-separated prioritized list of amazon sqs queues where the first is highest.");
        taskQueues.setArgs(1);
        taskQueues.setRequired(true);
        taskQueues.setArgName("name");
        options.addOption(taskQueues);
        

        Option deadLetterQueue = new Option(
                DEAD_LETTER_QUEUE_OPTION,
                "dead-letter-queue-name",
                true,
                "The name of the dead letter amazon sqs queue");
        deadLetterQueue.setArgs(1);
        deadLetterQueue.setRequired(true);
        deadLetterQueue.setArgName("name");
        options.addOption(deadLetterQueue);

        Option auditQueue = new Option(
                AUDIT_QUEUE_OPTION,
                "audit-queue-name",
                true,
                "The name of the audit amazon sqs queue");
        auditQueue.setArgs(1);
        auditQueue.setRequired(true);
        auditQueue.setArgName("name");
        options.addOption(auditQueue);

        Option bitErrorQueue = new Option(
                BIT_ERROR_QUEUE_OPTION,
                "bit-error-queue-name",
                true,
                "The name of the bit error amazon sqs queue");
        bitErrorQueue.setArgs(1);
        bitErrorQueue.setRequired(true);
        bitErrorQueue.setArgName("name");
        options.addOption(bitErrorQueue);
        
        
        Option workDirPath = new Option(WORK_DIR_PATH_OPTION, "work-dir", true,
                                        "Directory that will be used to " +
                                        "temporarily store files as they " +
                                        "are being processed.");
        workDirPath.setArgs(1);
        workDirPath.setRequired(true);
        options.addOption(workDirPath);

        Option duplicationQueue = new Option(DUPLICATION_QUEUE_OPTION, "high-priority-queue-name", true,
                "The name of the sqs high priority duplication queue");
        duplicationQueue.setArgs(1);
        duplicationQueue.setRequired(true);
        options.addOption(duplicationQueue);
        
        Option localDuplicationDir =
            new Option(LOCAL_DUPLICATION_DIR_OPTION, "local-duplication-dir",
                       true, "Indicates that a local duplication policy " +
                             "directory should be used.");
        localDuplicationDir.setArgs(1);
        localDuplicationDir.setArgName("file");
        options.addOption(localDuplicationDir);

        Option policyBucketSuffix =
                new Option(POLICY_BUCKET_SUFFIX,
                           "policy-bucket-suffix",
                           true,
                           "The last portion of the name of the S3 bucket where " +
                           "duplication policies can be found.");
            policyBucketSuffix.setRequired(false);
            options.addOption(policyBucketSuffix);

        Option notificationRecipients =
                new Option(NOTIFICATION_RECIPIENTS_OPTION,
                           "notification-recipients",
                           true,
                           "A comma-separated list of email addresses");
        notificationRecipients.setRequired(false);
        options.addOption(notificationRecipients);

        Option refreshFrequency =
                new Option(REFRESH_OPTION,
                           "policy-refresh-frequency-ms",
                           true,
                           "The frequency in milliseconds between refreshes of duplication policies.");
        refreshFrequency.setRequired(false);
        options.addOption(refreshFrequency);


        return options;
    }
    
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);

        String configPath = cmd.getOptionValue(CONFIG_FILE_OPTION);

        if(configPath != null){
            if(new File(configPath).exists()){
                try {
                    SystemPropertyLoader.load(configPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            setSystemProperty(
                    ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
                    configPath);
        }

        String workerCount = cmd.getOptionValue(MAX_WORKERS_OPTION);
        if (workerCount != null) {
            Integer.parseInt(workerCount);
            setSystemProperty(TaskWorkerManager.MAX_WORKER_PROPERTY_KEY,
                    workerCount);
        }


        String taskQueueNames = cmd
                .getOptionValue(TASK_QUEUES_OPTION);
        if (taskQueueNames != null) {
            setSystemProperty(
                    WorkmanConfigurationManager.TASK_QUEUES_KEY,
                    taskQueueNames);
        }

        String deadLetterQueueName = cmd
                .getOptionValue(DEAD_LETTER_QUEUE_OPTION);
        if (deadLetterQueueName != null) {
            setSystemProperty(
                    WorkmanConfigurationManager.DEAD_LETTER_QUEUE_KEY,
                    deadLetterQueueName);
        }

        String auditQueueName = cmd.getOptionValue(AUDIT_QUEUE_OPTION);
        if (auditQueueName != null) {
            setSystemProperty(WorkmanConfigurationManager.AUDIT_QUEUE_KEY,
                              auditQueueName);
        }

        String bitErrorQueueName = cmd.getOptionValue(BIT_ERROR_QUEUE_OPTION);
        if (bitErrorQueueName != null) {
            setSystemProperty(WorkmanConfigurationManager.BIT_ERROR_QUEUE_KEY,
                              bitErrorQueueName);
        }

        String workDirPath = cmd.getOptionValue(WORK_DIR_PATH_OPTION);
        if(workDirPath == null || workDirPath.trim() == ""){
            //this should never happen since workDirPath is required,
            //but I'll leave this in here as a sanity check.
            workDirPath = System.getProperty("java.io.tmpdir") + 
                            File.separator + "duplication-work";
        }

        setSystemProperty(ConfigurationManager.WORK_DIRECTORY_PATH_KEY, workDirPath);
        initializeWorkDir(workDirPath);
        
        
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
                    setSystemProperty(
                            WorkmanConfigurationManager.DUPLICATION_POLICY_DIR_KEY,
                        localDuplicationPolicyDirPath);
                }
            }
            
            String policyBucketSuffix =
                    cmd.getOptionValue(POLICY_BUCKET_SUFFIX);
            setSystemProperty(
                    WorkmanConfigurationManager.DUPLICATION_POLICY_BUCKET_SUFFIX,
                    policyBucketSuffix);
                
            String duplicationQueueName = cmd.getOptionValue(DUPLICATION_QUEUE_OPTION);
            setSystemProperty(
                    WorkmanConfigurationManager.HIGH_PRIORITY_DUPLICATION_QUEUE_KEY,
                    duplicationQueueName);

            String notificationRecipients = cmd.getOptionValue(NOTIFICATION_RECIPIENTS_OPTION);
            setSystemProperty(
                    WorkmanConfigurationManager.NOTIFICATION_RECIPIENTS,
                    notificationRecipients);

            String refreshMs = cmd.getOptionValue(REFRESH_OPTION);
            if(refreshMs == null){
                refreshMs = DEFAULT_POLICY_UPDATE_FREQUENCY_MS +"";
            }
            setSystemProperty(
                    WorkmanConfigurationManager.POLICY_MANAGER_REFRESH_FREQUENCY_MS,
                    refreshMs);

        ApplicationContext context = 
                new AnnotationConfigApplicationContext(AppConfig.class);
    }

    /**
     * @param workDirPath
     */
    private static void initializeWorkDir(String workDirPath) {

        try{
            File workDir = new File(workDirPath);

            if(!workDir.exists()) {
                if(!workDir.mkdirs()){
                    String message = "Unable to create work dir: "
                            + workDir.getAbsolutePath() +
                            ". Check that workman process has " +
                            "permission to create this directory";
                    log.error(message);
                    System.exit(1);
                }
            }
            
        }catch(Exception ex){
            log.error(
                    "failed to initialize workDir " + workDirPath + ":"
                            + ex.getMessage(), ex);
            System.exit(1);
        }
        
    }

    /**
     * A util method that logs the property set action before setting the
     * property.
     * 
     * @param key
     * @param value
     */
    private static void setSystemProperty(String key, String value) {
        log.info("Setting system property {} to {}", key, value);
        System.setProperty(key, value);
    }
}
