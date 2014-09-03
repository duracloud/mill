/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest.jpa;

import java.text.MessageFormat;
import java.util.Iterator;

import org.duracloud.common.collection.StreamingIterator;
import org.duracloud.error.NotFoundException;
import org.duracloud.mill.db.model.ManifestItem;
import org.duracloud.mill.db.repo.JpaManifestItemRepo;
import org.duracloud.mill.db.util.JpaIteratorSource;
import org.duracloud.mill.manifest.ManifestItemWriteException;
import org.duracloud.mill.manifest.ManifestStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class JpaManifestStore implements
                             ManifestStore {
    private static Logger log = LoggerFactory.getLogger(JpaManifestStore.class);
    private JpaManifestItemRepo manifestItemRepo;

    @Autowired
    public JpaManifestStore(JpaManifestItemRepo manifestItemRepo) {
        this.manifestItemRepo = manifestItemRepo;
    }

    @Override
    public void write(String account,
                      String storeId,
                      String spaceId,
                      String contentId,
                      String contentChecksum) throws ManifestItemWriteException {

        log.debug("preparing to write {}, {}, {}, {}, {}",
                  account,
                  storeId,
                  spaceId,
                  contentId,
                  contentChecksum);
        ManifestItem item = new ManifestItem();

        try {
            item.setAccount(account);
            item.setStoreId(storeId);
            item.setSpaceId(spaceId);
            item.setContentId(contentId);
            item.setContentChecksum(contentChecksum);
            ManifestItem result = this.manifestItemRepo.saveAndFlush(item);
            log.info("successfully wrote {} to the jpa repo.", result);

        } catch (Exception ex) {
            throw new ManifestItemWriteException(ex, item);
        }
    }

    @Override
    public Iterator<ManifestItem> getItems(final String account,
                                           final String storeId,
                                           final String spaceId) {
        return (Iterator) new StreamingIterator<ManifestItem>(new JpaIteratorSource<JpaManifestItemRepo, ManifestItem>(this.manifestItemRepo) {
            @Override
            protected Page<ManifestItem> getNextPage(Pageable pageable,
                                                     JpaManifestItemRepo repo) {
                return manifestItemRepo
                        .findByAccountAndStoreIdAndSpaceIdOrderByContentIdAsc(account,
                                                                              storeId,
                                                                              spaceId,
                                                                              pageable);
            }
        });
    }

    @Override
    public ManifestItem
            getItem(final String account,
                    final String storeId,
                    final String spaceId,
                    final String contentId) throws NotFoundException {
        ManifestItem item = this.manifestItemRepo
                .findByAccountAndStoreIdAndSpaceIdAndContentId(account,
                                                               storeId,
                                                               spaceId,
                                                               contentId);
        if (item == null) {
            throw new NotFoundException(MessageFormat.format("No ManifestItem could be found matching the specified params: account={0}, storeId={1}, spaceId={2}, contentId={3}",
                                        account,
                                        storeId,
                                        spaceId,
                                        contentId));
        }
        
        return item;
    }

}
