/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.s3.AmazonS3Client;
import org.duracloud.s3storage.S3StorageProvider;
import org.duracloud.storage.provider.StorageProvider;
import org.duracloud.swiftstorage.SwiftStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Daniel Bernstein
 * Date: Sep 9, 2014
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
    public String auditLogSpaceId() {
        return SystemConfig.getInstance().getAuditLogSpaceId();
    }

    @Bean
    public StorageProvider storageProvider() {
        //build the storage provider with a placeholder key since audit log generator
        //does not depend on creating new spaces.
        SystemConfig systemConfig = systemConfig();
        if (systemConfig.getAwsType() == "swift") {
            Map<String, String> map = new HashMap<String, String>();
            map.put("AWS_REGION", systemConfig.getAwsRegion());
            map.put("SWIFT_S3_ENDPOINT", systemConfig.getAwsEndpoint());
            map.put("SWIFT_S3_SIGNER_TYPE", systemConfig.getAwsSigner());
            return new SwiftStorageProvider(systemConfig.getAwsAccessKey(), systemConfig.getAwsAccessKey(), map);
        } else {
            return new S3StorageProvider(new AmazonS3Client(), "aduracloudmillprefix", null);
        }
    }

}
