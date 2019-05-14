/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman.spring;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
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

public class AppDriver extends DriverSupport {

    private static final Logger log = LoggerFactory.getLogger(AppDriver.class);

    private static final String TASK_QUEUES_OPTION = "q";

    public static final long DEFAULT_POLICY_UPDATE_FREQUENCY_MS = 5 * 60 * 1000;

    private static class WorkmanOptions extends CommonCommandLineOptions {
        public WorkmanOptions() {
            super();
            Option taskQueues = new Option(TASK_QUEUES_OPTION,
                                           "task-queue-names",
                                           true,
                                           "A comma-separated prioritized list of queue name keys " +
                                           "(ie not the sqs queue names themselves!) where the first is highest.");
            taskQueues.setArgs(1);
            taskQueues.setRequired(false);
            taskQueues.setArgName("name");
            addOption(taskQueues);

        }
    }

    public AppDriver() {
        super(new WorkmanOptions());
    }

    public static void main(String[] args) {
        new AppDriver().execute(args);
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.util.DriverSupport#executeImpl(org.apache.commons.cli.CommandLine)
     */
    @Override
    protected void executeImpl(CommandLine cmd) {

        String taskQueueNames = cmd.getOptionValue(TASK_QUEUES_OPTION);
        if (taskQueueNames != null) {
            setSystemProperty(ConfigConstants.QUEUE_TASK_ORDERED, taskQueueNames);
        }

        List<PropertyDefinition> defintions =
            new PropertyDefinitionListBuilder().addAws()
                                               .addMillDb()
                                               .addMcDb()
                                               .addDeadLetterQueue()
                                               .addQueueType()
                                               .addRabbitMQConfig()
                                               .addQueueType()
                                               .addAuditQueue()
                                               .addBitIntegrityQueue()
                                               .addBitIntegrityErrorQueue()
                                               .addBitIntegrityReportQueue()
                                               .addNotifications()
                                               .addWorkDir()
                                               .addTaskQueueOrder()
                                               .addDuplicationPolicyBucketSuffix()
                                               .addDuplicationPolicyRefreshFrequency()
                                               .addDuplicationHighPriorityQueue()
                                               .addLocalDuplicationDir()
                                               .addMaxWorkers()
                                               .build();
        PropertyVerifier verifier = new PropertyVerifier(defintions);
        verifier.verify(System.getProperties());

        TaskProducerConfigurationManager config = new WorkmanConfigurationManager();

        String workDirPath = config.getWorkDirectoryPath();

        if (workDirPath == null || workDirPath.trim() == "") {
            // this should never happen since workDirPath is required,
            // but I'll leave this in here as a sanity check.
            workDirPath = System.getProperty("java.io.tmpdir") + File.separator + "workman-work";
        }

        initializeWorkDir(workDirPath);

        String localDuplicationPolicyDirPath = config.getDuplicationPolicyDir();

        if (localDuplicationPolicyDirPath != null && !new File(localDuplicationPolicyDirPath).exists()) {
            System.err.print("The local duplication policy directory "
                             + "path you specified, "
                             + localDuplicationPolicyDirPath + " does not exist: ");
            die();
        }

        ApplicationContext context = new AnnotationConfigApplicationContext("org.duracloud.mill");
    }

    /**
     * @param workDirPath
     */
    private void initializeWorkDir(String workDirPath) {

        try {
            File workDir = new File(workDirPath);

            if (!workDir.exists()) {
                if (!workDir.mkdirs()) {
                    String message = "Unable to create work dir: "
                                     + workDir.getAbsolutePath()
                                     + ". Check that workman process has "
                                     + "permission to create this directory";
                    log.error(message);
                    System.exit(1);
                }
            }

        } catch (Exception ex) {
            log.error("failed to initialize workDir " + workDirPath + ":" + ex.getMessage(), ex);
            System.exit(1);
        }

    }

}
