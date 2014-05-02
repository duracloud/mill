/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import org.duracloud.audit.AuditLogStore;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
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
                if(storageProviderType == StorageProviderType.AMAZON_S3 ||
                    storageProviderType == StorageProviderType.SDSC) {
                    contentChecksum = getContentChecksum();
                    if(! storeChecksum.equals(contentChecksum)) {
                        result = BitIntegrityResult.FAILURE;
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

    private void writeResult(final BitIntegrityResult result, final String auditLogChecksum,
                             final String contentIndexChecksum, final String storeChecksum,
                             final String contentChecksum) throws TaskExecutionFailedException {
        if (result == BitIntegrityResult.SUCCESS) {
            writeSuccessResult(result, storeChecksum);
        } else {
            writeNonSuccessResult(result, auditLogChecksum, contentIndexChecksum,
                                  storeChecksum, contentChecksum);
        }
    }

    private void writeSuccessResult(final BitIntegrityResult result,
                                    final String checksum)
        throws TaskExecutionFailedException {
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Since the checksums match only log one of the checksum values
                    bitLogStore
                        .write(bitTask.getAccount(), bitTask.getStoreId(),
                               bitTask.getSpaceId(), bitTask.getContentId(),
                               System.currentTimeMillis(), storageProviderType,
                               result, checksum, null, null, null, null);
                    return "success";
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage(
                    "Could not write successful result to BitLogStore"), e);
        }
        log.info(
            "Checksum result={} account={} storeId={} storeType={} space={}" +
                " contentId={} checksum={}", result, bitTask.getAccount(),
            bitTask.getStoreId(), storageProviderType, bitTask.getSpaceId(),
            bitTask.getContentId(), checksum);
    }

    private void writeNonSuccessResult(final BitIntegrityResult result,
                                       final String auditLogChecksum,
                                       final String contentIndexChecksum,
                                       final String storeChecksum,
                                       final String contentChecksum)
        throws TaskExecutionFailedException {
        try {
            new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    // Since the checksums match only log one of the checksum values
                    bitLogStore
                        .write(bitTask.getAccount(), bitTask.getStoreId(),
                               bitTask.getSpaceId(), bitTask.getContentId(),
                               System.currentTimeMillis(), storageProviderType,
                               result, contentChecksum, storeChecksum,
                               auditLogChecksum, contentIndexChecksum, null);
                    return "success";
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage(
                    "Could not write non-successful result to BitLogStore"), e);
        }
        log.error(
            "Checksum result={} account={} storeId={} storeType={} space={}" +
                " contentId={} auditLogChecksum={} contentIndexChecksum={} storeChecksum={}" +
                " contentChecksum={}", result, bitTask.getAccount(),
            bitTask.getStoreId(), storageProviderType, bitTask.getSpaceId(),
            bitTask.getContentId(), auditLogChecksum, contentIndexChecksum,
            storeChecksum, contentChecksum);
    }

    private String getAuditLogChecksum() throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    return auditLogStore.getLatestLogItem(
                        bitTask.getAccount(),
                        bitTask.getStoreId(),
                        bitTask.getSpaceId(),
                        bitTask.getContentId()).getContentMd5();
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage("Could not find latest audit log item"), e);
        }
    }

    private String getContentIndexChecksum() {
        return contentIndexClient
            .get(bitTask.getAccount(), bitTask.getStoreId(),
                 bitTask.getSpaceId(), bitTask.getContentId()).
                getProps().get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
    }

    private String getStoreChecksum() throws TaskExecutionFailedException {
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    return store.getContentProperties(bitTask.getSpaceId(),
                                                      bitTask.getContentId())
                                .get(
                                    StorageProvider.PROPERTIES_CONTENT_CHECKSUM);
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage(
                    "Could not retrieve checksum from storage provider"), e);
        }
    }

    private String getContentChecksum() throws TaskExecutionFailedException {
        final ChecksumUtil checksumUtil = new ChecksumUtil(MD5);
        try {
            return new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    try(InputStream inputStream = store.getContent(bitTask.getSpaceId(),
                                                                   bitTask.getContentId())) {
                        return checksumUtil.generateChecksum(inputStream);
                    }
                }
            });
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(
                buildFailureMessage(
                    "Could not compute checksum from content stream"), e);
        }
    }

    private String buildFailureMessage(String message) {
        StringBuilder builder = new StringBuilder();
        builder.append("Failure to bit-integrity check content item due to: ");
        builder.append(message);
        builder.append(" Account: ");
        builder.append(bitTask.getAccount());
        builder.append(" Source StoreID: ");
        builder.append(bitTask.getStoreId());
        builder.append(" Store Type: ");
        builder.append(storageProviderType);
        builder.append(" SpaceID: ");
        builder.append(bitTask.getSpaceId());
        builder.append(" ContentID: ");
        builder.append(bitTask.getContentId());
        return builder.toString();
    }
}
