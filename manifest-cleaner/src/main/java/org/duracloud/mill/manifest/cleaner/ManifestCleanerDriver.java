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
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.manifest.ManifestStore;
import org.duracloud.mill.util.CommonCommandLineOptions;
import org.duracloud.mill.util.DriverSupport;
import org.duracloud.mill.util.PropertyDefinition;
import org.duracloud.mill.util.PropertyDefinitionListBuilder;
import org.duracloud.mill.util.PropertyVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Daniel Bernstein
 */

public class ManifestCleanerDriver extends DriverSupport {

    private static Logger log = LoggerFactory
        .getLogger(ManifestCleanerDriver.class);

    public ManifestCleanerDriver() {
        super(new CommonCommandLineOptions());
    }

    public static void main(String[] args) {
        new ManifestCleanerDriver().execute(args);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.duracloud.mill.util.DriverSupport#executeImpl(org.apache.commons.
     * cli.CommandLine)
     */
    @Override
    protected void executeImpl(CommandLine cmd) {
        try {
            List<PropertyDefinition> defintions =
                new PropertyDefinitionListBuilder().addMillDb()
                                                   .addManifestExpirationDate()
                                                   .build();
            PropertyVerifier verifier = new PropertyVerifier(defintions);
            verifier.verify(System.getProperties());
            String time = System.getProperty(ConfigConstants.MANIFEST_EXPIRATION_TIME);
            Date expirationDate;
            expirationDate = parseExpirationDate(time);

            ApplicationContext context = new AnnotationConfigApplicationContext("org.duracloud.mill");
            log.info("spring context initialized.");
            ManifestStore store = context.getBean(ManifestStore.class);
            log.info("beginning purge of deleted items");

            long deleted = 0l;
            long total = 0l;
            while ((deleted = store.purgeDeletedItemsBefore(expirationDate)) > 0) {
                total += deleted;
                log.info("Deleted {} items that were flagged as deleted before {}",
                         deleted,
                         expirationDate);
            }

            log.info("Purge completed: Deleted a grand total of {} items that were flagged as deleted before {}",
                     total,
                     expirationDate);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            log.info("exiting...");
        }
    }

    /**
     * @param time
     * @return
     * @throws ParseException
     */
    private Date parseExpirationDate(String time) throws ParseException {

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

}
