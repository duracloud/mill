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
import org.duracloud.mill.config.ConfigConstants;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 6, 2013
 */
public class WorkmanConfigurationManager 
                extends TaskProducerConfigurationManager {

    
    public String getDeadLetterQueueName() {
        return System.getProperty(ConfigConstants.QUEUE_NAME_DEAD_LETTER);
    }

    /**
     * @return
     */
    public List<String> getTaskQueueNames() {
        return Arrays.asList(System.getProperty(ConfigConstants.QUEUE_TASK_ORDERED).split(","));
    }

    /**
     * @return
     */
    public String getPolicyBucketSuffix() {
        return System.getProperty(ConfigConstants.DUPLICATION_POLICY_BUCKET_SUFFIX);
    }

    public Long getPolicyManagerRefreshFrequencyMs(){
        String refresh = System.getProperty(ConfigConstants.DUPLICATION_POLICY_REFRESH_FREQUENCY);
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
        String recipients =  System.getProperty(ConfigConstants.NOTIFICATION_RECIPIENTS);
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
        return System.getProperty(ConfigConstants.QUEUE_NAME_DUP_HIGH_PRIORITY);
    }
    
    public String getBitErrorQueueName(){
        return System.getProperty(ConfigConstants.QUEUE_NAME_BIT_ERROR);
    }

}
