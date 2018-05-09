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
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.common.util.ChecksumUtil.Algorithm;
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
 * @author Daniel Bernstein
 * Date: 10/14/2014
 */
public class BitIntegrityCheckTaskProcessorFactory
    extends TaskProcessorFactoryBase {

    private static Logger log =
        LoggerFactory.getLogger(BitIntegrityCheckTaskProcessorFactory.class);

    private StorageProviderFactory storageProviderFactory;
    private BitLogStore bitLogStore;
    private TaskQueue bitErrorQueue;
    private TaskQueue auditTaskQueue;
    private ManifestStore manifestStore;

    public BitIntegrityCheckTaskProcessorFactory(CredentialsRepo repo,
                                                 StorageProviderFactory storageProviderFactory,
                                                 BitLogStore bitLogStore,
                                                 TaskQueue bitErrorQueue,
                                                 TaskQueue auditTaskQueue,
                                                 ManifestStore manifestStore) {
        super(repo);
        this.storageProviderFactory = storageProviderFactory;
        this.bitLogStore = bitLogStore;
        this.bitErrorQueue = bitErrorQueue;
        this.auditTaskQueue = auditTaskQueue;
        this.manifestStore = manifestStore;
    }

    @Override
    public boolean isSupported(Task task) {
        return task.getType().equals(Task.Type.BIT);
    }

    @Override
    protected TaskProcessor createImpl(Task task)
        throws TaskProcessorCreationFailedException {

        BitIntegrityCheckTask bitTask = new BitIntegrityCheckTask();
        bitTask.readTask(task);
        String subdomain = bitTask.getAccount();

        try {
            StorageProviderCredentials credentials =
                getCredentialRepo().getStorageProviderCredentials(subdomain, bitTask.getStoreId());
            StorageProvider store = storageProviderFactory.create(credentials);
            StorageProviderType storageProviderType = credentials.getProviderType();
            return new BitIntegrityCheckTaskProcessor(bitTask,
                                                      store,
                                                      manifestStore, storageProviderType,
                                                      bitLogStore,
                                                      bitErrorQueue,
                                                      auditTaskQueue,
                                                      new ContentChecksumHelper(storageProviderType,
                                                                                bitTask,
                                                                                store,
                                                                                new ChecksumUtil(Algorithm.MD5)));
        } catch (Exception e) {
            log.error("failed to create TaskProcessor: unable to locate" +
                      " credentials for subdomain: " + e.getMessage(), e);
            throw new TaskProcessorCreationFailedException(
                "failed to create TaskProcessor: unable to locate credentials " +
                "for subdomain: " + subdomain, e);
        }
    }
}
