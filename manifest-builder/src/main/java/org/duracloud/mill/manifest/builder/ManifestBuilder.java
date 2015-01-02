/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest.builder;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.duracloud.client.ContentStore;
import org.duracloud.common.util.DateUtil;
import org.duracloud.mill.db.repo.MillJpaRepoConfig;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Daniel Bernstein Date: Jan 2, 2015
 */
@Component
public class ManifestBuilder {
    private static final Logger log = LoggerFactory
            .getLogger(ManifestBuilder.class);
    private Collection<ContentStore> contentStores;
    private List<String> spaceList;
    private String account;
    private boolean dryRun;
    private boolean clean;
    private ManifestStore manifestStore;

    @Autowired
    public ManifestBuilder(ManifestStore manifestStore) {
        this.manifestStore = manifestStore;
    }

    /**
     * 
     * @param account
     * @param contentStores
     * @param spaceList
     * @param manifestItemRepo
     * @param clean
     * @param dryRun
     */
    public void init(String account,
                     Collection<ContentStore> contentStores,
                     List<String> spaceList,
                     boolean clean,
                     boolean dryRun) {
        this.account = account;
        this.contentStores = contentStores;
        this.spaceList = spaceList;
        this.clean = clean;
        this.dryRun = dryRun;

    }

    public void execute() throws Exception {
        if(clean){
            clean();
        }
        build();
    }

    /**
     * 
     */
    private void build() throws Exception {
        for (ContentStore store : contentStores) {
            String storeId = store.getStoreId();
            List<String> spaces = store.getSpaces();
            for (String spaceId : spaces) {
                if (spaceList.isEmpty() || spaceList.contains(spaceId)) {
                    buildSpace(storeId, spaceId, store);
                }
            }
        }
    }

    /**
     * @param spaceId
     * @param spaceId 
     * @param store
     */
    private void
            buildSpace(String storeId, String spaceId, ContentStore store) throws Exception {
        log.info("starting manifest rebuild for storeId={} spaceId={}",storeId, spaceId);
        Iterator<String> contentIds = store.getSpaceContents(spaceId);
        while (contentIds.hasNext()) {
            String contentId = contentIds.next();
            updateContentId(storeId,spaceId, contentId,store);
        }
        log.info("completed manifest rebuild for storeId={} spaceId={}",storeId, spaceId);

    }

    /**
     * @param store
     * @param spaceId
     * @param contentId
     */
    private void updateContentId(String storeId,
                                 String spaceId,
                                 String contentId,
                                 ContentStore store) throws Exception {
        String message = MessageFormat
                .format("rebuilt manifest entry for storeId={0} spaceId=\"{1}\" contentId=\"{2}\"",
                        storeId,
                        spaceId,
                        contentId);
        if (dryRun) {
            log.info("(dry run: no update) - " + message);
        } else {
            log.debug("about to rebuild manifest entry for storeId={} spaceId=\"{}\" contentId=\"{}\"",
                      storeId,
                      spaceId,
                      contentId);
            Map<String, String> props = store.getContentProperties(spaceId,
                                                                   contentId);
            String modified = props
                    .get(StorageProvider.PROPERTIES_CONTENT_MODIFIED);
            Date timeStamp;

            try {
                timeStamp = DateUtil.convertToDate(modified);
            } catch (Exception ex) {
                timeStamp = new Date();
            }

            manifestStore.addUpdate(account, 
                                    storeId, 
                                    spaceId, 
                                    contentId, 
                                    props.get(StorageProvider.PROPERTIES_CONTENT_CHECKSUM), 
                                    props.get(StorageProvider.PROPERTIES_CONTENT_MIMETYPE), 
                                    props.get(StorageProvider.PROPERTIES_CONTENT_SIZE), 
                                    timeStamp);

            log.info(message);

        }
    }

    /**
     * 
     */
    private void clean() throws Exception {
        for (ContentStore store : contentStores) {
            String storeId = store.getStoreId();
            List<String> spaces = store.getSpaces();
            for (String spaceId : spaces) {
                if (spaceList.isEmpty() || spaceList.contains(spaceId)) {
                    if (!dryRun) {
                        manifestStore.delete(account, storeId, spaceId);
                        log.info("manifest deleted for storeId={} spaceId={}",
                                 storeId,
                                 spaceId);
                    } else {
                        log.info("you're in dry run mode: manifest for storeId={} spaceId={} will be "
                                         + "deleted when doing a \"wet\" run. No modifications have been made.",
                                 storeId,
                                 spaceId);
                    }
                }
            }
        }
    }
}
