/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest.jpa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.duracloud.audit.AuditLogItem;
import org.duracloud.mill.auditor.jpa.JpaAuditLogStore;
import org.duracloud.mill.db.model.JpaAuditLogItem;
import org.duracloud.mill.db.model.ManifestItem;
import org.duracloud.mill.db.repo.JpaManifestItemRepo;
import org.duracloud.mill.manifest.ManifestItemWriteException;
import org.duracloud.mill.test.AbstractTestBase;
import org.duracloud.mill.test.jpa.JpaTestBase;
import org.easymock.Capture;
import org.easymock.Mock;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Daniel Bernstein Date: Sep 2, 2014
 */
public class JpaManifestStoreTest extends JpaTestBase<ManifestItem> {

    @Mock
    private JpaManifestItemRepo repo;

    private JpaManifestStore store;
    private String account = "account";
    private String storeId = "store-id";
    private String spaceId = "space-id";
    private String contentId = "content-id";
    private String contentChecksum = "content-checksum";

    /**
     * Test method for
     * {@link org.duracloud.mill.manifest.jpa.JpaManifestStore#write(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * .
     * 
     * @throws ManifestItemWriteException
     */
    @Test
    public void testWrite() throws ManifestItemWriteException {
        createTestSubject();
        Capture<ManifestItem> capture = new Capture<>();
        ManifestItem returnItem = createMock(ManifestItem.class);
        expect(repo.saveAndFlush(capture(capture))).andReturn(returnItem);
        replayAll();
        store.write(account, storeId, spaceId, contentId, contentChecksum);
        ManifestItem item = capture.getValue();
        assertEquals(account, item.getAccount());
        assertEquals(storeId, item.getStoreId());
        assertEquals(spaceId, item.getSpaceId());
        assertEquals(contentId, item.getContentId());
        assertEquals(contentChecksum, item.getContentChecksum());
    }

    private void createTestSubject() {
        store = new JpaManifestStore(repo);
    }

    /**
     * Test method for
     * {@link org.duracloud.mill.manifest.jpa.JpaManifestStore#getItems(java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testGetItems() {
        createTestSubject();

        Capture<Pageable> capture = new Capture<>();
        int count = 10;

        Page<ManifestItem> page = setupPage(count);
        expect(this.repo.findByAccountAndStoreIdAndSpaceIdOrderByContentIdAsc(eq(account),
                                                                              eq(storeId),
                                                                              eq(spaceId),
                                                                              capture(capture)))
                .andReturn(page);
        replayAll();

        Iterator<ManifestItem> it = this.store.getItems(account,
                                                        storeId,
                                                        spaceId);
        verifyIterator(count, it);
        verifyPageable(capture);
    }

    /**
     * Test method for
     * {@link org.duracloud.mill.manifest.jpa.JpaManifestStore#getItem(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testGetItem() throws Exception {
        createTestSubject();
        ManifestItem returnItem = createMock(ManifestItem.class);
        expect(repo.findByAccountAndStoreIdAndSpaceIdAndContentId(account,
                                                                  storeId,
                                                                  spaceId,
                                                                  contentId))
                .andReturn(returnItem);
        replayAll();
        assertNotNull(store.getItem(account, storeId, spaceId, contentId));
    }

  
    /* (non-Javadoc)
     * @see org.duracloud.mill.test.jpa.JpaTestBase#create()
     */
    @Override
    protected ManifestItem create() {
        return createMock(ManifestItem.class);
    }

}
