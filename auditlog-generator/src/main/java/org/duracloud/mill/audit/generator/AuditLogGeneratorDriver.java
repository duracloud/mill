/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.mill.config.ConfigurationManager;
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

    private static final String LOG_ROOT_DIR_OPTION = "d";
    private static final String DURACLOUD_USERNAME_OPTION = "u";
    private static final String DURACLOUD_PASSWORD_OPTION = "p";
    private static final String DURACLOUD_HOST_OPTION = "h";
    private static final String DURACLOUD_PORT_OPTION = "r";

    private static final String AUDIT_LOGS_SPACE_OPTION = "s";
    
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
        Option logRootOption = new Option(LOG_ROOT_DIR_OPTION, "log-root-dir", true,
                "Directory in which audit logs will be generated.");
        logRootOption.setArgs(1);
        logRootOption.setArgName("directory");
        logRootOption.setRequired(true);
        options.addOption(logRootOption);

        Option duracloudUsername = new Option(DURACLOUD_USERNAME_OPTION, "duracloud-username", true,
                "DuraCloud username with write access to the host and audit log space.");
        duracloudUsername.setArgs(1);
        duracloudUsername.setArgName("username");
        duracloudUsername.setRequired(true);
        options.addOption(duracloudUsername);
        
        Option duracloudPassword = new Option(DURACLOUD_PASSWORD_OPTION, "duracloud-password", true,
                "DuraCloud password.");
        duracloudPassword.setArgs(1);
        duracloudPassword.setArgName("password");
        duracloudPassword.setRequired(true);
        options.addOption(duracloudPassword);
        
        Option duracloudHost = new Option(DURACLOUD_HOST_OPTION, "duracloud-host", true,
                "DuraCloud Host");
        duracloudHost.setArgs(1);
        duracloudHost.setArgName("host");
        duracloudHost.setRequired(true);
        options.addOption(duracloudHost);
        
        
        Option duracloudPort = new Option(DURACLOUD_PORT_OPTION, "duracloud-port", true,
                "DuraCloud port");
        duracloudPort.setArgs(1);
        duracloudPort.setArgName("port");
        options.addOption(duracloudPort);

        Option auditLogsSpaceId = new Option(AUDIT_LOGS_SPACE_OPTION, "audit-logs-space-id", true,
                "DuraCloud space where audit logs will be stored.");
        auditLogsSpaceId.setArgs(1);
        auditLogsSpaceId.setArgName("space-id");
        auditLogsSpaceId.setRequired(true);
        options.addOption(auditLogsSpaceId);

        
        return options;
    }
    
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        SystemConfig config = SystemConfig.instance();
        
        config.setDuracloudHost(cmd.getOptionValue(DURACLOUD_HOST_OPTION));
        String port = cmd.getOptionValue(DURACLOUD_PORT_OPTION);
        if(port != null){
            config.setDuracloudPort(Integer.parseInt(port));
        }
        
        config.setDuracloudUsername(cmd.getOptionValue(DURACLOUD_USERNAME_OPTION));
        config.setDuracloudPassword(cmd.getOptionValue(DURACLOUD_PASSWORD_OPTION));
        config.setAuditLogSpaceId(cmd.getOptionValue(AUDIT_LOGS_SPACE_OPTION));
        
        String logRootDir = cmd.getOptionValue(LOG_ROOT_DIR_OPTION);
        initializeLogRoot(logRootDir);
        ApplicationContext context = 
                new AnnotationConfigApplicationContext("org.duracloud.mill");
        log.info("spring context initialized.");
        AuditLogGenerator generator = context.getBean(AuditLogGenerator.class);
        generator.execute();
        log.info("exiting...");
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
