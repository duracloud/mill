/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore.spring;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.durastore.DurastoreTaskProducerConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


/**
 * A main class responsible for parsing command line arguments and launching the
 * DuraStore Task Producer service.
 * 
 * @author Daniel Bernstein Date: Oct 30, 2013
 */
public class AppDriver {
    private static final Logger log = LoggerFactory.getLogger(AppDriver.class);
    /**
     * 
     */
    private static final String DUPLICATION_QUEUE_OPTION = "d";
    private static final String CONFIG_FILE_OPTION = "c";
    private static final String LOCAL_DUPLICATION_DIR_OPTION = "l";
    private static final String POLICY_BUCKET_SUFFIX = "p";
    private static final String NOTIFICATION_RECIPIENTS_OPTION = "n";
    private static final String REFRESH_OPTION = "r";

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

        Option help = new Option("h", "help");
        options.addOption(help);

        Option configFile = new Option(CONFIG_FILE_OPTION, "config-file", true,
                "A properties file containing configuration info");
        configFile.setArgs(1);
        configFile.setArgName("file");
        options.addOption(configFile);

        Option duplicationQueueName = new Option(DUPLICATION_QUEUE_OPTION, "high-priority-duplication-queue", true,
                "Name of the high priority duplication queue.");
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

        if(cmd.hasOption("h")){
            die();
        }
        
        String configPath = cmd.getOptionValue(CONFIG_FILE_OPTION);
        setSystemProperty(
                ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
                configPath);

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
                    DurastoreTaskProducerConfigurationManager.
                        DUPLICATION_POLICY_DIR_KEY,
                    localDuplicationPolicyDirPath);
            }
        }
        
        String policyBucketSuffix =
                cmd.getOptionValue(POLICY_BUCKET_SUFFIX);
        setSystemProperty(
                DurastoreTaskProducerConfigurationManager.DUPLICATION_POLICY_BUCKET_SUFFIX,
                policyBucketSuffix);
            
        String duplicationQueueName = cmd.getOptionValue(DUPLICATION_QUEUE_OPTION);
        setSystemProperty(
                DurastoreTaskProducerConfigurationManager.HIGH_PRIORITY_DUPLICATION_QUEUE_KEY,
                duplicationQueueName);

        String notificationRecipients = cmd.getOptionValue(NOTIFICATION_RECIPIENTS_OPTION);
        setSystemProperty(
                DurastoreTaskProducerConfigurationManager.NOTIFICATION_RECIPIENTS,
                notificationRecipients);

        String refreshMs = cmd.getOptionValue(REFRESH_OPTION);
        setSystemProperty(
                DurastoreTaskProducerConfigurationManager.POLICY_MANAGER_REFRESH_FREQUENCY_MS,
                refreshMs);

        ApplicationContext context = 
                new AnnotationConfigApplicationContext(AppConfig.class);
    }
    
    private static void setSystemProperty(String name, String value){
        if(value != null){
            System.setProperty(name, value);
            log.info("set property:  {}={}", name, value);
        }
    }
}
