/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import org.duracloud.s3storage.S3StorageProvider;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Daniel Bernstein
 *         Date: Sep 9, 2014
 */
@Configuration
public class AuditGeneratorConfig {
    private static Logger log = LoggerFactory.getLogger(AuditGeneratorConfig.class);

    @Bean
    public String logsDirectory() {
        String dir = systemConfig().getLogsDirectory();
        log.info("initializing logsDirectory with value: {}", dir);
        return dir;
    }
    
    /**
     * @return
     */
    private SystemConfig systemConfig() {
        return SystemConfig.instance();
    }

    @Bean
    public String auditLogSpaceId(){
        return SystemConfig.getInstance().getAuditLogSpaceId();
    }
    
    @Bean
    public StorageProvider storageProvider(){
        String accessKey = systemConfig().getAwsAccessKeyId();
        String secretKey = systemConfig().getAwsSecretKey();
        return new S3StorageProvider(accessKey, secretKey);

    }

}
