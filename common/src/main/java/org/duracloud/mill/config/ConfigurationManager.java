/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.config;

import java.io.File;
import java.io.IOException;

import org.duracloud.mill.util.SystemPropertyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: Oct 25, 2013
 */
public class ConfigurationManager {
    
    private static Logger log = LoggerFactory
            .getLogger(ConfigurationManager.class);

    public static final String DUPLICATION_QUEUE_KEY = "duplicationQueue";
    public static final String CREDENTIALS_FILE_PATH_KEY = "credentialsFilePath";
    public static final String DURACLOUD_MILL_CONFIG_FILE_KEY = "duracloud.mill.configFile";

    public String getCredentialsFilePath() {
        return System.getProperty(CREDENTIALS_FILE_PATH_KEY);
    }

    public String getDuplicationQueueName() {
        return System.getProperty(DUPLICATION_QUEUE_KEY);
    }

    public void init() {
        String defaultConfigFile = System.getProperty("user.home")
                + File.separator + "duracloud.mill.properties";
        String configPath = System.getProperty(
                DURACLOUD_MILL_CONFIG_FILE_KEY, defaultConfigFile);
        
        try {
            SystemPropertyLoader.load(configPath);
        } catch (IOException e) {
            log.error(e.getMessage(),e);
            throw new RuntimeException(e);
        }

    }
}
