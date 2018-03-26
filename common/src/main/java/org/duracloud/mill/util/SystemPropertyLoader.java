/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: Oct 30, 2013
 */
public class SystemPropertyLoader {
    private static Logger log = LoggerFactory.getLogger(SystemPropertyLoader.class);

    private SystemPropertyLoader() {
        // Ensures no instances are made of this class, as there are only static members.
    }

    /**
     * @param filePath
     */
    public static void load(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Config file " + filePath + " not found.");
        }

        log.info("loading {}", file);

        // load configuration info into System properties.
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            props.load(is);
            for (Object key : props.keySet()) {
                if (System.getProperty(key.toString()) == null) {
                    System.setProperty(key.toString(), props.get(key).toString());
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to load configuration file: " + file, e);
        }

        log.info("successfully loaded {}", file);

    }
}
