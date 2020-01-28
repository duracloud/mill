/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.util.CommonCommandLineOptions;
import org.duracloud.mill.util.DriverSupport;
import org.duracloud.mill.util.PropertyDefinition;
import org.duracloud.mill.util.PropertyDefinitionListBuilder;
import org.duracloud.mill.util.PropertyVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Daniel Bernstein
 */

public class AuditLogGeneratorDriver extends DriverSupport {

    private static final Logger log = LoggerFactory.getLogger(AuditLogGeneratorDriver.class);

    public AuditLogGeneratorDriver() {
        super(new CommonCommandLineOptions());
    }

    public static void main(String[] args) {
        new AuditLogGeneratorDriver().execute(args);
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.util.DriverSupport#executeImpl(org.apache.commons.cli.CommandLine)
     */
    @Override
    protected void executeImpl(CommandLine cmd) {
        PropertyDefinitionListBuilder builder = new PropertyDefinitionListBuilder();
        List<PropertyDefinition> list = builder.addAws()
                                               .addMillDb()
                                               .addDuracloudAuditSpace()
                                               .addWorkDir()
                                               .addGlobalWorkDir()
                                               .build();

        new PropertyVerifier(list).verify(System.getProperties());

        SystemConfig config = SystemConfig.instance();
        config.setAuditLogSpaceId(System
                                      .getProperty(ConfigConstants.AUDIT_LOGS_SPACE_ID));

        if (config.getAwsType() == "swift") {
            config.setAwsType(System.getProperty(ConfigConstants.AWS_TYPE));
            config.setAwsAccessKey(System.getProperty(ConfigConstants.AWS_ACCESS_KEY_ID));
            config.setAwsSecretKey(System.getProperty(ConfigConstants.AWS_SECRET_KEY));
            config.setAwsEndpoint(System.getProperty(ConfigConstants.AWS_ENDPOINT));
            config.setAwsRegion(System.getProperty(ConfigConstants.AWS_REGION));
            config.setAwsSigner(System.getProperty(ConfigConstants.AWS_SIGNER));
        }

        String workDir = System.getProperty(ConfigConstants.GLOBAL_WORK_DIRECTORY_PATH);
        if (workDir == null) {
            //for backwards compatibility use old work directory if no global work dir configured.
            workDir = System.getProperty(ConfigConstants.WORK_DIRECTORY_PATH);
        }

        String logRootDir = workDir + File.separator + "audit-logs";
        initializeLogRoot(logRootDir);
        ApplicationContext context = new AnnotationConfigApplicationContext("org.duracloud.mill");
        log.info("spring context initialized.");
        AuditLogGenerator generator = context.getBean(AuditLogGenerator.class);
        generator.execute();
        log.info("exiting...");
    }

    /**
     * @param logRootPath
     */
    private static void initializeLogRoot(String logRootPath) {

        try {
            File logRootDir = new File(logRootPath);

            if (!logRootDir.exists()) {
                if (!logRootDir.mkdirs()) {
                    String message = "Unable to create log root dir: "
                                     + logRootDir.getAbsolutePath() +
                                     ". Please make sure that this process has " +
                                     "permission to create this directory";
                    log.error(message);
                    System.exit(1);
                }
            }

            SystemConfig.instance().setLogsDirectory(logRootPath);
        } catch (Exception ex) {
            log.error("failed to initialize log root dir " + logRootPath + ":" + ex.getMessage(), ex);
            System.exit(1);
        }

    }
}
