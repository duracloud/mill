/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog.jpa;

import java.util.List;

import org.duracloud.mill.bitlog.BitLogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Daniel Bernstein
 * Date: Oct 17, 2014
 */
@Repository(value = "bitLogItemRepo")
public interface JpaBitLogItemRepo extends JpaRepository<JpaBitLogItem, Long> {

    /**
     * @param account
     * @param storeId
     * @param spaceId
     * @param pageable
     * @return
     */
    public Page<JpaBitLogItem> findByAccountAndStoreIdAndSpaceId(String account,
                                                                 String storeId,
                                                                 String spaceId,
                                                                 Pageable pageable);

    /**
     * @param account
     * @param storeId
     * @param spaceId
     */
    @Modifying
    @Query(nativeQuery = true,
           value = "delete from bit_log_item where account=?1 and store_id=?2 and space_id=?3 limit 50000")
    public int deleteFirst50000ByAccountAndStoreIdAndSpaceId(String account,
                                                             String storeId,
                                                             String spaceId);

    /**
     * @param account
     * @param storeId
     * @param spaceId
     * @return
     */
    @Query("select b from JpaBitLogItem b where b.account = ?1 and b.storeId = ?2 and b.spaceId = ?3 and (b.result = " +
           "'ERROR' or b.result = 'FAILURE')")
    public List<BitLogItem> findErrorsAndFailures(String account,
                                                  String storeId,
                                                  String spaceId);
}
