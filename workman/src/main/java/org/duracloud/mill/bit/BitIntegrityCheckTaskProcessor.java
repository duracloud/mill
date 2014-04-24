/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.contentindex.client.ContentIndexClient;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static org.duracloud.common.util.ChecksumUtil.Algorithm.MD5;

/**
 * @author Erik Paulsson
 *         Date: 4/23/14
 */
public class BitIntegrityCheckTaskProcessor implements TaskProcessor {

    private final Logger log =
        LoggerFactory.getLogger(BitIntegrityCheckTaskProcessor.class);

    private BitIntegrityCheckTask bitTask;
    private StorageProvider store;
    private AuditLogStore auditLogStore;
    private ContentIndexClient contentIndexClient;
    private StorageProviderType storageProviderType;

    public BitIntegrityCheckTaskProcessor(BitIntegrityCheckTask bitTask,
                                          StorageProvider store,
                                          StorageProviderType storageProviderType,
                                          AuditLogStore auditLogStore,
                                          ContentIndexClient contentIndexClient) {
        this.bitTask = bitTask;
        this.store = store;
        this.storageProviderType = storageProviderType;
        this.auditLogStore = auditLogStore;
        this.contentIndexClient = contentIndexClient;
    }

    @Override
    public void execute() throws TaskExecutionFailedException {
        String contentChecksum = null;
        //String auditLogChecksum = auditLogStore.getLogItems();
        String storeChecksum = store
            .getContentProperties(bitTask.getSpaceId(), bitTask.getContentId()).
                get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
        String contentIndexChecksum = contentIndexClient
            .get(bitTask.getAccount(), bitTask.getStoreId(),
                 bitTask.getSpaceId(), bitTask.getContentId()).
                getProps().get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
        boolean match = false;

        if(storeChecksum != null && contentIndexChecksum != null) {
            if(storeChecksum.equals(contentIndexChecksum)) {
                match = true;
                if(storageProviderType.equals(StorageProviderType.AMAZON_S3) ||
                    storageProviderType.equals(StorageProviderType.SDSC)) {
                    ChecksumUtil checksumUtil = new ChecksumUtil(MD5);
                    try(InputStream inputStream = store.getContent(bitTask.getSpaceId(),
                                                               bitTask.getContentId())) {
                        contentChecksum = checksumUtil.generateChecksum(inputStream);
                        if(storeChecksum.equals(contentChecksum)) {
                            match = true;
                        }
                    } catch(IOException ioe) {
                        log.error("Error reading inputStream to generate checksum", ioe);
                    }
                }
            }
            log.info("Checksum match={} account={} storeId={} storeType={} space={}" +
                     " contentId={} storeChecksum={} contentIndexChecksum={}" +
                     " contentChecksum={}", match, bitTask.getAccount(),
                     bitTask.getStoreId(), storageProviderType, bitTask.getSpaceId(),
                     bitTask.getContentId(), storeChecksum, contentIndexChecksum,
                     contentChecksum);
        } else {
            log.error("NULL checksums: account={} storeId={} storeType={}" +
                      " space={} contentId={} storeChecksum={} contentIndexChecksum={}",
                      bitTask.getAccount(), bitTask.getStoreId(), storageProviderType,
                      bitTask.getSpaceId(), bitTask.getContentId(), storeChecksum,
                      contentIndexChecksum);
        }
    }
}
