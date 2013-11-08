/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.common.taskproducer;

import org.duracloud.mill.config.ConfigurationManager;

/**
 * @author Daniel Bernstein
 *	       Date: Nov 5, 2013
 */
public class TaskProducerConfigurationManager extends ConfigurationManager {
    public static final String DUPLICATION_POLICY_DIR_KEY = "duplicationPolicyDir";

    public String getDuplicationPolicyDir() {
        return System.getProperty(DUPLICATION_POLICY_DIR_KEY);
    }

}
