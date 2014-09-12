/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.db.repo;

import org.duracloud.mill.util.FileChecker;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
@Configuration
@ImportResource("classpath:/mill-jpa-config.xml")
public class MillJpaRepoConfig {
    public static final String XML_CONFIG_PATH = "mill.config.path";
    static {
        String prop = XML_CONFIG_PATH;
        String defaultPath = "/" + System.getProperty("user.home")
                + "/duracloud-mill/mill-config.properties";
        FileChecker.check(prop, defaultPath);
    }
}
