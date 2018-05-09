/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Bernstein
 * Date: Dec 11, 2013
 */
public class Frequency {
    private String timeUnit;
    private int value;
    private static Map<String, Integer> timeUnitToCalendarMap;

    static {
        timeUnitToCalendarMap = new HashMap<String, Integer>();
        timeUnitToCalendarMap.put("s", Calendar.SECOND);
        timeUnitToCalendarMap.put("M", Calendar.MINUTE);
        timeUnitToCalendarMap.put("h", Calendar.HOUR);
        timeUnitToCalendarMap.put("d", Calendar.DAY_OF_MONTH);
        timeUnitToCalendarMap.put("m", Calendar.MONTH);
    }

    public Frequency(String frequency) throws ParseException {
        parse(frequency);
    }

    /**
     * @param frequency
     */
    private void parse(String frequency) throws ParseException {
        Pattern p = Pattern.compile("([0]|[1-9][0-9]*)([sMhdm])");
        Matcher m = p.matcher(frequency);
        if (!m.matches()) {
            throw new ParseException(frequency + " is not a valid frequency", 0);
        }

        value = Integer.parseInt(m.group(1));
        timeUnit = m.group(2);
    }

    /**
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * @return the timeUnit as Calendar Constant
     */
    public int getTimeUnit() {
        return timeUnitToCalendarMap.get(this.timeUnit);
    }

    public String getTimeUnitAsString() {
        return this.timeUnit;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getValue() + this.timeUnit;
    }
}
