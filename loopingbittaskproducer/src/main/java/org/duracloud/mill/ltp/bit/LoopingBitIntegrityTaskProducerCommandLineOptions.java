/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import org.apache.commons.cli.Option;
import org.duracloud.mill.ltp.LoopingTaskProducerCommandLineOptions;

/**
 * @author Daniel Bernstein
 *	       Date: May 5, 2014
 */
public class LoopingBitIntegrityTaskProducerCommandLineOptions extends
        LoopingTaskProducerCommandLineOptions {
    private static final long serialVersionUID = 1L;
 
    public static final String SPACE_EXCLUSION_LIST_OPTION = "x";

    /**
     * 
     */
    public LoopingBitIntegrityTaskProducerCommandLineOptions() {
        super();
        Option configFile = new Option(SPACE_EXCLUSION_LIST_OPTION, "exclusion-list", true,
                "A file containing exclusions as regular expressions, one expression per line." +
                "Expressions will be matched against the following path: /{account}/{storeId}/{spaceId}");
        configFile.setArgs(1);
        configFile.setArgName("file");
        addOption(configFile);
    }
}
