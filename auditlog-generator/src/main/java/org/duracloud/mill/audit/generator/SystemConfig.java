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
    private String awsAccessKey;
    private String awsSecretKey;
    private String swiftEndpoint;
    private String swiftSignerType;

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

    public String getAwsAccessKey() {
        return awsAccessKey;
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

    public String getSwiftEndpoint() {
        return swiftEndpoint;
    }

    public void setSwiftEndpoint(String swiftEndpoint) {
        this.swiftEndpoint = swiftEndpoint;
    }

    public String getSwiftSignerType() {
        return swiftSignerType;
    }

    public void setSwiftSignerType(String swiftSignerType) {
        this.swiftSignerType = swiftSignerType;
    }

}
