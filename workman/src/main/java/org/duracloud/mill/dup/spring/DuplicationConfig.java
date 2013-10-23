package org.duracloud.mill.dup.spring;

import org.duracloud.mill.credential.CredentialRepo;
import org.duracloud.mill.dup.DuplicationTaskProcessorFactory;
import org.duracloud.mill.workman.RootTaskProcessorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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