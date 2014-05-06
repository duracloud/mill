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
public class LoopingTaskProducerConfigurationManager extends
        TaskProducerConfigurationManager {
    public static final String OUTPUT_QUEUE_KEY = "outputQueue";

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.config.ConfigurationManager#addRequiredProperties()
     */
    @Override
    protected void addRequiredProperties() {
        super.addRequiredProperties();
        addRequiredProperty(OUTPUT_QUEUE_KEY);
    }

    /**
     * @return
     */
    public String getOutputQueue() {
        return System.getProperty(OUTPUT_QUEUE_KEY);
    }
}
