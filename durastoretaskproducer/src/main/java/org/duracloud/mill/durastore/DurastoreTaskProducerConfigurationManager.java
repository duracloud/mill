/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;


/**
 * @author Daniel Bernstein
 *	       Date: Oct 31, 2013
 */
public class DurastoreTaskProducerConfigurationManager extends TaskProducerConfigurationManager{

    public static final String DUPLICATION_POLICY_BUCKET_SUFFIX = "policyBucketSuffix";
    public static final String NOTIFICATION_RECIPIENTS = "notificationRecipients";

    /**
     * @return
     */
    public String getJMSConnectionUrlTemplate() {
        return System.getProperty("jms.connection.url.template");
    }
    
    /* (non-Javadoc)
     * @see org.duracloud.mill.config.ConfigurationManager#addRequiredProperties()
     */
    @Override
    protected void addRequiredProperties() {
        super.addRequiredProperties();
        addRequiredProperty(HIGH_PRIORITY_DUPLICATION_QUEUE_KEY);
    }

    /**
     * @return
     */
    public String getPolicyBucketSuffix() {
        return System.getProperty(DUPLICATION_POLICY_BUCKET_SUFFIX);
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
}
