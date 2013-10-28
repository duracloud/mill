package org.duracloud.mill.workman.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.mill.workman.ConfigurationManager;
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

//        Option credentialsFile = new Option("f",
//                                     "credentials-file",
//                                     true,
//                                     "A json file containing provider credential lists.");
//        credentialsFile.setArgs(1);
//        credentialsFile.setArgName("file");
//
//        options.addOption(credentialsFile);
        
        
        return options;
    }
    
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);

        String configPath = cmd.getOptionValue("c");

        if(configPath != null){
            System.setProperty(
                    ConfigurationManager.DURACLOUD_WORKMAN_CONFIG_FILE_KEY,
                    configPath);
        }
        
        ApplicationContext context = 
                new AnnotationConfigApplicationContext(AppConfig.class);
    }
}
