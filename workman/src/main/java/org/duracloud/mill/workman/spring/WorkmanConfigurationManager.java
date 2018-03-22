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

import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;
import org.duracloud.mill.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: Dec 6, 2013
 */
public class WorkmanConfigurationManager extends TaskProducerConfigurationManager {

    private static Logger log = LoggerFactory.getLogger(WorkmanConfigurationManager.class);

    public String getDeadLetterQueueName() {
        return System.getProperty(ConfigConstants.QUEUE_NAME_DEAD_LETTER);
    }

    /**
     * @return
     */
    public List<String> getTaskQueueNames() {
        List<String> queueNames = Arrays.asList(System.getProperty(ConfigConstants.QUEUE_TASK_ORDERED).split(","));
        for (int i = 0; i < queueNames.size(); i++) {
            String key = queueNames.get(i);
            String value = System.getProperty(key.trim());
            log.info("Resolved concrete queue name from key : {}={}", key, value);
            queueNames.set(i, value);
        }

        return queueNames;
    }

    /**
     * @return
     */
    public String getPolicyBucketSuffix() {
        return System.getProperty(ConfigConstants.DUPLICATION_POLICY_BUCKET_SUFFIX);
    }

    public Long getPolicyManagerRefreshFrequencyMs() {
        String refresh = System.getProperty(ConfigConstants.DUPLICATION_POLICY_REFRESH_FREQUENCY);
        if (refresh != null) {
            return new Long(refresh);
        } else {
            return 5 * 60 * 1000l;
        }
    }

    /**
     * @return
     */
    public String getHighPriorityDuplicationQueueName() {
        return System.getProperty(ConfigConstants.QUEUE_NAME_DUP_HIGH_PRIORITY);
    }

    public String getBitErrorQueueName() {
        return System.getProperty(ConfigConstants.QUEUE_NAME_BIT_ERROR);
    }

}
