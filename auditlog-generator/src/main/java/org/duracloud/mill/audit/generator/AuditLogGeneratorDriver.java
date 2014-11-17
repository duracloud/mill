/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.mill.db.util.MillJpaPropertiesVerifier;
import org.duracloud.mill.util.SystemPropertyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 
 * @author Daniel Bernstein
 *
 */

public class AuditLogGeneratorDriver {

    private static final Logger log = LoggerFactory.getLogger(AuditLogGeneratorDriver.class);
    private static final String CONFIG_FILE_OPTION = "c";

    private static void usage() {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(80);
        help.printHelp(AuditLogGeneratorDriver.class.getCanonicalName(), getOptions());
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

        Option configFile = new Option(CONFIG_FILE_OPTION,
                                       "config-file-path",
                                       true,
                                       "Path to the mill config file");
        configFile.setArgs(1);
        configFile.setArgName("path");
        configFile.setRequired(true);
        options.addOption(configFile);

        
        return options;
    }
    
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        SystemConfig config = SystemConfig.instance();
        
        String configPath = cmd.getOptionValue(CONFIG_FILE_OPTION);
        try {
            SystemPropertyLoader.load(configPath);
        } catch (IOException e) {
            log.error("failed to load config properties file " + configPath, e);
        }

        
        config.setAuditLogSpaceId(getSysProp("duracloud.audit.space", configPath));
        
        new MillJpaPropertiesVerifier().verify();
        
        String logRootDir = getSysProp("audit.log.root.dir", configPath);
        initializeLogRoot(logRootDir);
        ApplicationContext context = 
                new AnnotationConfigApplicationContext("org.duracloud.mill");
        log.info("spring context initialized.");
        AuditLogGenerator generator = context.getBean(AuditLogGenerator.class);
        generator.execute();
        log.info("exiting...");
    }

    /**
     * @param string
     * @return
     */
    private static String getSysProp(String systemPropKey, String filePath) {
        String value = System.getProperty(systemPropKey);
        if(value == null){
            throw new RuntimeException("property " + systemPropKey
                    + " must be set in " + filePath);
        }
        
        return value;
    }

    /**
     * @param logRootPath
     */
    private static void initializeLogRoot(String logRootPath) {

        try{
            File logRootDir = new File(logRootPath);

            if(!logRootDir.exists()) {
                if(!logRootDir.mkdirs()){
                    String message = "Unable to create log root dir: "
                            + logRootDir.getAbsolutePath() +
                            ". Please make sure that this process has " +
                            "permission to create this directory";
                    log.error(message);
                    System.exit(1);
                }
            }
            
            SystemConfig.instance().setLogsDirectory(logRootPath);
        }catch(Exception ex){
            log.error(
                    "failed to initialize log root dir " + logRootPath + ":"
                            + ex.getMessage(), ex);
            System.exit(1);
        }
        
    }


    private static void setSystemProperty(String key, String value) {
        log.info("Setting system property {} to {}", key, value);
        System.setProperty(key, value);
    }
}
