/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import org.duracloud.mill.common.taskproducer.TaskProducerConfigurationManager;

/**
 * @author Daniel Bernstein
 *	       Date: Dec 6, 2013
 */
public class LoopingTaskProducerConfigurationManager extends TaskProducerConfigurationManager {
    /* (non-Javadoc)
     * @see org.duracloud.mill.config.ConfigurationManager#addRequiredProperties()
     */
    @Override
    protected void addRequiredProperties() {
        super.addRequiredProperties();
        addRequiredProperty(LOW_PRIORITY_DUPLICATION_QUEUE_KEY);
    }
}
