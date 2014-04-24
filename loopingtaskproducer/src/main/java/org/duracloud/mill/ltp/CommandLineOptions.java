/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 23, 2014
 */
public class CommandLineOptions extends Options{
    
    public static final String CONFIG_FILE_OPTION           = "c";
    public static final String MAX_TASK_QUEUE_SIZE_OPTION   = "m";
    public static final String STATE_FILE_PATH              = "s";
    public static final String FREQUENCY_OPTION             = "f";

    public CommandLineOptions(){
        super();
        
        Option configFile = new Option(CONFIG_FILE_OPTION, "config-file", true,
                "A properties file containing configuration info");
        configFile.setArgs(1);
        configFile.setArgName("file");
        addOption(configFile);

        Option help = new Option("h", "help");
        addOption(help);

        Option frequencyOption = new Option(
                FREQUENCY_OPTION,
                "frequency",
                true,
                "The frequency for a complete run through all store policies. Specify in hours (e.g. 3h), days (e.g. 3d), or months (e.g. 3m). Default is 1m - i.e. one month");
        frequencyOption.setRequired(false);
        addOption(frequencyOption);

        Option stateFile = new Option(STATE_FILE_PATH, "state-file-path", true,
                "Indicates the path of file containing state info");
        stateFile.setArgs(1);
        stateFile.setRequired(true);
        stateFile.setArgName("file");
        addOption(stateFile);

        Option maxTaskQueueSize = new Option(
                MAX_TASK_QUEUE_SIZE_OPTION,
                "max-task-queue-size",
                true,
                "Indicates how large the task queue should be allowed to grow before the Looping Task Producer quits."
                        + "directory should be used.");
        maxTaskQueueSize.setArgs(1);
        maxTaskQueueSize.setArgName("integer");
        addOption(maxTaskQueueSize);

    }
}
