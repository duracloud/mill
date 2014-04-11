/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman.spring;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 6, 2013
 */
public class WorkmanConfigurationManager 
                extends TaskProducerConfigurationManager {

    public static final String DEAD_LETTER_QUEUE_KEY = "deadLetterQueue";
    public static final String TASK_QUEUES_KEY = "taskQueues";
    public static final String CONTENT_INDEX_PORT = "contentIndexPort";
    public static final String CONTENT_INDEX_HOST = "contentIndexHost";
    public static final String DUPLICATION_POLICY_BUCKET_SUFFIX = "policyBucketSuffix";
    public static final String NOTIFICATION_RECIPIENTS = "notificationRecipients";
    public static final String POLICY_MANAGER_REFRESH_FREQUENCY_MS = "policyManagerRefreshFrequencyMs";
    public static final String HIGH_PRIORITY_DUPLICATION_QUEUE_KEY = "highPriorityDuplicationQueue";
    
    public String getDeadLetterQueueName() {
        return System.getProperty(DEAD_LETTER_QUEUE_KEY);
    }

    
    /* (non-Javadoc)
     * @see org.duracloud.mill.config.ConfigurationManager#addRequiredProperties()
     */
    @Override
    protected void addRequiredProperties() {
        super.addRequiredProperties();
        addRequiredProperty(TASK_QUEUES_KEY);
        addRequiredProperty(DEAD_LETTER_QUEUE_KEY);
        addRequiredProperty(AUDIT_QUEUE_KEY);
    }


    /**
     * @return
     */
    public List<String> getTaskQueueNames() {
        return Arrays.asList(System.getProperty(TASK_QUEUES_KEY).split(","));
    }


    /**
     * @return
     */
    public String getContentIndexHost() {
        return System.getProperty(CONTENT_INDEX_HOST, "localhost");
    }


    /**
     * @return
     */
    public int getContentIndexPort() {
         return Integer.valueOf(System.getProperty(CONTENT_INDEX_PORT, "9200"));
    }
    
    /**
     * @return
     */
    public String getPolicyBucketSuffix() {
        return System.getProperty(DUPLICATION_POLICY_BUCKET_SUFFIX);
    }

    public Long getPolicyManagerRefreshFrequencyMs(){
        String refresh = System.getProperty(POLICY_MANAGER_REFRESH_FREQUENCY_MS);
        if(refresh != null){
            return new Long(refresh);
        }else{
            return 5*60*1000l;
        }
    }
    /**
     * @return
     */
    public String[] getNotificationRecipients() {
        String recipients =  System.getProperty(NOTIFICATION_RECIPIENTS);
        if(StringUtils.isBlank(recipients)){
            return null;
        }else{
            return recipients.split(",");
        }
    }

    /**
     * @return
     */
    public String getHighPriorityDuplicationQueueName() {
        return System.getProperty(HIGH_PRIORITY_DUPLICATION_QUEUE_KEY);
    }

}
