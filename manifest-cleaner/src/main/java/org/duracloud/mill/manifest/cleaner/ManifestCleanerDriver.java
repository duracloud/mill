/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest.cleaner;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.mill.db.util.MillJpaPropertiesVerifier;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.util.SystemPropertyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 
 * @author Daniel Bernstein
 * 
 */

public class ManifestCleanerDriver {

    private static final Logger log = LoggerFactory
            .getLogger(ManifestCleanerDriver.class);

    private static final String CONFIG_FILE_OPTION = "c";
    private static final String EXPIRATION_TIME = "t";

    private static void usage() {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(80);
        help.printHelp(ManifestCleanerDriver.class.getCanonicalName(),
                       getOptions());
    }

    private static CommandLine parseArgs(String[] args) {
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (ParseException e) {
            System.err.println(e);
            die();
        }
        return cmd;
    }

    private static void die() {
        usage();
        System.exit(1);
    }

    private static Options getOptions() {
        Options options = new Options();
        Option configFile = new Option(CONFIG_FILE_OPTION,
                                       "config-file-path",
                                       true,
                                       "Path to the mill config file");
        configFile.setArgs(1);
        configFile.setArgName("path");
        configFile.setRequired(true);
        options.addOption(configFile);

        Option expirationTime = new Option(EXPIRATION_TIME,
                                           "expiration-time",
                                           true,
                                           "time in seconds, minutes, hours, or days after which items deleted items should be purged. "
                                                   + "Expected format: [number: 0-n][timeunit:s,m,h,d]. "
                                                   + "For example 2 hours would be represented as 2h");
        expirationTime.setArgs(1);
        expirationTime.setArgName("time");
        expirationTime.setRequired(true);
        options.addOption(expirationTime);

        return options;
    }

    public static void main(String[] args) {
        try {

            CommandLine cmd = parseArgs(args);

            String time = cmd.getOptionValue(EXPIRATION_TIME);
            Date expirationDate;
            expirationDate = parseExpirationDate(time);

            String configPath = cmd.getOptionValue(CONFIG_FILE_OPTION);
            SystemPropertyLoader.load(configPath);
            new MillJpaPropertiesVerifier().verify();
            ApplicationContext context = new AnnotationConfigApplicationContext("org.duracloud.mill");
            log.info("spring context initialized.");
            ManifestStore store = context.getBean(ManifestStore.class);
            Long deleted = store.purgeDeletedItemsBefore(expirationDate);
            log.info("Deleted {} items that were flagged as deleted before {}",
                     deleted,
                     expirationDate);

        } catch (Exception e) {
           log.error(e.getMessage(), e);
        } finally{
            log.info("exiting...");
        }
    }

    /**
     * @param time
     * @return
     * @throws ParseException
     */
    private static Date parseExpirationDate(String time) throws ParseException {

        Calendar c = Calendar.getInstance();
        String pattern = "([0-9]+)([smhd])";
        if (!time.matches(pattern)) {
            throw new ParseException(time + " is not a valid time value.");
        }

        int amount = Integer.parseInt(time.replaceAll(pattern, "$1"));
        String units = time.replaceAll(pattern, "$2");

        int field = Calendar.SECOND;
        if (units.equals("m")) {
            field = Calendar.MINUTE;
        } else if (units.equals("h")) {
            field = Calendar.HOUR;
        } else if (units.equals("d")) {
            field = Calendar.DATE;
        } else {
            // should never happen.
            throw new RuntimeException("unit " + units + " not recognized.");
        }

        c.add(field, -1 * amount);
        return c.getTime();

    }

    private static void setSystemProperty(String key, String value) {
        log.info("Setting system property {} to {}", key, value);
        System.setProperty(key, value);
    }
}
