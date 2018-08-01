/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.impl;

import java.util.HashSet;
import java.util.Set;

import org.duracloud.account.db.model.AccountInfo;
import org.duracloud.account.db.model.StorageProviderAccount;
import org.duracloud.account.db.repo.DuracloudAccountRepo;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.StorageProviderCredentials;
import org.duracloud.mill.credentials.StorageProviderCredentialsNotFoundException;
import org.duracloud.storage.domain.StorageProviderType;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A integration test for the DefaultCredentialsRepoImplTest.
 *
 * @author Daniel Bernstein
 * Date: 7/3/2014
 */
@RunWith(EasyMockRunner.class)
public class DefaultCredentialsRepoImplTest extends EasyMockSupport {
    @TestSubject
    private DefaultCredentialsRepoImpl repo;
    private String testSubdomain = "test";
    private String testStoreId = "1";

    @Mock
    DuracloudAccountRepo accountRepo;

    @Mock
    private AccountInfo accountInfo;

    @Mock
    private StorageProviderAccount primary;

    @Mock
    private StorageProviderAccount secondary;

    @Before
    public void setup() {

        repo = new DefaultCredentialsRepoImpl(accountRepo);
    }

    @After
    public void tearDown() {
        verifyAll();
    }

    private void setupStorageProviderAccount(StorageProviderAccount account, String storeId, boolean target) {
        EasyMock.expect(account.getId()).andReturn(Long.valueOf(storeId));
        if (target) {
            EasyMock.expect(account.getPassword()).andReturn("password");
            EasyMock.expect(account.getUsername()).andReturn("username");
            EasyMock.expect(account.getProviderType()).andReturn(StorageProviderType.AMAZON_S3);
            EasyMock.expect(account.getProperties()).andReturn(null);
        }
    }

    @Test
    public void testGetStorageProviderCredentialsPrimary()
        throws AccountCredentialsNotFoundException,
        StorageProviderCredentialsNotFoundException {

        setupPrimary(testStoreId, true);

        replayAll();
        StorageProviderCredentials creds = repo
            .getStorageProviderCredentials(testSubdomain, testStoreId);
        Assert.assertNotNull(creds);
    }

    @Test
    public void testGetStorageProviderCredentialsSecondary()
        throws AccountCredentialsNotFoundException,
        StorageProviderCredentialsNotFoundException {

        setupPrimary("2", false);
        setupStorageProviderAccount(secondary, testStoreId, true);
        Set<StorageProviderAccount> secondaries = new HashSet<>();
        secondaries.add(secondary);
        EasyMock.expect(accountInfo.getSecondaryStorageProviderAccounts()).andReturn(secondaries);

        replayAll();
        StorageProviderCredentials creds = repo
            .getStorageProviderCredentials(testSubdomain, testStoreId);
        Assert.assertNotNull(creds);
    }

    private void setupPrimary(String storeId, boolean target) {
        setupStorageProviderAccount(primary, storeId, target);
        EasyMock.expect(accountInfo.getPrimaryStorageProviderAccount()).andReturn(primary);
        EasyMock.expect(accountRepo.findBySubdomain(testSubdomain)).andReturn(accountInfo);
    }

    @Test
    public void testAccountNotFound() {

        EasyMock.expect(accountRepo.findBySubdomain(testSubdomain)).andReturn(null);
        replayAll();
        try {
            repo.getStorageProviderCredentials(testSubdomain, testStoreId);
            Assert.assertTrue(false);
        } catch (AccountCredentialsNotFoundException e) {
            Assert.assertTrue(true);
        } catch (StorageProviderCredentialsNotFoundException e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testStorageProviderAccountNotFound() {

        setupPrimary("2", false);
        Set<StorageProviderAccount> secondaries = new HashSet<>();
        EasyMock.expect(accountInfo.getSecondaryStorageProviderAccounts()).andReturn(secondaries);
        replayAll();

        try {
            repo.getStorageProviderCredentials(testSubdomain, testStoreId);
            Assert.assertTrue(false);
        } catch (AccountCredentialsNotFoundException e) {
            Assert.assertTrue(false);
        } catch (StorageProviderCredentialsNotFoundException e) {
            Assert.assertTrue(true);
        }

    }

}
