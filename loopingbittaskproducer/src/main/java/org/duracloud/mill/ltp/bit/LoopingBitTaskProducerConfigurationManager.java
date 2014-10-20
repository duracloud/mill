/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import java.io.File;

import org.duracloud.mill.ltp.PathFilterManager;
import org.duracloud.mill.ltp.LoopingTaskProducerConfigurationManager;

/**
 * @author Daniel Bernstein
 *	       Date: May 5, 2014
 */
public class LoopingBitTaskProducerConfigurationManager extends LoopingTaskProducerConfigurationManager {
    public static final String EXCLUSION_LIST = "exclusionList";
    public static final String INCLUSION_LIST = "inclusionList";

    public PathFilterManager getPathFilterManager(){
        PathFilterManager pathFilterManager = new PathFilterManager();
        
        String exclusions = System.getProperty(EXCLUSION_LIST);
        if(exclusions != null){
            pathFilterManager.setExclusions(new File(exclusions));
        }

        String inclusions = System.getProperty(INCLUSION_LIST);
        if(inclusions != null){
            pathFilterManager.setInclusions(new File(inclusions));
        }
        
        return pathFilterManager;

    }

}
