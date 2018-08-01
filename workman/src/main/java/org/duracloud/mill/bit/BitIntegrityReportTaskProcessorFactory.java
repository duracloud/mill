/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorCreationFailedException;
import org.duracloud.mill.workman.TaskProcessorFactoryBase;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: 5/7/2014
 */
public class BitIntegrityReportTaskProcessorFactory
    extends TaskProcessorFactoryBase {

    private static Logger log =
        LoggerFactory.getLogger(BitIntegrityReportTaskProcessorFactory.class);

    private BitLogStore bitLogStore;
    private StorageProviderFactory storageProviderFactory;
    private TaskProducerConfigurationManager workmanConfigurationManager;
    private NotificationManager notificationManager;

    public BitIntegrityReportTaskProcessorFactory(CredentialsRepo repo,
                                                  BitLogStore bitLogStore,
                                                  StorageProviderFactory storageProviderFactory,
                                                  TaskProducerConfigurationManager workmanConfigurationManager,
                                                  NotificationManager notificationManager) {
        super(repo);
        this.bitLogStore = bitLogStore;
        this.storageProviderFactory = storageProviderFactory;
        this.workmanConfigurationManager = workmanConfigurationManager;
        this.notificationManager = notificationManager;
    }

    @Override
    public boolean isSupported(Task task) {
        return task.getType().equals(Task.Type.BIT_REPORT);
    }

    @Override
    protected TaskProcessor createImpl(Task task)
        throws TaskProcessorCreationFailedException {

        BitIntegrityCheckReportTask bitTask = new BitIntegrityCheckReportTask();
        bitTask.readTask(task);

        try {

            AccountCredentials credentials = getCredentialRepo().getAccountCredentials(bitTask.getAccount());
            for (StorageProviderCredentials creds : credentials.getProviderCredentials()) {
                if (creds.isPrimary()) {
                    StorageProvider store = storageProviderFactory.create(creds);

                    return new BitIntegrityReportTaskProcessor(bitTask,
                                                               bitLogStore,
                                                               store,
                                                               workmanConfigurationManager,
                                                               notificationManager);

                }
            }

            throw new TaskProcessorCreationFailedException(
                "Unable to find a set of primary storage providder credentials");
        } catch (Exception e) {
            log.error("failed to create TaskProcessor: " + e.getMessage(), e);
            throw new TaskProcessorCreationFailedException(
                "failed to create TaskProcessor: ", e);
        }
    }
}
