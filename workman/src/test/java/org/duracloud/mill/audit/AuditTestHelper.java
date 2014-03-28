/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import java.util.HashMap;
import java.util.Map;

import org.duracloud.audit.task.AuditTask;
import org.duracloud.common.util.DateUtil;

/**
 * @author Daniel Bernstein
 *	       Date: Mar 21, 2014
 */
public class AuditTestHelper {

    /**
     * @return
     */
    public static AuditTask createTestAuditTask() {
        AuditTask task = new AuditTask();
        task.setAccount("account");
        task.setStoreId("1");
        task.setStoreType("type");
        task.setSpaceId("spaceId");
        task.setContentId("contentId");
        task.setAction(AuditTask.ActionType.DELETE_CONTENT.name());
        task.setContentSize("1000");
        task.setDateTime(System.currentTimeMillis()+"");
        task.setContentChecksum("12341243214234");
        task.setUserId("user");
        task.setContentMimetype("application/text");
        Map<String,String> map = new HashMap<>();
        map.put("key", "value");
        task.setContentProperties(map);
        task.setSpaceACLs("acls");
        return task;
    }
}
