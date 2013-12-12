/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman.spring;

import org.duracloud.mill.config.ConfigurationManager;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 6, 2013
 */
public class WorkmanConfigurationManager extends ConfigurationManager {

    public static final String DEAD_LETTER_QUEUE_KEY = "deadLetterQueue";

    public String getDeadLetterQueueName() {
        return System.getProperty(DEAD_LETTER_QUEUE_KEY);
    }

    
    /* (non-Javadoc)
     * @see org.duracloud.mill.config.ConfigurationManager#addRequiredProperties()
     */
    @Override
    protected void addRequiredProperties() {
        super.addRequiredProperties();
        addRequiredProperty(LOW_PRIORITY_DUPLICATION_QUEUE_KEY);
        addRequiredProperty(HIGH_PRIORITY_DUPLICATION_QUEUE_KEY);
    }
}
