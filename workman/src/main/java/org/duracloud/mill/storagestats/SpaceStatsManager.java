/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats;

import java.util.Date;

import org.duracloud.mill.db.model.SpaceStats;
import org.duracloud.mill.db.repo.JpaSpaceStatsRepo;
import org.duracloud.mill.db.repo.MillJpaRepoConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Daniel Bernstein
 *         Date: Mar 1, 2016
 */
@Component
public class SpaceStatsManager {
    private JpaSpaceStatsRepo repo;
    public SpaceStatsManager(JpaSpaceStatsRepo repo){
        this.repo = repo;
    }
    
    @Transactional(MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
    public SpaceStats addSpaceStats(Date timestamp,
                                    String account,
                                    String storeId,
                                    String spaceId,
                                    long byteCount,
                                    long objectCount) {
        return repo
                .save(new SpaceStats(timestamp,
                                     account,
                                     storeId,
                                     spaceId,
                                     byteCount,
                                     objectCount));
    }
}
