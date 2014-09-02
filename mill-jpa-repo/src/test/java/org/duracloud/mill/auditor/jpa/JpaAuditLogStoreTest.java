/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.auditor.jpa;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.duracloud.audit.AuditLogItem;
import org.duracloud.audit.AuditLogWriteFailedException;
import org.duracloud.error.NotFoundException;
import org.duracloud.mill.db.model.JpaAuditLogItem;
import org.duracloud.mill.db.repo.JpaAuditLogItemRepo;
import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Daniel Bernstein
 *         Date: Aug 29, 2014
 */
@RunWith(EasyMockRunner.class)
public class JpaAuditLogStoreTest extends EasyMockSupport{

    private JpaAuditLogStore auditLogStore;
    
    @Mock
    private JpaAuditLogItemRepo repo;
    

    
    @After
    public void tearDown(){
        verifyAll();
    }
    
    private String account = "account";
    private String storeId = "store-id";
    private String spaceId = "space-id";
    private String contentId= "content-id";
    private String contentMd5= "content-md5";
    private String mimetype = "mime-type";
    private String contentSize = "content-size";
    private String user = "user";
    private String action = "action";
    private String properties= "{}";
    private String spaceAcls= "{}";
    private String sourceSpaceId = "source-space-id";
    private String sourceContentId = "source-content-id";
    private Date timestamp = new Date();
    
    /**
     * Test method for
     * {@link org.duracloud.mill.auditor.jpa.JpaAuditLogStore#write(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date)}
     * .
     * @throws AuditLogWriteFailedException 
     */
    @Test
    public void testWrite() throws AuditLogWriteFailedException {

        Capture<JpaAuditLogItem> jpaItemCapture = new Capture<>();

        
        expect(repo.saveAndFlush(capture(jpaItemCapture)))
                .andReturn(new JpaAuditLogItem());
        
        replayAll();


        this.auditLogStore = new JpaAuditLogStore(repo);

        this.auditLogStore.write(account,
                                 storeId,
                                 spaceId,
                                 contentId,
                                 contentMd5,
                                 mimetype,
                                 contentSize,
                                 user,
                                 action,
                                 properties,
                                 spaceAcls,
                                 sourceSpaceId,
                                 sourceContentId,
                                 timestamp);
        
        JpaAuditLogItem item = jpaItemCapture.getValue();
        
        assertEquals(item.getAccount(), account);
        assertEquals(item.getSpaceId(), spaceId);
        assertEquals(item.getContentId(), contentId);
        assertEquals(item.getContentMd5(), contentMd5);
        assertEquals(item.getMimetype(), mimetype);
        assertEquals(item.getUsername(), user);
        assertEquals(item.getContentSize(), contentSize);
        assertEquals(item.getAction(), action);
        assertEquals(item.getContentProperties(), properties);
        assertEquals(item.getSpaceAcls(), spaceAcls);
        assertEquals(item.getSourceSpaceId(),sourceSpaceId);
        assertEquals(item.getSourceContentId(), sourceContentId);
        assertEquals(item.getTimestamp(), timestamp.getTime());

    }

    /**
     * Test method for {@link org.duracloud.mill.auditor.jpa.JpaAuditLogStore#getLogItems(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testGetLogItemsByAccountAndSpace() {
        this.auditLogStore = new JpaAuditLogStore(repo);
        Capture<Pageable> capture = new Capture<>();
        int count = 10;

        Page<JpaAuditLogItem> page = setupPage(count);
        expect(this.repo.findByAccountAndSpaceIdOrderByContentIdAsc(eq(account),
                                                                    eq(spaceId),
                                                                    capture(capture)))
                .andReturn(page);
        replayAll();

        Iterator<AuditLogItem> it = this.auditLogStore.getLogItems(account,
                                                                   spaceId);
        verifyIterator(count, it);
        verifyPageable(capture);
    }

    private void verifyPageable(Capture<Pageable> capture) {
        Pageable pageable = capture.getValue();
        assertNotNull(pageable);
        assertEquals(0,pageable.getPageNumber());
    }

    private void verifyIterator(int count, Iterator<AuditLogItem> it) {
        int recount = 0;
        while(it.hasNext()){
            it.next();
            recount++;
        }
        assertEquals(count, recount);
    }

    private Page<JpaAuditLogItem> setupPage(int count) {
        Page<JpaAuditLogItem> page = createMock(Page.class);
        
         List<JpaAuditLogItem> items = new ArrayList<>();
        for(int i = 0; i < count; i++){
            items.add(new JpaAuditLogItem());
        }
        expect(page.getContent()).andReturn(items);
        expect(page.getTotalPages()).andReturn(1);
        return page;
    }

    /**
     * Test method for {@link org.duracloud.mill.auditor.jpa.JpaAuditLogStore#getLogItems(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testGetLogItemsByAccountStoreIdSpaceIdAndContentId() {
        this.auditLogStore = new JpaAuditLogStore(repo);
        Capture<Pageable> capture = new Capture<>();
        int count = 10;

        Page<JpaAuditLogItem> page = setupPage(count);
        expect(this.repo
                .findByAccountAndStoreIdAndSpaceIdAndContentIdOrderByContentIdAsc(eq(account),
                                                                                  eq(storeId),
                                                                                  eq(spaceId),
                                                                                  eq(contentId),
                                                                                  capture(capture)))
                .andReturn(page);
        replayAll();

        Iterator<AuditLogItem> it = this.auditLogStore.getLogItems(account,
                                                                   storeId,
                                                                   spaceId,
                                                                   contentId);
        verifyIterator(count, it);
        verifyPageable(capture);
    }

    /**
     * Test method for {@link org.duracloud.mill.auditor.jpa.JpaAuditLogStore#getLatestLogItem(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     * @throws NotFoundException 
     */
    @Test
    public void testGetLatestLogItem() throws NotFoundException {
        this.auditLogStore = new JpaAuditLogStore(repo);
        expect(repo
                .findByAccountAndStoreIdAndSpaceIdAndContentIdOrderByTimestampDesc(account,
                                                                                   storeId,
                                                                                   spaceId,
                                                                                   contentId))
                .andReturn(Arrays.asList(new JpaAuditLogItem[] { new JpaAuditLogItem() }));
        replayAll();
        AuditLogItem item = this.auditLogStore.getLatestLogItem(account, storeId, spaceId, contentId);
        assertNotNull(item);
        
    }

    /**
     * Test method for {@link org.duracloud.mill.auditor.jpa.JpaAuditLogStore#updateProperties(org.duracloud.audit.AuditLogItem, java.lang.String)}.
     * @throws AuditLogWriteFailedException 
     */
    @Test
    public void testUpdateProperties() throws AuditLogWriteFailedException {
        this.auditLogStore = new JpaAuditLogStore(repo);
        JpaAuditLogItem item = new JpaAuditLogItem();
        Long id = 100l;
        item.setId(id);
        String serializedProps = "{}";
        
        JpaAuditLogItem freshItem = createMock(JpaAuditLogItem.class);
        expect(repo.findOne(eq(id))).andReturn(freshItem);
        freshItem.setContentProperties(serializedProps);
        expectLastCall();
        expect(repo.saveAndFlush(freshItem)).andReturn(freshItem);
        replayAll();
        
        this.auditLogStore.updateProperties(item, serializedProps);
        
        
        
    }

}
