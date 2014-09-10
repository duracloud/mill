/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import org.duracloud.sync.endpoint.DuraStoreSyncEndpoint;

/**
 * @author Daniel Bernstein
 *         Date: Sep 9, 2014
 */
public class SystemConfig {

    private static SystemConfig instance = new SystemConfig();

    private String logsDirectory;
    private String durastoreContext = "durastore";
    private String duracloudHost;
    private int duracloudPort = 443;
    private String duracloudUsername;
    private String duracloudPassword;
    private String auditLogSpaceId;

    public static SystemConfig instance(){
        return instance;
    }

    public static SystemConfig getInstance() {
        return instance;
    }

    public static void setInstance(SystemConfig instance) {
        SystemConfig.instance = instance;
    }

    public String getLogsDirectory() {
        return logsDirectory;
    }

    public void setLogsDirectory(String logsDirectory) {
        this.logsDirectory = logsDirectory;
    }

    public String getDurastoreContext() {
        return durastoreContext;
    }

    public void setDurastoreContext(String durastoreContext) {
        this.durastoreContext = durastoreContext;
    }

    public String getDuracloudHost() {
        return duracloudHost;
    }

    public void setDuracloudHost(String duracloudHost) {
        this.duracloudHost = duracloudHost;
    }

    public int getDuracloudPort() {
        return duracloudPort;
    }

    public void setDuracloudPort(int duracloudPort) {
        this.duracloudPort = duracloudPort;
    }

    public String getDuracloudUsername() {
        return duracloudUsername;
    }

    public void setDuracloudUsername(String duracloudUsername) {
        this.duracloudUsername = duracloudUsername;
    }

    public String getDuracloudPassword() {
        return duracloudPassword;
    }

    public void setDuracloudPassword(String duracloudPassword) {
        this.duracloudPassword = duracloudPassword;
    }

    public String getAuditLogSpaceId() {
        return auditLogSpaceId;
    }

    public void setAuditLogSpaceId(String auditLogSpaceId) {
        this.auditLogSpaceId = auditLogSpaceId;
    }
    
    
}
