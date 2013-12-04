package org.duracloud.mill.workman.spring;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.workman.TaskWorkerManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 
 * @author Daniel Bernstein
 *
 */

public class AppDriver {

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
        Option configFile = new Option("c", "config-file", true,
                "A properties file containing configuration info");
        configFile.setArgs(1);
        configFile.setArgName("file");
        options.addOption(configFile);

        Option maxWorkers = new Option(
                "w",
                "max-workers",
                true,
                "The max number of worker threads that can run at a time. " +
                "The default value is " + TaskWorkerManager.DEFAULT_POOL_SIZE +
                ". Setting with value will override the " +
                TaskWorkerManager.MAX_WORKER_PROPERTY_KEY +
                " if set in the configuration file.");
        maxWorkers.setArgs(1);
        maxWorkers.setArgName("count");
        options.addOption(maxWorkers);

        Option queueName = new Option(
                "q",
                "queue-name",
                true,
                "The name of the amazon sqs queue");
        queueName.setArgs(1);
        queueName.setRequired(true);
        queueName.setArgName("name");
        options.addOption(queueName);

        return options;
    }
    
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);

        String configPath = cmd.getOptionValue("c");

        if(configPath != null){
            System.setProperty(
                    ConfigurationManager.DURACLOUD_MILL_CONFIG_FILE_KEY,
                    configPath);
        }

        String workerCount = cmd.getOptionValue("w");
        if(workerCount != null){
            Integer.parseInt(workerCount);
            System.setProperty(TaskWorkerManager.MAX_WORKER_PROPERTY_KEY,
                               workerCount);
        }

        String queueName = cmd.getOptionValue("q");
        if(queueName != null){
            System.setProperty(ConfigurationManager.DUPLICATION_QUEUE_KEY,
                               queueName);
        }
        
        ApplicationContext context = 
                new AnnotationConfigApplicationContext(AppConfig.class);
    }
}
