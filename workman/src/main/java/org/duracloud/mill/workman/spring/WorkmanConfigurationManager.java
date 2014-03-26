/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman.spring;

import org.duracloud.mill.config.ConfigurationManager;

import javax.ws.rs.HEAD;
import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 6, 2013
 */
public class WorkmanConfigurationManager extends ConfigurationManager {

    public static final String DEAD_LETTER_QUEUE_KEY = "deadLetterQueue";
    public static final String TASK_QUEUES_KEY = "taskQueues";

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
}
