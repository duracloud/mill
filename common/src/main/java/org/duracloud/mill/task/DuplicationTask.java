/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.task;

import java.util.Map;

import org.duracloud.common.queue.task.Task;
import org.duracloud.common.queue.task.TypedTask;

/**
 * Provides the information necessary to complete a content
 * duplication activity.
 *
 * @author Bill Branan
 * Date: 10/18/13
 */
public class DuplicationTask extends TypedTask {

    public static final String DEST_STORE_ID_PROP = "destStoreId";

    private String destStoreId;

    /**
     * Gets the source store ID. This is equivalent to calling getStoreId()
     *
     * @return source store ID
     */
    public String getSourceStoreId() {
        return getStoreId();
    }

    /**
     * Sets the source store ID. This is equivalent to calling setStoreId(storeId)
     *
     * @param sourceStoreId
     */
    public void setSourceStoreId(String sourceStoreId) {
        setStoreId(sourceStoreId);
    }

    public String getDestStoreId() {
        return destStoreId;
    }

    public void setDestStoreId(String destStoreId) {
        this.destStoreId = destStoreId;
    }

    @Override
    public void readTask(Task task) {
        super.readTask(task);

        Map<String, String> props = task.getProperties();
        setDestStoreId(props.get(DEST_STORE_ID_PROP));
    }

    @Override
    public Task writeTask() {
        Task task = super.writeTask();
        task.setType(Task.Type.DUP);
        task.addProperty(DEST_STORE_ID_PROP, getDestStoreId());
        return task;
    }

}
