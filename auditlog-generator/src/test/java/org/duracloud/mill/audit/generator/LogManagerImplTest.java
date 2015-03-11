/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.duracloud.audit.AuditLogItem;
import org.duracloud.common.util.ContentIdUtil;
import org.duracloud.mill.db.model.JpaAuditLogItem;
import org.duracloud.mill.db.repo.JpaAuditLogItemRepo;
import org.duracloud.mill.test.AbstractTestBase;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

/**
 * @author Daniel Bernstein Date: Sep 8, 2014
 */
public class LogManagerImplTest extends AbstractTestBase {
    private LogManagerImpl manager;
    private File logsRootDir;
    private String accountId = "account";

    private String storeId = "store-id";

    private String spaceId = "space-id";

    private String contentId = "content-id";

    private String checksum = "checksum";

    @Mock
    private JpaAuditLogItemRepo repo;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private SpaceLog spaceLog;
    private String logSpace = "log-space";

    @Before
    public void setup() {
        logsRootDir = new File(System.getProperty("java.io.tmpdir")
                + File.separator + System.currentTimeMillis());
        logsRootDir.mkdirs();
    }

    @After
    public void tearDown() {
        super.tearDown();
        FileUtils.deleteQuietly(logsRootDir);
    };

    /**
     * Test method for
     * {@link org.duracloud.mill.audit.generator.LogManagerImpl#write(org.duracloud.mill.db.model.JpaAuditLogItem)}
     * .
     * 
     * @throws IOException
     */
    @Test
    public void testWrite() throws IOException {
        manager = new LogManagerImpl(storageProvider,
                                     logsRootDir.getAbsolutePath(),
                                     repo,
                                     logSpace) {
            /*
             * (non-Javadoc)
             * 
             * @see
             * org.duracloud.mill.audit.generator.LogManagerImpl#createSpaceLog
             * (org.duracloud.mill.audit.generator.LogKey)
             */
            @Override
            protected SpaceLog createSpaceLog(LogKey key) {
                return spaceLog;
            }
        };

        long itemId1 = 1;
        long itemId2 = 2l;
        
        JpaAuditLogItem item1 = createItem(itemId1,new Date(System.currentTimeMillis()-1));
        JpaAuditLogItem duplicateItem = createItem(itemId2,new Date(System.currentTimeMillis()));

        spaceLog.write(eq(item1));
        expectLastCall();

        JpaAuditLogItem freshItem = createMock(JpaAuditLogItem.class);
        freshItem.setWritten(true);
        expectLastCall().times(2);

        expect(this.repo.saveAndFlush(freshItem)).andReturn(freshItem).times(2);
        expect(this.repo.getOne(anyLong())).andReturn(freshItem).times(2);

        replayAll();

        this.manager.write(item1);
        this.manager.write(duplicateItem);

    }
    
    /**
     * @param itemId1
     * @param date
     * @return
     */
    private JpaAuditLogItem createItem(long itemId, Date date) {
        JpaAuditLogItem item = createMock(JpaAuditLogItem.class);
        expect(item.getContentId()).andReturn("content-id").atLeastOnce();
        expect(item.getContentMd5()).andReturn("content-md5").atLeastOnce();
        expect(item.getContentProperties()).andReturn("content-props").atLeastOnce();
        expect(item.getContentSize()).andReturn("content-size").atLeastOnce();
        expect(item.getMimetype()).andReturn("mimetype").atLeastOnce();
        expect(item.getSourceContentId()).andReturn("source").atLeastOnce();
        expect(item.getSourceSpaceId()).andReturn("source").atLeastOnce();
        expect(item.getSpaceAcls()).andReturn("acls").atLeastOnce();
        expect(item.getUsername()).andReturn("username").atLeastOnce();

        expect(item.getAction()).andReturn("action").atLeastOnce();
        expect(item.getAccount()).andReturn(accountId).atLeastOnce();
        expect(item.getStoreId()).andReturn(storeId).atLeastOnce();
        expect(item.getSpaceId()).andReturn(spaceId).atLeastOnce();
        //expect(item.getTimestamp()).andReturn(date.getTime());
        expect(item.getId()).andReturn(itemId);
        return item;
    }

    @Test
    public void testPurge() {
        createManager();
        expect(this.repo.deleteByWrittenTrueAndTimestampLessThan(anyLong())).andReturn(0l);
        replayAll();
        this.manager.purgeExpired();
    }

    @Test
    public void testFlushLogs() throws IOException {

        int fileCount = 3;

        List<File> files = new ArrayList<>();

        for (int i = 0; i < fileCount; i++) {
            File file = new File(logsRootDir, "test-log-" + i);
            FileUtils.touch(file);
            if (i < fileCount - 1) {
                createFileOfLength(file, SpaceLog.MAX_FILE_SIZE);
            } else {
                createFileOfLength(file, SpaceLog.MAX_FILE_SIZE - 1);
            }
            files.add(file);

            String contentId =
                ContentIdUtil.getContentId(file, logsRootDir, null);
            expect(this.storageProvider.addContent(eq(logSpace),
                                                   eq(contentId),
                                                   isA(String.class),
                                                   isNull(Map.class),
                                                   eq(file.length()),
                                                   isA(String.class),
                                                   isA(InputStream.class)))
                    .andReturn("checksum");

        }

        replayAll();
        createManager();

        this.manager.flushLogs();

        assertTrue(!files.get(0).exists());
        assertTrue(!files.get(1).exists());
        assertTrue(files.get(2).exists());

    }

    private void createManager() {
        manager = new LogManagerImpl(storageProvider,
                                     logsRootDir.getAbsolutePath(),
                                     repo, 
                                     logSpace);
    }

    private void createFileOfLength(File file, long length) {
        try {
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            f.setLength(length);
            f.close();
        } catch (Exception e) {
            System.err.println(e);
        }

    }

}
