/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.task;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.duracloud.common.queue.task.Task;
import org.junit.Test;

/**
 * @author Bill Branan
 * Date: 10/24/13
 */
public class DuplicationTaskTest {

    private String account = "account-id";
    private String sourceStoreId = "source-store-id";
    private String destStoreId = "dest-store-id";
    private String spaceId = "space-id";
    private String contentId = "content-id";

    @Test
    public void testReadTask() {
        Task task = new Task();
        task.setType(Task.Type.DUP);
        task.addProperty(DuplicationTask.ACCOUNT_PROP, account);
        task.addProperty(DuplicationTask.STORE_ID_PROP, sourceStoreId);
        task.addProperty(DuplicationTask.DEST_STORE_ID_PROP, destStoreId);
        task.addProperty(DuplicationTask.SPACE_ID_PROP, spaceId);
        task.addProperty(DuplicationTask.CONTENT_ID_PROP, contentId);

        DuplicationTask dupTask = new DuplicationTask();
        dupTask.readTask(task);

        assertEquals(account, dupTask.getAccount());
        assertEquals(sourceStoreId, dupTask.getSourceStoreId());
        assertEquals(destStoreId, dupTask.getDestStoreId());
        assertEquals(spaceId, dupTask.getSpaceId());
        assertEquals(contentId, dupTask.getContentId());
    }

    @Test
    public void testWriteTask() {
        DuplicationTask dupTask = new DuplicationTask();
        dupTask.setAccount(account);
        dupTask.setSourceStoreId(sourceStoreId);
        dupTask.setDestStoreId(destStoreId);
        dupTask.setSpaceId(spaceId);
        dupTask.setContentId(contentId);

        Task task = dupTask.writeTask();
        assertEquals(Task.Type.DUP, task.getType());

        Map<String, String> taskProps = task.getProperties();
        assertEquals(account,
                     taskProps.get(DuplicationTask.ACCOUNT_PROP));
        assertEquals(sourceStoreId,
                     taskProps.get(DuplicationTask.STORE_ID_PROP));
        assertEquals(destStoreId,
                     taskProps.get(DuplicationTask.DEST_STORE_ID_PROP));
        assertEquals(spaceId,
                     taskProps.get(DuplicationTask.SPACE_ID_PROP));
        assertEquals(contentId,
                     taskProps.get(DuplicationTask.CONTENT_ID_PROP));
    }

}
