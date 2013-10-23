package org.duracloud.mill.workman.spring;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
        
        Option credentialsFile = new Option("f",
                                     "credentials-file",
                                     true,
                                     "A json file containing provider credential lists.");
        credentialsFile.setArgs(1);
        credentialsFile.setArgName("file");
        credentialsFile.setRequired(true);

        options.addOption(credentialsFile);
        
        return options;
    }
    
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        String filePath = cmd.getOptionValue("f");
        System.setProperty("credentials.file.path", filePath);
        
        if(!new File(filePath).exists()){
            System.err.print("Specified file " +  filePath + " not found.");
            die();
        }
        ApplicationContext context = 
                new AnnotationConfigApplicationContext(AppConfig.class);
    }
}
