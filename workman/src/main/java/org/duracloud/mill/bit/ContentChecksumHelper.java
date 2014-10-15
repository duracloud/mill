/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.io.InputStream;

import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates retry logic for calculating 
 * content checksums.
 * @author Daniel Bernstein 
           Date: Oct 14, 2014
 */
public class ContentChecksumHelper {
    private static Logger log = LoggerFactory
            .getLogger(ContentChecksumHelper.class);
    private StorageProviderType storageProviderType;
    private ChecksumUtil checksumUtil;
    private BitIntegrityCheckTask bitTask;
    private StorageProvider store;
    private boolean checked = false;
    private String contentChecksum;

    /**
     * @param storageProviderType
     * @param bitTask
     * @param store
     */
    public ContentChecksumHelper(StorageProviderType storageProviderType,
                                 BitIntegrityCheckTask bitTask,
                                 StorageProvider store,
                                 ChecksumUtil checksumUtil) {
        this.storageProviderType = storageProviderType;
        this.bitTask = bitTask;
        this.store = store;
        this.checksumUtil = checksumUtil;
    }

    /**
     * 
     * @param correctChecksum
     *            The presumed correct checksum. If the calculated checksum of
     *            the content does not match this checksum, a rety is triggered
     *            to ensure that there was no
     * @return
     * @throws TaskExecutionFailedException
     */
    public String
            getContentChecksum(final String correctChecksum) throws TaskExecutionFailedException {

        if (checked) {
            return contentChecksum;
        }

        // if (!isContentChecksumCalculated()) {
        // throw new DuraCloudRuntimeException(
        // BitIntegrityMessageUtil
        // .buildFailureMessage("Could not compute checksum: " +
        // "this storage provider does not provide access to content.",
        // bitTask,
        // storageProviderType));
        // }

        try {
            contentChecksum = new Retrier().execute(new Retriable() {
                @Override
                public String retry() throws Exception {
                    try (InputStream inputStream = store.getContent(bitTask
                            .getSpaceId(), bitTask.getContentId())) {
                        String checksum = checksumUtil
                                .generateChecksum(inputStream);
                        if (!correctChecksum.equals(checksum)) {
                            throw new ChecksumsDoNotMatchException(BitIntegrityMessageUtil
                                    .buildFailureMessage("The content checksum does not match specified checksum: ",
                                                         bitTask,
                                                         storageProviderType));
                        } else {
                            log.debug("success - content checksum matches specified checksum: {}",
                                      correctChecksum);
                            return checksum;
                        }
                    }
                }
            });
        } catch (NotFoundException | ChecksumsDoNotMatchException ex) {
            if (ex instanceof NotFoundException) {
                log.warn(BitIntegrityMessageUtil
                        .buildFailureMessage("Could not compute checksum  - content not found",
                                             bitTask,
                                             storageProviderType));
            }
        } catch (Exception e) {
            throw new BitIntegrityCheckTaskExecutionFailedException(BitIntegrityMessageUtil
                                                                            .buildFailureMessage("Could not compute checksum from content stream",
                                                                                                 bitTask,
                                                                                                 storageProviderType),
                                                                    e);
        }

        this.checked = true;
        return this.contentChecksum;
    }
}
