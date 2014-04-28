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
import org.duracloud.error.NotFoundException;
import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.util.dynamodb.ItemWriteFailedException;
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
    private BitLogStore bitLogStore;
    private ContentIndexClient contentIndexClient;
    private StorageProviderType storageProviderType;

    public BitIntegrityCheckTaskProcessor(BitIntegrityCheckTask bitTask,
                                          StorageProvider store,
                                          StorageProviderType storageProviderType,
                                          AuditLogStore auditLogStore,
                                          BitLogStore bitLogStore,
                                          ContentIndexClient contentIndexClient) {
        this.bitTask = bitTask;
        this.store = store;
        this.storageProviderType = storageProviderType;
        this.auditLogStore = auditLogStore;
        this.bitLogStore = bitLogStore;
        this.contentIndexClient = contentIndexClient;
    }

    @Override
    public void execute() throws TaskExecutionFailedException {
        String contentChecksum = null;  // only used for S3 and SDSC
        String auditLogChecksum = getAuditLogChecksum();
        String contentIndexChecksum = getContentIndexChecksum();
        String storeChecksum = getStoreChecksum();
        BitIntegrityResult result = BitIntegrityResult.FAILURE;

        if(auditLogChecksum != null && contentIndexChecksum != null && storeChecksum != null) {
            if(storeChecksum.equals(contentIndexChecksum) &&
                storeChecksum.equals(auditLogChecksum)) {

                result = BitIntegrityResult.SUCCESS;
                if(storageProviderType.equals(StorageProviderType.AMAZON_S3) ||
                    storageProviderType.equals(StorageProviderType.SDSC)) {
                    try {
                        contentChecksum = getContentChecksum();
                        if(! storeChecksum.equals(contentChecksum)) {
                            result = BitIntegrityResult.FAILURE;
                        }
                    } catch(IOException ioe) {
                        result = BitIntegrityResult.ERROR;
                        log.error("Error reading inputStream to generate checksum", ioe);
                    }
                }
            }
        } else {
            log.error("NULL checksums: account={} storeId={} storeType={}" +
                          " space={} contentId={} auditLogChecksum={} storeChecksum={}" +
                          " contentIndexChecksum={}", bitTask.getAccount(),
                      bitTask.getStoreId(), storageProviderType,
                      bitTask.getSpaceId(), bitTask.getContentId(),
                      auditLogChecksum, storeChecksum, contentIndexChecksum);
        }

        writeResult(result, auditLogChecksum, contentIndexChecksum,
                    storeChecksum, contentChecksum);
    }

    private void writeResult(BitIntegrityResult result, String auditLogChecksum,
                             String contentIndexChecksum, String storeChecksum,
                             String contentChecksum) throws ItemWriteFailedException {
        if (result == BitIntegrityResult.SUCCESS) {
            // Since the checksums match only log one of the checksum values
            log.info(
                "Checksum result={} account={} storeId={} storeType={} space={}" +
                    " contentId={} contentChecksum={}", result, bitTask.getAccount(),
                bitTask.getStoreId(), storageProviderType, bitTask.getSpaceId(),
                bitTask.getContentId(), storeChecksum);

            bitLogStore.write(bitTask.getAccount(), bitTask.getStoreId(),
                              bitTask.getSpaceId(), bitTask.getContentId(),
                              System.currentTimeMillis(), result, storeChecksum,
                              null, null, null, null);
        } else {
            log.error(
                "Checksum result={} account={} storeId={} storeType={} space={}" +
                    " contentId={} auditLogChecksum={} contentIndexChecksum={} storeChecksum={}" +
                    " contentChecksum={}", result, bitTask.getAccount(),
                bitTask.getStoreId(), storageProviderType, bitTask.getSpaceId(),
                bitTask.getContentId(), auditLogChecksum, contentIndexChecksum,
                storeChecksum, contentChecksum);

            bitLogStore.write(bitTask.getAccount(), bitTask.getStoreId(),
                              bitTask.getSpaceId(), bitTask.getContentId(),
                              System.currentTimeMillis(), result, contentChecksum,
                              storeChecksum, auditLogChecksum, contentIndexChecksum, null);
        }
    }

    private String getAuditLogChecksum() {
        String auditLogChecksum = null;
        try {
            auditLogChecksum = auditLogStore.getLatestLogItem(
                bitTask.getAccount(), bitTask.getStoreId(),
                bitTask.getSpaceId(), bitTask.getContentId()).getContentMd5();
        } catch(NotFoundException nfe) {
            log.error("Could not find latest log item for" +
                          " account=" + bitTask.getAccount() +
                          " storeId="+ bitTask.getStoreId() +
                          " space=" + bitTask.getSpaceId() +
                          " contentId=" + bitTask.getContentId(), nfe);
        }
        return auditLogChecksum;
    }

    private String getContentIndexChecksum() {
        return contentIndexClient
            .get(bitTask.getAccount(), bitTask.getStoreId(),
                 bitTask.getSpaceId(), bitTask.getContentId()).
                getProps().get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
    }

    private String getStoreChecksum() {
        return store.getContentProperties(bitTask.getSpaceId(),
                                          bitTask.getContentId())
                    .get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
    }

    private String getContentChecksum() throws IOException {
        String contentChecksum = null;
        ChecksumUtil checksumUtil = new ChecksumUtil(MD5);
        try(InputStream inputStream = store.getContent(bitTask.getSpaceId(),
                                                       bitTask.getContentId())) {
            contentChecksum = checksumUtil.generateChecksum(inputStream);
        }
        return contentChecksum;
    }
}
