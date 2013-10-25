/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: Oct 25, 2013
 */
public class ConfigurationManager {
    
    private static Logger log = LoggerFactory
            .getLogger(ConfigurationManager.class);
    public static final String DURACLOUD_WORKMAN_CONFIG_FILE_KEY = "duracloud.workman.configFile";

    public String getCredentialsFilePath() {
        return System.getProperty("credentials.file.path",
                "no-credentials-file-path-set");
    }

    public String getQueueUrl() {
        return System.getProperty("duracloud.sqsQueueUrl",
                "no-duracloud-sqs-queue-url-set");
    }

    public void init() {
        String defaultConfigFile = System.getProperty("user.dir")
                + File.separator + "duracloud.workman.config.properties";
        String configPath = System.getProperty(
                DURACLOUD_WORKMAN_CONFIG_FILE_KEY, defaultConfigFile);

        File configFile = new File(configPath);
        if (!configFile.exists()) {
            throw new RuntimeException("Config file " + configPath
                    + " not found.");
        }

        log.info("loading {}", configFile);

        // load configuration info into System properties.
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(configFile)) {
            props.load(is);
            for (Object key : props.keySet()) {
                if (System.getProperty(key.toString()) == null) {
                    System.setProperty(key.toString(), props.get(key)
                            .toString());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file: "
                    + configFile, e);
        }

        log.info("successfully loaded {}", configFile);

        // String filePath = cmd.getOptionValue("f");
        // if(filePath != null){
        // System.setProperty("credentials.file.path", filePath);
        // if(!new File(filePath).exists()){
        // System.err.print("Specified file " + filePath + " not found.");
        // die();
        // }
        // }
    }
}
