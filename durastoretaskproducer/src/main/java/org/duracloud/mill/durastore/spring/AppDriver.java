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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Daniel Bernstein
 *	       Date: Oct 30, 2013
 */
public class AppDriver {

    private static final String CONFIG_FILE_OPTION = "c";
    private static final String LOCAL_DUPLICATION_FILE_OPTION = "l";

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

        Option duplicationQueueName = new Option("d", "duplication-queue", true,
                "Name of the duplication queue.");
        duplicationQueueName.setArgs(1);
        duplicationQueueName.setArgName("name");
        options.addOption(duplicationQueueName);

        Option localDuplicationFile = new Option(LOCAL_DUPLICATION_FILE_OPTION, "local-duplication-policy", true,
                "Indicates that a local duplication policy file should be used.");
        localDuplicationFile.setArgs(1);
        localDuplicationFile.setArgName("file");
        options.addOption(localDuplicationFile);

        return options;
    }
    
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);

        String configPath = cmd.getOptionValue(CONFIG_FILE_OPTION);

        if(configPath != null){
            System.setProperty(
                    ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
                    configPath);
        }

        String localDuplicationPolicyFile = cmd.getOptionValue(LOCAL_DUPLICATION_FILE_OPTION);
        if(localDuplicationPolicyFile != null){
            System.setProperty(DurastoreTaskProducerConfigurationManager.DUPLICATION_POLICY_FILE_KEY, localDuplicationPolicyFile);
            if(!new File(localDuplicationPolicyFile).exists()){
                System.err.print("The local duplication policy file you specified, "+localDuplicationPolicyFile + " does not exist: ");
                die();
            }

        }

        
        ApplicationContext context = 
                new AnnotationConfigApplicationContext(AppConfig.class);
    }
}
