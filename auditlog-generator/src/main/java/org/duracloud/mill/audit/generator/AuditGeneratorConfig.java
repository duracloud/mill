/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import org.duracloud.client.ContentStore;
import org.duracloud.sync.endpoint.DuraStoreSyncEndpoint;
import org.duracloud.sync.util.StoreClientUtil;
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
    
    @Bean
    public DuraStoreSyncEndpoint endpoint(){
        String username = systemConfig().getDuracloudUsername();
        String spaceId =  systemConfig().getAuditLogSpaceId();
        log.info("initializing end point with username={} and spaceId={}",
                 username,
                 spaceId);
        return new DuraStoreSyncEndpoint(contentStore(), username, spaceId, false);
    }
    
    /**
     * @return
     */
    private SystemConfig systemConfig() {
        return SystemConfig.instance();
    }

    @Bean
    public ContentStore contentStore(){
        String host = systemConfig().getDuracloudHost();
        int port = systemConfig().getDuracloudPort();
        String context = systemConfig().getDurastoreContext();
        String username = systemConfig().getDuracloudUsername();
        String password = systemConfig().getDuracloudPassword();

        log.info("initializing content store: host={}, port={}, context={}, username={}",
                 host,
                 port, 
                 context,
                 username);

        StoreClientUtil util = new StoreClientUtil();
        return util.createContentStore(host, port, context, username, password, null);
    }

}
