/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;


/**
 * @author Daniel Bernstein
 *	       Date: Oct 31, 2013
 */
public class DurastoreTaskProducerConfigurationManager extends TaskProducerConfigurationManager{

    public static final String DUPLICATION_POLICY_BUCKET_SUFFIX = "policyBucketSuffix";

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
}
