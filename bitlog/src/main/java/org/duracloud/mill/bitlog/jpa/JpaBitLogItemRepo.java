/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bitlog.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Daniel Bernstein
 *         Date: Oct 17, 2014
 */
@Repository(value="bitLogItemRepo")
public interface JpaBitLogItemRepo extends JpaRepository<JpaBitLogItem, Long> {

    /**
     * @param account
     * @param storeId
     * @param spaceId
     * @param pageable
     * @return
     */
            Page<JpaBitLogItem>
            findByAccountAndStoreIdAndSpaceIdOrderByContentIdAsc(String account,
                                                                 String storeId,
                                                                 String spaceId,
                                                                 Pageable pageable);


}
