/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 23, 2014
 */
public class CommonCommandLineOptions extends Options{
    
    private static final long serialVersionUID = 1;

    public static final String CONFIG_FILE_OPTION           = "c";

    public CommonCommandLineOptions(){
        super();
        
        Option configFile = new Option(CONFIG_FILE_OPTION, "config-file", true,
                "A properties file containing configuration info");
        configFile.setArgs(1);
        configFile.setArgName("file");
        configFile.setRequired(true);
        addOption(configFile);
    }
}
