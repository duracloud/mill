/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagereporter;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.duracloud.account.db.model.AccountInfo;
import org.duracloud.account.db.model.AccountInfo.AccountStatus;
import org.duracloud.account.db.model.StorageProviderAccount;
import org.duracloud.account.db.repo.DuracloudAccountRepo;
import org.duracloud.mill.db.repo.JpaSpaceStatsRepo;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.storage.domain.StorageProviderType;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author dbernstein
 * @since: Jun 29, 2017
 */
@RunWith(EasyMockRunner.class)
public class StorageReporterTest extends EasyMockSupport {

    @Mock
    private JpaSpaceStatsRepo statsRepo;

    @Mock
    private DuracloudAccountRepo accountRepo;

    @Mock
    private NotificationManager notification;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        verifyAll();
    }

    @Test
    public void test() throws Exception {
        String accountId = "test";
        Long primaryId = 1l;
        Long secondaryId = 2l;

        AccountInfo account = createMock(AccountInfo.class);
        expect(account.getAcctName()).andReturn("Account Name").atLeastOnce();
        StorageProviderAccount primary = createMock(StorageProviderAccount.class);
        expect(primary.getId()).andReturn(primaryId).atLeastOnce();
        expect(primary.getStorageLimit()).andReturn(2).atLeastOnce();
        expect(primary.getProviderType())
            .andReturn(StorageProviderType.AMAZON_S3).atLeastOnce();

        StorageProviderAccount secondary = createMock(StorageProviderAccount.class);
        expect(secondary.getId()).andReturn(secondaryId).atLeastOnce();
        expect(secondary.getStorageLimit()).andReturn(2).atLeastOnce();
        expect(secondary.getProviderType())
            .andReturn(StorageProviderType.AMAZON_GLACIER).atLeastOnce();

        expect(account.getSubdomain()).andReturn(accountId).atLeastOnce();
        expect(account.getPrimaryStorageProviderAccount()).andReturn(primary);
        expect(account.getSecondaryStorageProviderAccounts())
            .andReturn(new HashSet<StorageProviderAccount>(Arrays
                                                               .asList(secondary)));

        expect(accountRepo.findByStatus(AccountStatus.ACTIVE))
            .andReturn(Arrays.asList(account));

        List<Object[]> primaryStats = new LinkedList<>();
        primaryStats.add(new Object[] {null, null, null,
                                       new BigDecimal(StorageProviderResult.TB * 2 + 1)});
        List<Object[]> secondaryStats = new LinkedList<>();
        secondaryStats.add(new Object[] {null, null, null,
                                         new BigDecimal(StorageProviderResult.TB * 2 - 1)});

        expect(statsRepo
                   .getByAccountIdAndStoreId(eq(accountId),
                                             eq(primaryId + ""),
                                             isA(Date.class),
                                             isA(Date.class),
                                             eq(JpaSpaceStatsRepo.INTERVAL_DAY)))
            .andReturn(primaryStats);
        expect(statsRepo
                   .getByAccountIdAndStoreId(eq(accountId),
                                             eq(secondaryId + ""),
                                             isA(Date.class),
                                             isA(Date.class),
                                             eq(JpaSpaceStatsRepo.INTERVAL_DAY)))
            .andReturn(secondaryStats);

        notification.sendEmail(isA(String.class), isA(String.class));
        expectLastCall();

        replayAll();
        StorageReporter reporter = new StorageReporter(statsRepo,
                                                       accountRepo,
                                                       notification);
        StorageReportResult result = reporter.run();

        assertEquals(1, result.getOversubscribedAccounts().size());
        assertEquals(0, result.getUndersubscribedAccounts().size());
        List<StorageProviderResult> spResults = result
            .getOversubscribedAccounts().get(0).getStorageProviderResults();
        for (StorageProviderResult spResult : spResults) {
            if (spResult.getStorageProviderAccount().getId()
                        .equals(primaryId)) {
                assertTrue(spResult.isOversubscribed());
            } else {
                assertFalse(spResult.isOversubscribed());
            }
        }
    }

}