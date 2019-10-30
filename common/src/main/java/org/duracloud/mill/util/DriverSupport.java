/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: Apr 23, 2014
 */
public abstract class DriverSupport {
    private static Logger log = LoggerFactory.getLogger(DriverSupport.class);

    private Options options;

    public DriverSupport(Options options) {
        this.options = options;
    }

    public void die() {
        usage();
        System.exit(1);
    }

    public void usage() {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(80);
        help.printHelp(getClass().getCanonicalName(), options);
    }

    public CommandLine parseArgs(String[] args) {
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = null;
        log.debug(String.valueOf(args));
        try {
            cmd = parser.parse(this.options, args);
        } catch (ParseException e) {
            System.err.println(e);
            die();
        }

        return cmd;
    }

    public final void execute(String[] args) {

        try {
            CommandLine cmd = parseArgs(args);

            if (cmd.hasOption("H")) {
                die();
            }

            processConfigFileOption(cmd);

            executeImpl(cmd);
        } catch (Throwable ex) {
            log.error("failed to startup " + getClass().getCanonicalName()
                      + ": " + ex.getMessage(), ex);
        }

    }

    /**
     * @param cmd
     */
    protected void processConfigFileOption(CommandLine cmd) {
        String configPath = cmd.getOptionValue(CommonCommandLineOptions.CONFIG_FILE_OPTION);
        if (configPath != null) {
            if (new File(configPath).exists()) {
                try {
                    SystemPropertyLoader.load(configPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * @param cmd
     */
    protected abstract void executeImpl(CommandLine cmd);

    protected void setSystemProperty(String name, String value) {
        setSystemProperty(name, value, null);

    }

    protected void setSystemProperty(String name, String value, String defaultValue) {
        if (value == null) {
            value = defaultValue;
        }

        System.setProperty(name, value);
    }

}
