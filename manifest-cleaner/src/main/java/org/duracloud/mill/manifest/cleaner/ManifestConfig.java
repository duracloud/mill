/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest.cleaner;

import org.duracloud.mill.db.repo.JpaManifestItemRepo;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.manifest.jpa.JpaManifestStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Daniel Bernstein
 *         Date: Sep 11, 2014
 */
@Configuration
public class ManifestConfig {

    @Bean
    public ManifestStore manifestStore(JpaManifestItemRepo manifestItemRepo){
        return new JpaManifestStore(manifestItemRepo);
    }

}
