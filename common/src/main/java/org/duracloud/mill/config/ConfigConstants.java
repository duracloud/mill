/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.config;

/**
 * @author Daniel Bernstein
 *         Date: Dec 5, 2014
 */
public class ConfigConstants {

    /*
     * QUEUES
     */
    public static final String QUEUE_NAME_BIT_ERROR = "queue.name.bit-error";
    public static final String QUEUE_NAME_BIT_INTEGRITY = "queue.name.bit-integrity";
    public static final String QUEUE_NAME_DUP_LOW_PRIORITY = "queue.name.dup-low-priority";
    public static final String QUEUE_NAME_DUP_HIGH_PRIORITY = "queue.name.dup-high-priority";
    public static final String QUEUE_NAME_AUDIT = "queue.name.audit";
    public static final String QUEUE_NAME_DEAD_LETTER = "queue.name.dead-letter";

    /*
     * SHARED
     */
    public static final String WORK_DIRECTORY_PATH = "workdir";
    public static final String NOTIFICATION_RECIPIENTS = "notification.recipients";

    /*
     * AUDIT LOG GENERATOR
     */
    public static final String AUDIT_LOGS_SPACE_ID = "audit-log-generator.audit-log-space-id";
    public static final String AUDIT_LOG_GENERATOR_AUDIT_LOG_SPACE_ID = "audit-log-generator.audit-log-space-id";

    /*
     * AWS CREDENTIALS
     */
    public static final String AWS_ACCESS_KEY_ID = "aws.accessKeyId";
    public static final String AWS_SECRET_KEY = "aws.secretKey";

    /*
     * WORKMAN
     */

    public static final String MAX_WORKERS = "max-workers";
    
    /*
     * LOOPING BIT TASK PRODUCER
     */
    public static final String LOOPING_BIT_MAX_TASK_QUEUE_SIZE = "looping.bit.max-task-queue-size";
    public static final String LOOPING_BIT_FREQUENCY = "looping.bit.frequency";
    public static final String LOOPING_BIT_STATE_FILE_PATH = "looping.bit.state-file-path";
    public static final String EXCLUSION_LIST_KEY = "looping.bit.exclusion-list-file";
    public static final String INCLUSION_LIST_KEY = "looping.bit.inclusion-list-file";

    
    /*
     * DUPLICATION
     */
    public static final String DUPLICATION_POLICY_REFRESH_FREQUENCY = "duplication-policy.refresh-frequency";
    public static final String DUPLICATION_POLICY_BUCKET_SUFFIX = "duplication-policy.bucket-suffix";
    public static final String LOCAL_DUPLICATION_DIR = "local-duplication-dir";

    /*
     * LOOPING DUPLICATION
     */
    public static final String LOOPING_DUP_FREQUENCY = "looping.dup.frequency";
    public static final String LOOPING_DUP_MAX_TASK_QUEUE_SIZE = "looping.dup.max-task-queue-size";
    public static final String LOOPING_DUP_STATE_FILE_PATH = "looping.dup.state-file-path";

    /*
     * MC DATABASE
     */
    public static final String MC_DB_NAME = "db.name";
    public static final String MC_DB_HOST = "db.host";
    public static final String MC_DB_PORT = "db.port";
    public static final String MC_DB_PASS = "db.pass";
    public static final String MC_DB_USER = "db.user";
    
    /*
     * MILL DATABASE
     */
    public static final String MILL_DB_NAME = "mill.db.name";
    public static final String MILL_DB_HOST = "mill.db.host";
    public static final String MILL_DB_PORT = "mill.db.port";
    public static final String MILL_DB_USER = "mill.db.user";
    public static final String MILL_DB_PASS = "mill.db.pass";
    
    /*
     * MANIFEST
     */
    public static final String MANIFEST_EXPIRATION_TIME = "manifest.expiration-time";
    /**
     * 
     */
    public static final String QUEUE_TASK_ORDERED = "queue.task.ordered";

}
