/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.duracloud.audit.AuditLogItem;
import org.duracloud.audit.AuditLogUtil;
import org.duracloud.mill.test.AbstractTestBase;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *         Date: Sep 8, 2014
 */
public class SpaceLogTest extends AbstractTestBase{

    private String accountId = "account";

    private String storeId = "store-id";
    
    private String spaceId = "space-id";

    private String contentId = "content-id";

    private String checksum = "checksum";
    
    private String sourceSpaceId = "source-space-id";

    private String sourceContentId = "source-content-id";
    
    private Date timestamp = new Date();
    
    private File logsRootDir;
    
    private SpaceLog spaceLog;
    
    @Mock
    private AuditLogItem item;

    
    @Before
    public void setup(){
        logsRootDir = new File(System.getProperty("java.io.tmpdir")
                               + File.separator + System.currentTimeMillis());
        logsRootDir.mkdirs();
    }
    
    @After
    public void tearDown() {
        super.tearDown();
        FileUtils.deleteQuietly(logsRootDir);
    };
    
    
    @Test
    public void testWriteNoExistingLogs() throws Exception {

        assertEquals(0,FileUtils.listFiles(logsRootDir,
                                       FileFilterUtils.trueFileFilter(),
                                       FileFilterUtils.trueFileFilter()).size());
        

        setupAuditItem();
        replayAll();
        createTestSubject();
        
        this.spaceLog.write(item);
        this.spaceLog.close();
        
        List<File> files = new ArrayList<>(FileUtils.listFiles(logsRootDir,
                                                               FileFilterUtils
                                                                       .trueFileFilter(),
                                                               FileFilterUtils
                                                                       .trueFileFilter()));
        assertEquals(1, files.size());
 
        File file = files.get(0);
        assertTrue(file.length() > 0);
        
        verifyFileContents();
    }

    private void verifyFileContents() throws FileNotFoundException,
                                              IOException {

        List<File> files = new ArrayList<>(FileUtils.listFiles(logsRootDir,
                                                               FileFilterUtils
                                                                       .trueFileFilter(),
                                                               FileFilterUtils
                                                                       .trueFileFilter()));
        assertEquals(1, files.size());
 
        File file = files.get(0);
        assertTrue(file.length() > 0);

        verifyFileContents(file);
    }

    private void verifyFileContents(File file) throws FileNotFoundException,
                                              IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        
        String line = reader.readLine();

        assertEquals(AuditLogUtil.getHeader(),line);
        
        line = reader.readLine();

        assertTrue(line.contains(accountId));
        assertTrue(line.contains(storeId));
        assertTrue(line.contains(spaceId));
        assertTrue(line.contains(contentId));
        assertTrue(line.contains(checksum));
        assertTrue(line.contains("props"));
        assertTrue(line.contains("mime"));
        assertTrue(line.contains("size"));
        assertTrue(line.contains(sourceContentId));
        assertTrue(line.contains(sourceSpaceId));
        assertTrue(line.contains("action"));
        assertTrue(line.contains("username"));
        assertTrue(line.contains("{ \"acl\": \"write\" }"));
        assertTrue(line.contains(this.spaceLog.formatDate(timestamp)));
    }
    
    

    /**
     * 
     */
    private void setupAuditItem() {
        expect(item.getAccount()).andReturn(accountId);
        expect(item.getStoreId()).andReturn(storeId);
        expect(item.getSpaceId()).andReturn(spaceId);
        expect(item.getContentId()).andReturn(contentId);
        expect(item.getContentMd5()).andReturn(checksum);
        expect(item.getContentProperties()).andReturn("props");
        expect(item.getContentSize()).andReturn("size");
        expect(item.getMimetype()).andReturn("mime");
        expect(item.getSourceContentId()).andReturn(sourceContentId);
        expect(item.getSourceSpaceId()).andReturn(sourceSpaceId);
        expect(item.getAction()).andReturn("action");
        expect(item.getUsername()).andReturn("username");
        expect(item.getSpaceAcls()).andReturn("{\n\r \"acl\": \"write\" \t\n\r}");
        expect(item.getTimestamp()).andReturn(timestamp.getTime());
        
    }

    private void createTestSubject() {
        LogKey key = new LogKey(accountId, storeId, spaceId);
        this.spaceLog = new SpaceLog(key, logsRootDir);
    }
    
    @Test
    public void testWriteExistingUndersizeLog() throws Exception {
        setupAuditItem();
        replayAll();
        createTestSubject();
        File file = this.spaceLog.createNewLogFile();
        FileUtils.touch(file);
        this.spaceLog.write(item);
        this.spaceLog.close();
        verifyFileContents();
    }

    
    @Test
    public void testWriteTwoExistingLogs() throws Exception {
        setupAuditItem();
        replayAll();
        createTestSubject();
        File leastRecent = this.spaceLog.createNewLogFile();
        FileUtils.touch(leastRecent);
        Thread.sleep(1000);
        File mostRecent = this.spaceLog.createNewLogFile();
        FileUtils.touch(mostRecent);

        this.spaceLog.write(item);
        this.spaceLog.close();
        verifyFileContents(mostRecent);
        
        assertTrue(leastRecent.length() == 0);
        assertTrue(mostRecent.length() > 0);

    }

}
