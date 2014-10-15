/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.io.IOException;
import java.io.InputStream;

import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

/**
 * @author Daniel Bernstein
 *         Date: Oct 15, 2014
 */
@RunWith(EasyMockRunner.class)
public class ContentChecksumHelperTest extends EasyMockSupport {
    private String spaceId = "spaceId";
    private String contentId = "contentId";
    private String checksum = "checksum";
    private String account = "account";
    private String storeId = "storeId";
    
    @Mock
    private StorageProvider store;
    
    @Mock
    private BitIntegrityCheckTask task;
    
    @Mock
    private ChecksumUtil checksumUtil;
    
    @Mock 
    private InputStream is;
    /**
     * @throws java.lang.Exception
     */
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

    /**
     * Test method for {@link org.duracloud.mill.bit.ContentChecksumHelper#getContentChecksum(java.lang.String)}.
     * @throws TaskExecutionFailedException 
     * @throws IOException 
     */
    @Test
    public void testGetContentChecksum() throws TaskExecutionFailedException, IOException {
        
        setupTask(1);
        setupStorageProvider(1);
        setupChecksumUtil(checksum);
        replayAll();
        ContentChecksumHelper helper = new ContentChecksumHelper(StorageProviderType.AMAZON_S3, task, store, checksumUtil);
        String resultChecksum = helper.getContentChecksum(checksum);
        assertEquals(resultChecksum, helper.getContentChecksum(checksum));
    }

    @Test
    public void testRetry() throws Exception{
        String badChecksum = "bad-checksum";
        setupTask(3);
        expect(task.getAccount()).andReturn(account);
        expect(task.getStoreId()).andReturn(storeId);
        
        setupStorageProvider(2);
        setupChecksumUtil(badChecksum);
        setupChecksumUtil(checksum);

        replayAll();
        ContentChecksumHelper helper = new ContentChecksumHelper(StorageProviderType.AMAZON_S3, task, store, checksumUtil);
        String resultChecksum = helper.getContentChecksum(checksum);
        assertEquals(resultChecksum, helper.getContentChecksum(checksum));

    }
    
    private void setupChecksumUtil(String checksum) {
        expect(checksumUtil.generateChecksum(is)).andReturn(checksum);
    }

    private void setupStorageProvider(int times) throws IOException {
        is.close();
        expectLastCall().times(times);
        expect(store.getContent(eq(spaceId), eq(contentId))).andReturn(is).times(times);

    }

    private void setupTask(int times) {
        expect(task.getSpaceId()).andReturn(spaceId).times(times);
        expect(task.getContentId()).andReturn(contentId).times(times);
    }

}
