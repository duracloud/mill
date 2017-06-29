/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.common.taskproducer;

import org.apache.commons.lang3.StringUtils;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.config.ConfigurationManager;

/**
 * @author Daniel Bernstein
 *	       Date: Nov 5, 2013
 */
public class TaskProducerConfigurationManager extends ConfigurationManager {

    public String getDuplicationPolicyDir() {
        return System.getProperty(ConfigConstants.LOCAL_DUPLICATION_DIR);
    }

    /**
     * @return
     */
    public String getDuplicationPolicyBucketSuffix() {
        return System.getProperty(ConfigConstants.DUPLICATION_POLICY_BUCKET_SUFFIX);
    }
    

    /**
     * @return
     */
    public String getBitReportQueueName() {
        return System.getProperty(ConfigConstants.QUEUE_NAME_BIT_REPORT);
    }
}
