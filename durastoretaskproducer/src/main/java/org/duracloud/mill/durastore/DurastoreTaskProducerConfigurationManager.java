/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import org.duracloud.mill.config.ConfigurationManager;

/**
 * @author Daniel Bernstein
 *	       Date: Oct 31, 2013
 */
public class DurastoreTaskProducerConfigurationManager extends ConfigurationManager{

    public static final String DUPLICATION_POLICY_FILE_KEY = "duplicationPolicyFile";

    public String getDuplicationPolicyFile(){
        return System.getProperty(DUPLICATION_POLICY_FILE_KEY);
    }
}
