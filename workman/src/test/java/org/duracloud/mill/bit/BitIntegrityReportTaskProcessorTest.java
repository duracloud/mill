/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.db.model.BitIntegrityReport;
import org.duracloud.mill.notification.NotificationManager;
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

/**
 * @author Daniel Bernstein
 * Date: 5/7/2014
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
    private TaskProducerConfigurationManager config;

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
        expect(task.getAccount()).andReturn(account).times(2);
        expect(task.getStoreId()).andReturn(storeId).times(2);
        expect(task.getSpaceId()).andReturn(spaceId).times(2);
        expect(task.getAttempts()).andReturn(2);

    }

    @Test
    public void testExecute() throws Exception {
        setupTask();

        File dir = new File(System.getProperty("java.io.tmpdir"),
                            System.currentTimeMillis() + "");
        assertTrue(dir.mkdirs());

        expect(config.getWorkDirectoryPath()).andReturn(dir.getAbsolutePath());
        BitLogItem item = createMock(BitLogItem.class);

        expect(item.getAccount()).andReturn(account).times(2);
        expect(item.getStoreId()).andReturn(storeId).times(2);
        expect(item.getSpaceId()).andReturn(spaceId).times(2);
        expect(item.getStoreType()).andReturn(StorageProviderType.AMAZON_S3).times(2);
        expect(item.getContentId()).andReturn("content").times(2);
        expect(item.getResult()).andReturn(BitIntegrityResult.ERROR).times(3);
        expect(item.getContentChecksum()).andReturn("checksum").times(2);
        expect(item.getManifestChecksum()).andReturn("checksum").times(2);
        expect(item.getStorageProviderChecksum()).andReturn("checksum").times(2);
        expect(item.getDetails()).andReturn("details").times(2);

        expect(item.getModified()).andReturn(new Date()).times(2);

        Iterator<BitLogItem> it = createMock(Iterator.class);
        expect(it.hasNext()).andReturn(true);
        expect(it.next()).andReturn(item);
        expect(it.hasNext()).andReturn(false).times(2);

        expect(this.bitLogStore.getBitLogItems(eq(account),
                                               eq(storeId),
                                               eq(spaceId))).andReturn(it);

        final Capture<InputStream> capture = new Capture<>();

        String reportSpace = "x-duracloud-admin";
        expect(this.store.getSpaces()).andReturn(Arrays.asList("aspace").iterator());
        this.store.createSpace(reportSpace);
        expectLastCall().once();

        expect(this.store.addContent(eq(reportSpace),
                                     isA(String.class),
                                     isA(String.class),
                                     (Map<String, String>) isNull(),
                                     anyLong(),
                                     isA(String.class),
                                     capture(capture)))
            .andAnswer(new IAnswer<String>() {
                @Override
                public String answer() throws Throwable {
                    InputStream is = capture.getValue();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    assertNotNull(reader.readLine());
                    assertNotNull(reader.readLine());
                    assertNull(reader.readLine());
                    return null;
                }
            });

        bitLogStore.delete(account, storeId, spaceId);
        expectLastCall().once();

        BitIntegrityReport report = createMock(BitIntegrityReport.class);
        expect(report.getAccount()).andReturn(account);
        expect(report.getStoreId()).andReturn(storeId);
        expect(report.getSpaceId()).andReturn(spaceId);
        expect(report.getId()).andReturn(1l);

        expect(bitLogStore.addReport(eq(account),
                                     eq(storeId),
                                     eq(spaceId),
                                     isA(String.class),
                                     isA(String.class),
                                     eq(BitIntegrityReportResult.FAILURE),
                                     isA(Date.class))).andReturn(report);

        notificiationManager.sendEmail(isA(String.class), isA(String.class));
        expectLastCall().once();

        replayAll();
        taskProcessor.execute();
    }

}
