/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagereporter;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.duracloud.account.db.repo.DuracloudAccountRepo;
import org.duracloud.mill.config.ConfigurationManager;
import org.duracloud.mill.db.repo.JpaSpaceStatsRepo;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.notification.SESNotificationManager;
import org.duracloud.mill.util.CommonCommandLineOptions;
import org.duracloud.mill.util.DriverSupport;
import org.duracloud.mill.util.PropertyDefinition;
import org.duracloud.mill.util.PropertyDefinitionListBuilder;
import org.duracloud.mill.util.PropertyVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 *  The main driver of the storage reporter tool.
 * 
 * @author dbernstein 
 * @since: Jun 28, 2017
 */
public class AppDriver extends DriverSupport {

    public AppDriver() {
        super(new CommonCommandLineOptions());
    }

    public static void main(String[] args) {
        new AppDriver().execute(args);
    }

    /**
     * @param cmd
     */
    @Override
    protected void executeImpl(CommandLine commandLine) {

        List<PropertyDefinition> defintions = new PropertyDefinitionListBuilder().addAws()
                                                                                 .addNotifications()
                                                                                 .addNotificationsNonTech()
                                                                                 .addMcDb()
                                                                                 .addMillDb()
                                                                                 .build();
        PropertyVerifier verifier = new PropertyVerifier(defintions);
        verifier.verify(System.getProperties());

        // configure spring components
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.duracloud.mill.db",
                                                                                            "org.duracloud.account.db");
        JpaSpaceStatsRepo statsRepo = (JpaSpaceStatsRepo) context
                .getBean(JpaSpaceStatsRepo.class);
        DuracloudAccountRepo accountRepo = (DuracloudAccountRepo) context
                .getBean(DuracloudAccountRepo.class);

        // setup notification client
        ConfigurationManager configManager = new ConfigurationManager();
        List<String> recipients = Arrays
                .asList(configManager.getNotificationRecipients());
        recipients.addAll(Arrays
                .asList(configManager.getNotificationRecipientsNonTech()));
        NotificationManager notification = new SESNotificationManager(recipients
                .toArray(new String[0]));

        StorageReporter reporter = new StorageReporter(statsRepo, accountRepo, notification);
        reporter.run();
        
        context.close();
    }
}
