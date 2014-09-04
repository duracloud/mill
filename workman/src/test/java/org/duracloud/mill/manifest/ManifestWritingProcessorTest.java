/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import java.util.Date;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.audit.task.AuditTask.ActionType;
import org.duracloud.mill.test.AbstractTestBase;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.easymock.Mock;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *         Date: Sep 4, 2014
 */
public class ManifestWritingProcessorTest extends AbstractTestBase{
    
    @Mock
    private AuditTask task;
    
    @Mock
    private ManifestStore store;

    private String account = "account";
    private String storeId = "store-id";
    private String spaceId = "space-id";
    private String contentId = "content-id";
    private String contentChecksum = "content-checksum";
    private String contentSize = "content-size";
    private String contentMimetype = "content-mimetype";
    private Long dateTime = System.currentTimeMillis();
    
    @Test
    public void testAddContentTask() throws TaskExecutionFailedException, ManifestItemWriteException {
        setupAddUpdate(ActionType.ADD_CONTENT);
    }

    @Test
    public void testCopyContent() throws TaskExecutionFailedException, ManifestItemWriteException {
        setupAddUpdate(ActionType.COPY_CONTENT);
    }

    private void
            setupAddUpdate(ActionType action) throws ManifestItemWriteException,
                                             TaskExecutionFailedException {
        setupTask(action);

        expect(task.getContentMimetype()).andReturn(contentMimetype);
        expect(task.getContentSize()).andReturn(contentSize);
        expect(task.getContentChecksum()).andReturn(contentChecksum);
        
        this.store.addUpdate(eq(account),
                             eq(storeId),
                             eq(spaceId),
                             eq(contentId),
                             eq(contentChecksum),
                             eq(contentMimetype),
                             eq(contentSize),
                             eq(new Date(dateTime)));
        replayAll();
        executeProcessor();
    }

    private void setupTask(ActionType action) {
        expect(task.getAccount()).andReturn(account);
        expect(task.getStoreId()).andReturn(storeId);
        expect(task.getSpaceId()).andReturn(spaceId);
        expect(task.getContentId()).andReturn(contentId);
        expect(task.getAction()).andReturn(action.name());
        expect(task.getDateTime()).andReturn(dateTime+"");
    }
    
    @Test
    public void testDeleteContent() throws TaskExecutionFailedException, ManifestItemWriteException {
        setupTask(ActionType.DELETE_CONTENT);
        this.store.flagAsDeleted(eq(account),
                             eq(storeId),
                             eq(spaceId),
                             eq(contentId),
                             eq(new Date(dateTime)));
        replayAll();
        executeProcessor();
    }

    @Test
    public void testCreateSpace() throws TaskExecutionFailedException {
        testIgnoreAction(ActionType.CREATE_SPACE);
    }

    @Test
    public void testDeleteSpace() throws TaskExecutionFailedException {
        testIgnoreAction(ActionType.DELETE_SPACE);
    }

    @Test
    public void testSetContentProperties() throws TaskExecutionFailedException {
        testIgnoreAction(ActionType.SET_CONTENT_PROPERTIES);
    }

    @Test
    public void testSetSpaceAcls() throws TaskExecutionFailedException {
        testIgnoreAction(ActionType.SET_SPACE_ACLS);
    }

    private void testIgnoreAction(ActionType action) throws TaskExecutionFailedException {
        setupTask(action);
        replayAll();
        executeProcessor();
    }

    
    private void executeProcessor() throws TaskExecutionFailedException {
        ManifestWritingProcessor processor = new ManifestWritingProcessor(task, store);
        processor.execute();
    }
    
    

}
