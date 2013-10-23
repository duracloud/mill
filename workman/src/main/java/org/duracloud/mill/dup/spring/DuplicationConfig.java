/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup.spring;

import org.duracloud.mill.credentials.CredentialRepo;
import org.duracloud.mill.dup.DuplicationTaskProcessorFactory;
import org.duracloud.mill.workman.RootTaskProcessorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A spring configuration that automatically hooks itself into the task
 * processing framework.
 * 
 * @author Daniel Bernstein
 * 
 */
@Configuration
public class DuplicationConfig {
    @Bean
    public DuplicationTaskProcessorFactory duplicationTaskProcessorFactory(
            CredentialRepo repo, RootTaskProcessorFactory root) {
        DuplicationTaskProcessorFactory f = new DuplicationTaskProcessorFactory(
                repo);
        root.addTaskProcessorFactory(f);
        return f;
    }

}