/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagereporter;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.duracloud.account.db.model.AccountInfo;
import org.duracloud.account.db.model.AccountInfo.AccountStatus;
import org.duracloud.account.db.model.StorageProviderAccount;
import org.duracloud.account.db.repo.DuracloudAccountRepo;
import org.duracloud.mill.db.repo.JpaSpaceStatsRepo;
import org.duracloud.mill.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the logic for generating storage reports. The storage
 * report shows which accounts contain oversubscribed storage providers. An
 * oversubscribed account is an account with at least one storage provider whose
 * most recent byte count exceeds the allocation set on the storage provider's
 * "storage limit" property. The report shows the total for each storage
 * provider, the storage limit, and the % capacity of that storage provider.
 * 
 * @author dbernstein
 * @since: Jun 29, 2017
 */
public class StorageReporter {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(StorageReporter.class);

    private JpaSpaceStatsRepo statsRepo;
    private DuracloudAccountRepo accountRepo;
    private NotificationManager notification;

    /**
     * @param statsRepo
     * @param accountRepo
     * @param notification
     */
    public StorageReporter(JpaSpaceStatsRepo statsRepo,
                           DuracloudAccountRepo accountRepo,
                           NotificationManager notification) {

        this.statsRepo = statsRepo;
        this.accountRepo = accountRepo;
        this.notification = notification;
    }

    /**
     * @return
     * 
     */
    public StorageReportResult run() {
        // for each active account
        List<AccountStorageReportResult> oversubscribedAccounts = new LinkedList<>();
        List<AccountStorageReportResult> undersubscribedAccounts = new LinkedList<>();

        Date now = new Date();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        Date lastMonth = c.getTime();
        List<AccountInfo> accounts = accountRepo
                .findByStatus(AccountStatus.ACTIVE);
        Collections.sort(accounts, new Comparator<AccountInfo>() {
            @Override
            public int compare(AccountInfo o1, AccountInfo o2) {
                return o1.getAcctName().compareTo(o2.getAcctName());
            }
        });

        for (AccountInfo account : accounts) {
            String accountId = account.getSubdomain();
            LOGGER.info("processing {}", accountId);
            // gather all storage providers for an account into a single list.
            StorageProviderAccount primary = account
                    .getPrimaryStorageProviderAccount();
            Set<StorageProviderAccount> secondary = account
                    .getSecondaryStorageProviderAccounts();
            List<StorageProviderAccount> providers = new LinkedList<>(secondary);
            providers.add(0, primary);

            AccountStorageReportResult result = new AccountStorageReportResult(account);

            // for each storage provider
            for (StorageProviderAccount storageProviderAccount : providers) {
                List<Object[]> stats = statsRepo
                        .getByAccountIdAndStoreId(accountId,
                                                  storageProviderAccount.getId()
                                                          + "",
                                                  lastMonth,
                                                  now,
                                                  JpaSpaceStatsRepo.INTERVAL_DAY);
                long total = 0;
                if (stats != null && stats.size() > 0) {
                    total = ((BigDecimal) stats.get(stats.size() - 1)[3])
                            .longValue();
                }
                result.addStorageProviderResult(storageProviderAccount, total);
            }

            if (result.isOversubscribed()) {
                oversubscribedAccounts.add(result);
            } else {
                undersubscribedAccounts.add(result);
            }

        }

        // build subject
        String subject = MessageFormat.format(
                                              "Storage Report:  {0} Oversubscribed account(s)",
                                              oversubscribedAccounts.size());

        StringBuilder body = new StringBuilder();

        if (oversubscribedAccounts.size() == 0) {
            body.append("Presently there are no oversubscribed accounts.\n\n");
        } else {
            body.append("Oversubscribed Accounts: \n\n");
        }
        // write oversubscribed in initial block
        for (AccountStorageReportResult result : oversubscribedAccounts) {
            appendResultToBody(result, body);
        }

        body.append("\nAll other (ie undersubscribedAccounts) accounts: \n\n");

        // write undersubscribedAccounts in following block
        for (AccountStorageReportResult result : undersubscribedAccounts) {
            appendResultToBody(result, body);
        }

        LOGGER.info("sending notification...");

        // send email
        notification.sendEmail(subject, body.toString());

        LOGGER.info("Report complete: {} oversubscribed,  {} undersubscribedAccounts.",
                    oversubscribedAccounts.size(),
                    undersubscribedAccounts.size());

        return new StorageReportResult(oversubscribedAccounts,
                                       undersubscribedAccounts);
    }

    /**
     * @param result
     * @param body
     */
    private void appendResultToBody(AccountStorageReportResult result,
                                    StringBuilder body) {
        AccountInfo account = result.getAccount();
        body.append(MessageFormat.format("{0} (subdomain={1}):\n",
                                         account.getAcctName(),
                                         account.getSubdomain()));
        for (StorageProviderResult presult : result
                .getStorageProviderResults()) {
            StorageProviderAccount spa = presult.getStorageProviderAccount();
            int limit = spa.getStorageLimit();
            String total = new BigDecimal(presult.getTotalBytes())
                    .divide(new BigDecimal(StorageProviderResult.TB),
                            2,
                            BigDecimal.ROUND_HALF_UP)
                    .setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            String capacity = new BigDecimal(presult.getTotalBytes())
                    .divide(new BigDecimal(limit * StorageProviderResult.TB),
                            2,
                            BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(2, BigDecimal.ROUND_HALF_UP).toString();

            String line = MessageFormat.format(
                                               "    provider={0}/{1}, current total: {2} TB, allocated storage: {3} TB, capacity: {4} %, status: {5}\n",
                                               spa.getId() + "",
                                               spa.getProviderType().name(),
                                               total,
                                               limit,
                                               capacity,
                                               presult.isOversubscribed()
                                                       ? "OVERSUBSCRIBED"
                                                       : "OK");
            body.append(line);

        }
    }
}
