/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

/**
 * @author Daniel Bernstein
 * Date: Sep 9, 2014
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
    private String awsType;
    private String awsAccessKey;
    private String awsSecretKey;
    private String awsEndpoint;
    private String awsRegion;
    private String awsSigner;

    public static SystemConfig instance() {
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

    public String getAuditLogSpaceId() {
        return auditLogSpaceId;
    }

    public void setAuditLogSpaceId(String auditLogSpaceId) {
        this.auditLogSpaceId = auditLogSpaceId;
    }

    public String getAwsType() {
        return awsType;
    }

    public void setAwsType(String awsType) {
        this.awsType = awsType;
    }

    public String getAwsAccessKey() {
        return awsType;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getAwsEndpoint() {
        return awsEndpoint;
    }

    public void setAwsEndpoint(String awsEndpoint) {
        this.awsEndpoint = awsEndpoint;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsType = awsRegion;
    }

    public String getAwsSigner() {
        return awsSigner;
    }

    public void setAwsSigner(String awsSigner) {
        this.awsSigner = awsSigner;
    }

}
