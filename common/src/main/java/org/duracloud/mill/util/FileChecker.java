/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Daniel Bernstein
 *
 */
public class FileChecker {
    private static Logger log = LoggerFactory.getLogger(FileChecker.class);
    /**
     * Ensures that a specified system property is set with a valid file path.
     * If the system property is not set, the specified default value will set instead.
     * If the resulting system property value does not resolve to an existing file,
     * The system will exit after logging the error.
     * @param systemProperty The system property to check
     * @param defaultFilePath The default property file path 
     */
    public static void check(String systemProperty, String defaultPropertyFilePath){
        String path = System.getProperty(systemProperty);

        if(path == null){
            path = defaultPropertyFilePath;
            System.setProperty(systemProperty, path);
            log.info("Using default " + systemProperty + " value: "
                    + path + ".  To override default specify java commandline param -D"
                    + systemProperty+"=/your/prop/file/path/here");
        }else{
            log.info("Using user-defined " + systemProperty + " property: " + path);
        }
        
        if(!new File(path).exists()){
            log.error(path + " does not exist. It is required to run this application. Exiting...");
            System.exit(1);
        }

    }
}
