/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.common.queue.task.Task;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorCreationFailedException;
import org.duracloud.mill.workman.TaskProcessorFactoryBase;
import org.duracloud.mill.common.storageprovider.StorageProviderFactory;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Erik Paulsson
 *         Date: 4/23/14
 */
public class BitIntegrityCheckTaskProcessorFactory
    extends TaskProcessorFactoryBase {

    private static Logger log =
        LoggerFactory.getLogger(BitIntegrityCheckTaskProcessorFactory.class);

    private StorageProviderFactory storageProviderFactory;
    private ContentIndexClient contentIndexClient;
    private AuditLogStore auditLogStore;
    private BitLogStore bitLogStore;

    public BitIntegrityCheckTaskProcessorFactory(CredentialsRepo repo,
                                                 StorageProviderFactory storageProviderFactory,
                                                 ContentIndexClient contentIndexClient,
                                                 AuditLogStore auditLogStore,
                                                 BitLogStore bitLogStore) {
        super(repo);
        this.contentIndexClient = contentIndexClient;
        this.auditLogStore = auditLogStore;
        this.storageProviderFactory = storageProviderFactory;
        this.bitLogStore = bitLogStore;
    }

    @Override
    protected boolean isSupported(Task task) {
        return task.getType().equals(Task.Type.BIT);
    }

    @Override
    protected TaskProcessor createImpl(Task task)
        throws TaskProcessorCreationFailedException {

        BitIntegrityCheckTask bitTask = new BitIntegrityCheckTask();
        bitTask.readTask(task);
        String subdomain = bitTask.getAccount();

        try {
            StorageProviderCredentials credentials = getCredentialRepo().
                getStorageProviderCredentials(subdomain, bitTask.getStoreId());
            StorageProvider store = storageProviderFactory.create(credentials);
            StorageProviderType storageProviderType = credentials.getProviderType();
            return new BitIntegrityCheckTaskProcessor(bitTask,
                                                      store,
                                                      storageProviderType,
                                                      auditLogStore,
                                                      bitLogStore,
                                                      contentIndexClient,
                                                      new ChecksumUtil(
                                                          ChecksumUtil.Algorithm.MD5));
        } catch (Exception e) {
            log.error("failed to create TaskProcessor: unable to locate" +
                          " credentials for subdomain: " + e.getMessage(), e);
            throw new TaskProcessorCreationFailedException(
                "failed to create TaskProcessor: unable to locate credentials " +
                    "for subdomain: "+ subdomain, e);
        }
    }
}
