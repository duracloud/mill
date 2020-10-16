/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import org.apache.commons.cli.CommandLine;
import org.duracloud.mill.util.CommonCommandLineOptions;
import org.duracloud.mill.util.DriverSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A main class responsible for parsing command line arguments and launching the
 * Looping Task Producer.
 *
 * @author Daniel Bernstein
 * Date: Nov 4, 2013
 */
public abstract class LoopingTaskProducerDriverSupport extends DriverSupport {
    private static Logger log = LoggerFactory.getLogger(LoopingTaskProducerDriverSupport.class);

    /**
     *
     */
    public LoopingTaskProducerDriverSupport(CommonCommandLineOptions options) {
        super(options);
    }

    @Override
    final protected void executeImpl(CommandLine cmd) {

        try {

            LoopingTaskProducer producer = buildTaskProducer();
            producer.run();

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage(), ex);
            System.exit(1);
        }

        log.info("looping task producer completed successfully.");
        System.exit(0);
    }

    /**
     * @return
     */
    protected abstract LoopingTaskProducer buildTaskProducer();

    /**
     * @param key
     * @return
     */
    protected int getMaxQueueSize(String key) {
        String maxTaskQueueSizeOption = System.getProperty(key);
        int maxTaskQueueSize = 10 * 1000;

        if (maxTaskQueueSizeOption != null) {
            maxTaskQueueSize = Integer.valueOf(maxTaskQueueSizeOption);
        }

        log.info("max task queue size: {}", maxTaskQueueSize);

        return maxTaskQueueSize;
    }

    /**
     * @param cmd
     */
    protected String getTaskQueueName(String key) {
        return System.getProperty(key);
    }

    /**
     * @param cmd
     * @return
     */
    protected Frequency getFrequency(String key) {
        String frequencyStr = System.getProperty(key);
        if (frequencyStr == null) {
            frequencyStr = "1m";
        }

        Frequency frequency = null;
        try {
            frequency = new Frequency(frequencyStr);
            log.info("frequency = {}{}", frequency.getValue(),
                     frequency.getTimeUnitAsString());
        } catch (java.text.ParseException ex) {
            System.out.println("Frequency parameter is invalid: " + frequency
                               + " Please refer to usage for valid examples.");
            die();
        }
        return frequency;
    }

    /**
     * @param startTimeKey
     * @return
     */
    protected LocalTime getStartTime(String startTimeKey) {
        String startTimeStr = System.getProperty(startTimeKey);
        if (startTimeStr == null) {
            return null;
        }

        try {
            LocalTime time = LocalTime.parse(startTimeStr);
            log.info("start time key ({}) = {}", startTimeKey, time.toString());
            return time;
        } catch (DateTimeParseException ex) {
            log.error(startTimeKey + " parameter value is invalid: "
                      + startTimeStr + " Must follow the format HH:dd:ss", ex);
            throw new RuntimeException(ex);
        }
    }
}
