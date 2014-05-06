/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import java.io.File;

import org.duracloud.mill.ltp.ExclusionManager;
import org.duracloud.mill.ltp.LoopingTaskProducerConfigurationManager;

/**
 * @author Daniel Bernstein
 *	       Date: May 5, 2014
 */
public class LoopingBitTaskProducerConfigurationManager extends LoopingTaskProducerConfigurationManager {
    public static final String SPACE_EXCLUSION_LIST = "spaceExclusionsList";

    public ExclusionManager getExclusionManager(){

        String exclusionsFile = System.getProperty(SPACE_EXCLUSION_LIST);

        if(exclusionsFile == null){
            return new ExclusionManager();
        }else{
            return new ExclusionManager(new File(exclusionsFile));
        }
    }
}
