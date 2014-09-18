/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.impl;

import org.duracloud.mill.util.PropertyFileHelper;
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
    private static final String CONFIG_PROPERTIES_PATH = "config.properties";
    static {
        String defaultPath = "/" + System.getProperty("user.home")
                + "/duracloud-mc/mc-config.properties";
        PropertyFileHelper.loadFromSystemProperty(CONFIG_PROPERTIES_PATH, defaultPath);
        new MCJpaPropertiesVerifier().verify();
        
    }
}
