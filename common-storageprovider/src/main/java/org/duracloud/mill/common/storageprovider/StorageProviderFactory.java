/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.common.storageprovider;

import org.duracloud.glacierstorage.GlacierStorageProvider;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.rackspacestorage.RackspaceStorageProvider;
import org.duracloud.s3storage.S3StorageProvider;
import org.duracloud.sdscstorage.SDSCStorageProvider;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;

/**
 * The class knows how to create a <code>StorageProvider</code> based on a set of credentials.
 * @author Daniel Bernstein
 *	       Date: Nov 6, 2013
 */
public class StorageProviderFactory {
    /**
     * @param credentials
     * @param storageProviderType
     * @return
     */
    public StorageProvider create(StorageProviderCredentials credentials) {
        
        StorageProviderType storageProviderType = credentials.getProviderType();
        
        if (storageProviderType.equals(StorageProviderType.AMAZON_S3)) {
            return new S3StorageProvider(credentials.getAccessKey(), credentials.getSecretKey());
        } else if (storageProviderType.equals(StorageProviderType.SDSC)) {
            return new SDSCStorageProvider(credentials.getAccessKey(),
                    credentials.getSecretKey());
        } else if (storageProviderType.equals(StorageProviderType.AMAZON_GLACIER)) {
            return new GlacierStorageProvider(credentials.getAccessKey(),
                    credentials.getSecretKey());
        } else if (storageProviderType.equals(StorageProviderType.RACKSPACE)) {
            return new RackspaceStorageProvider(credentials.getAccessKey(),
                    credentials.getSecretKey());
        }
        throw new RuntimeException(storageProviderType
                + " is not a supported storage provider type");
    }    

}
