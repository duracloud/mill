/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.common.storageprovider;

import org.duracloud.audit.provider.AuditStorageProvider;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.util.UserUtil;
import org.duracloud.glacierstorage.GlacierStorageProvider;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.util.SimpleUserUtil;
import org.duracloud.s3storage.S3StorageProvider;
import org.duracloud.snapshotstorage.ChronopolisStorageProvider;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.duracloud.swiftstorage.SwiftStorageProvider;

/**
 * The class knows how to create a <code>StorageProvider</code> based on a set of credentials.
 *
 * @author Daniel Bernstein
 * Date: Nov 6, 2013
 */
public class StorageProviderFactory {

    /**
     * Creates a StorageProvider which captures events and passes them to the
     * audit queue.
     *
     * @param credentials      needed to connect to storage provider
     * @param accountSubdomain subdomain of the storage provider account
     * @param auditQueue       used to capture changes to stored content
     * @return
     */
    public StorageProvider createWithAudit(StorageProviderCredentials credentials,
                                           String accountSubdomain,
                                           TaskQueue auditQueue) {
        UserUtil userUtil = new SimpleUserUtil();
        StorageProvider storageprovider = create(credentials);
        StorageProvider auditProvider =
            new AuditStorageProvider(storageprovider,
                                     accountSubdomain,
                                     credentials.getProviderId(),
                                     credentials.getProviderType().getName(),
                                     userUtil,
                                     auditQueue);
        return auditProvider;
    }

    /**
     * Creates a StorageProvider
     *
     * @param credentials
     * @return
     */
    public StorageProvider create(StorageProviderCredentials credentials) {

        StorageProviderType storageProviderType = credentials.getProviderType();

        if (storageProviderType.equals(StorageProviderType.AMAZON_S3)) {
            return new S3StorageProvider(credentials.getAccessKey(),
                                         credentials.getSecretKey(),
                                         credentials.getOptions());
        } else if (storageProviderType.equals(StorageProviderType.AMAZON_GLACIER)) {
            return new GlacierStorageProvider(credentials.getAccessKey(),
                                              credentials.getSecretKey(),
                                              credentials.getOptions());
        } else if (storageProviderType.equals(StorageProviderType.CHRONOPOLIS)) {
            return new ChronopolisStorageProvider(credentials.getAccessKey(),
                                                  credentials.getSecretKey(),
                                                  credentials.getOptions());
        } else if (storageProviderType.equals(StorageProviderType.SWIFT_S3)) {
            return new SwiftStorageProvider(credentials.getAccessKey(),
                                            credentials.getSecretKey(),
                                            credentials.getOptions());
        }
        throw new RuntimeException(storageProviderType
                                   + " is not a supported storage provider type");
    }

}
