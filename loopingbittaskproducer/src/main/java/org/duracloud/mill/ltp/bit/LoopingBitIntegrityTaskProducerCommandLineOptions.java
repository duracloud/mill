/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.bit;

import org.apache.commons.cli.Option;
import org.duracloud.mill.util.CommonCommandLineOptions;

/**
 * @author Daniel Bernstein
 * Date: May 5, 2014
 */
public class LoopingBitIntegrityTaskProducerCommandLineOptions extends CommonCommandLineOptions {
    private static final long serialVersionUID = 1L;

    public static final String EXCLUSION_LIST_OPTION = "x";

    public static final String INCLUSION_LIST_OPTION = "i";

    /**
     *
     */
    public LoopingBitIntegrityTaskProducerCommandLineOptions() {
        super();
        Option exlucsionsList =
            new Option(EXCLUSION_LIST_OPTION, "exclusion-list", true,
                       "A file containing exclusions as regular expressions, one expression per " +
                       "line. Expressions will be matched against the following path: " +
                       "/{account}/{storeId}/{spaceId}");
        exlucsionsList.setArgs(1);
        exlucsionsList.setArgName("file");
        addOption(exlucsionsList);

        Option inclusionList =
            new Option(INCLUSION_LIST_OPTION, "inclusion-list", true,
                       "A file containing inclusions as regular expressions, one expression per " +
                       "line. Expressions will be matched against the following path: " +
                       "/{account}/{storeId}/{spaceId}");
        inclusionList.setArgs(1);
        inclusionList.setArgName("file");
        addOption(inclusionList);

    }
}
