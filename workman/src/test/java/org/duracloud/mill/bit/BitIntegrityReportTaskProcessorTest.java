/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.db.model.BitIntegrityReport;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.workman.spring.WorkmanConfigurationManager;
import org.duracloud.reportdata.bitintegrity.BitIntegrityReportResult;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

/**
 * @author Daniel Bernstein Date: 5/7/2014
 */
@RunWith(EasyMockRunner.class)
public class BitIntegrityReportTaskProcessorTest extends EasyMockSupport {

    private static final String account = "account-id";
    private static final String storeId = "store-id";
    private static final String spaceId = "space-id";

    @Mock
    private BitIntegrityCheckReportTask task;

    @Mock
    private BitLogStore bitLogStore;

    @TestSubject
    private BitIntegrityReportTaskProcessor taskProcessor;

    @Mock
    private StorageProvider store;

    @Mock
    private WorkmanConfigurationManager config;
    
    @Mock
    private NotificationManager notificiationManager;

    @Before
    public void setup() throws IOException {
        taskProcessor = new BitIntegrityReportTaskProcessor(task,
                                                            bitLogStore,
                                                            store,
                                                            config, 
                                                            notificiationManager);

    }

    @After
    public void teardown() {
        verifyAll();
    }

    private void setupTask() {
        expect(task.getAccount()).andReturn(account);
        expect(task.getStoreId()).andReturn(storeId);
        expect(task.getSpaceId()).andReturn(spaceId);
    }

    @Test
    public void testExecute() throws Exception {
        setupTask();

        File dir = new File(System.getProperty("java.io.tmpdir"),
                            System.currentTimeMillis() + "");
        assertTrue(dir.mkdirs());

        expect(config.getWorkDirectoryPath()).andReturn(dir.getAbsolutePath());
        BitLogItem item = createMock(BitLogItem.class);

        expect(item.getAccount()).andReturn(account);
        expect(item.getStoreId()).andReturn(storeId);
        expect(item.getSpaceId()).andReturn(spaceId);
        expect(item.getStoreType()).andReturn(StorageProviderType.AMAZON_S3);
        expect(item.getContentId()).andReturn("content");
        expect(item.getResult()).andReturn(BitIntegrityResult.ERROR).times(2);
        expect(item.getContentChecksum()).andReturn("checksum");
        expect(item.getManifestChecksum()).andReturn("checksum");
        expect(item.getStorageProviderChecksum()).andReturn("checksum");
        expect(item.getDetails()).andReturn("details");

        expect(item.getModified()).andReturn(new Date());

        Iterator<BitLogItem> it = createMock(Iterator.class);
        expect(it.hasNext()).andReturn(true);
        expect(it.next()).andReturn(item);
        expect(it.hasNext()).andReturn(false).times(2);

        expect(this.bitLogStore.getBitLogItems(eq(account),
                                               eq(storeId),
                                               eq(spaceId))).andReturn(it);
        final Capture<InputStream> capture = new Capture<>();

        this.store.addContent(eq("x-duracloud-admin"),
                              isA(String.class),
                              isA(String.class),
                              (Map<String, String>) isNull(),
                              anyLong(),
                              isA(String.class),
                              capture(capture));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            /*
             * (non-Javadoc)
             * 
             * @see org.easymock.IAnswer#answer()
             */
            @Override
            public Object answer() throws Throwable {
                InputStream is = capture.getValue();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                assertNotNull(reader.readLine());
                assertNotNull(reader.readLine());
                assertNull(reader.readLine());
                return null;
            }
        });
        
        bitLogStore.delete(account, storeId, spaceId);
        expectLastCall();
        

        BitIntegrityReport report = createMock(BitIntegrityReport.class);
        expect(bitLogStore.addReport(eq(account),
                              eq(storeId),
                              eq(spaceId),
                              isA(String.class),
                              isA(String.class),
                              eq(BitIntegrityReportResult.FAILURE),
                              isA(Date.class))).andReturn(report);
        
        notificiationManager.bitIntegrityErrors(isA(BitIntegrityReport.class), isA(List.class));
        expectLastCall();
        
        replayAll();
        taskProcessor.execute();
    }

}
