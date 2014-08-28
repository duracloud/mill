/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.impl;

import org.duracloud.mill.util.FileChecker;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
@Configuration
@ImportResource("classpath:/credentials-repo-jpa-config.xml")
public class CredentialsRepoConfig {
    private static final String MC_CONFIG_PATH_PROP = "mc.config.path";
    static {
        String prop = MC_CONFIG_PATH_PROP;
        String defaultPath = "/" + System.getProperty("user.home")
                + "/duracloud-mc/mc-config.properties";
        FileChecker.check(prop, defaultPath);
    }
}
