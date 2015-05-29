/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorCreationFailedException;
import org.duracloud.mill.workman.TaskProcessorFactoryBase;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein Date: Oct 21, 2014
 */
public class SpaceComparisonTaskProcessorFactory extends
        TaskProcessorFactoryBase {

    private BitLogStore bitLogStore;
    private ManifestStore manifestStore;
    private StorageProviderFactory storageProviderFactory;

    private static final Logger log = LoggerFactory
            .getLogger(SpaceComparisonTaskProcessorFactory.class);

    public SpaceComparisonTaskProcessorFactory(CredentialsRepo repo,
                                               StorageProviderFactory storageProviderFactory,
                                               BitLogStore bitLogStore,
                                               TaskQueue bitErrorQueue,
                                               ManifestStore manifestStore) {
        super(repo);
        this.bitLogStore = bitLogStore;
        this.manifestStore = manifestStore;
        this.storageProviderFactory = storageProviderFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.workman.TaskProcessorFactoryBase#createImpl(org.duracloud
     * .common.queue.task.Task)
     */
    @Override
    protected TaskProcessor
            createImpl(Task task) throws TaskProcessorCreationFailedException {

        try {
            BitIntegrityCheckReportTask bitTask = new BitIntegrityCheckReportTask();
            bitTask.readTask(task);
            String account = bitTask.getAccount();
            StorageProviderCredentials credentials = getCredentialRepo()
                    .getStorageProviderCredentials(account, bitTask.getStoreId());
            StorageProvider store = storageProviderFactory.create(credentials);
            StorageProviderType storageProviderType = credentials.getProviderType();
            
            return new SpaceComparisonTaskProcessor(bitTask,
                                                    bitLogStore,
                                                    manifestStore,
                                                    store,
                                                    storageProviderType);
        } catch (Exception e) {
            log.error("failed to create TaskProcessor: " + e.getMessage(), e);
            throw new TaskProcessorCreationFailedException("failed to create TaskProcessor: ",
                                                           e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.workman.TaskProcessorFactory#isSupported(org.duracloud
     * .common.queue.task.Task)
     */
    @Override
    public boolean isSupported(Task task) {
        return task.getType().equals(Task.Type.BIT_REPORT);
    }
}
