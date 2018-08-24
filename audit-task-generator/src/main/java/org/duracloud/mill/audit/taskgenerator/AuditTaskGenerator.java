/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.taskgenerator;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.client.ContentStore;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.common.queue.task.Task;
import org.duracloud.domain.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 */
public class AuditTaskGenerator {
    private static final Logger log = LoggerFactory.getLogger( AuditTaskGenerator.class );
    private ContentStore contentStore;
    private String spaceId;
    private String account;
    private boolean dryRun;
    private int totalAuditTasksAddedToQueue = 0;
    private String auditQueueName;
    private String username;

    public AuditTaskGenerator(ContentStore contentStore, String spaceId, boolean dryRun, String account,
                              String auditQueueName, String username) {
        this.contentStore = contentStore;
        this.dryRun = dryRun;
        this.spaceId = spaceId;
        this.account = account;
        this.auditQueueName = auditQueueName;
        this.username = username;
    }

    public void execute() throws Exception {
        long startTime = System.currentTimeMillis();

        final TaskQueue queue = new SQSTaskQueue( auditQueueName );
        log.info( "Created task queue for queue " + auditQueueName );

        String storeId = contentStore.getStoreId();
        Iterator<String> contentIds = this.contentStore.getSpaceContents( this.spaceId );

        while (contentIds.hasNext()) {
            final String contentId = contentIds.next();
            Map<String, String> props = contentStore.getContentProperties( spaceId, contentId );
            Content content = contentStore.getContent( spaceId, contentId );
            AuditTask task = new AuditTask();
            task.setAction( AuditTask.ActionType.ADD_CONTENT.name() );
            task.setUserId( this.username );
            task.setDateTime( String.valueOf( System.currentTimeMillis() ) );
            task.setAccount( account );
            task.setStoreId( contentStore.getStoreId() );
            task.setStoreType( contentStore.getStorageProviderType() );
            task.setSpaceId( spaceId );
            task.setSpaceACLs( null );
            task.setContentId( contentId );
            task.setContentChecksum( props.get( ContentStore.CONTENT_CHECKSUM ) );
            task.setContentMimetype( props.get( ContentStore.CONTENT_MIMETYPE ) );
            task.setContentSize( props.get( ContentStore.CONTENT_SIZE ) );
            task.setContentProperties( props );
            Task writeTask = task.writeTask();
            if (!dryRun) {
                queue.put( writeTask );
                log.info( "Wrote {} to {}", writeTask.toString(), auditQueueName );
            } else {
                log.info( "Dry Run -> Ignored: write {} to {}", writeTask.toString(), auditQueueName );
            }

            totalAuditTasksAddedToQueue++;
        }

        String duration = DurationFormatUtils.formatDurationHMS( System.currentTimeMillis() - startTime );

        log.info( "duration={} total_audit_tasks_added_to_queue={}",
                  duration,
                  totalAuditTasksAddedToQueue );

    }
}
