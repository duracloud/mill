/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.io.IOException;

import org.duracloud.mill.bitlog.BitLogStore;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Bernstein
 *         Date: 5/7/2014
 */
@RunWith(EasyMockRunner.class)
public class BitIntegrityReportTaskProcessorTest extends EasyMockSupport{

    private static final String account = "account-id";
    private static final String storeId = "store-id";
    private static final String spaceId = "space-id";

    private BitIntegrityCheckReportTask task;

    @Mock
    private BitLogStore bitLogStore;
    
    @TestSubject
    private BitIntegrityReportTaskProcessor taskProcessor;

    @Before
    public void setup() throws IOException {

        task = new BitIntegrityCheckReportTask();
        task.setAccount(account);
        task.setStoreId(storeId);
        task.setSpaceId(spaceId);
        taskProcessor = new BitIntegrityReportTaskProcessor(task, bitLogStore);

    }

    @After
    public void teardown() {
        verifyAll();
    }

    @Test
    public void testExecute() throws Exception {
        replayAll();
        taskProcessor.execute();
    }

}
