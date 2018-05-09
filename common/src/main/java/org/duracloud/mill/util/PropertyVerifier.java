/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 * Date: Dec 4, 2014
 */
public class PropertyVerifier {

    private static Logger log = LoggerFactory.getLogger(PropertyVerifier.class);
    private Collection<PropertyDefinition> propDefs;

    public PropertyVerifier(Collection<PropertyDefinition> propDefs) {
        if (propDefs == null || propDefs.size() == 0) {
            throw new IllegalArgumentException("propDefs must contain at least one property definition.");
        }
        this.propDefs = propDefs;
    }

    /**
     * @param properties
     * @return A list of failed properties, including properties that were
     * required and missing, or optional properties with invalid values.
     * If the returned collection is empty, validation was successful.
     */
    public Collection<PropertyDefinition> validateProperties(Properties properties) {
        List<PropertyDefinition> failedDefinitions = new LinkedList<>();
        for (PropertyDefinition def : propDefs) {
            String value = properties.getProperty(def.getName());
            if (def.isRequired() && null == value) {
                failedDefinitions.add(def);
                continue;
            }

            if (def.getValidValues().size() > 0 && null != value) {
                if (!def.getValidValues().contains(value)) {
                    failedDefinitions.add(def);
                }
            }
        }
        return failedDefinitions;
    }

    public void logFailuresAndThrowRuntime(Collection<PropertyDefinition> failures) {
        StringBuilder error = new StringBuilder(
            "The property file is missing required field and/or contains invalid values. See below for details:");
        //Failures
        for (PropertyDefinition def : failures) {
            String validValues = "";

            error.append("\n");
            if (def.getValidValues().size() > 0) {
                validValues = ", valid values: (" + StringUtils.join(def.getValidValues(), " | ") + ")";
            }
            error.append(MessageFormat.format("    property:  {0}, required: {1} {2}", def.getName(), def.isRequired(),
                                              validValues));
        }

        throw new RuntimeException(error.toString());
    }

    public void logValues(Properties props) {
        StringBuilder message = new StringBuilder("Current configuration:");
        //Failures
        for (PropertyDefinition def : this.propDefs) {
            message.append("\n");
            String value = props.getProperty(def.getName());
            if (def.isSensitive()) {
                value = "*****************";
            }
            message.append(MessageFormat.format("    {0}: {1}", def.getName(), value));
        }

        log.info(message.toString());
    }

    /**
     * @param properties
     */
    public void verify(Properties properties) {
        Collection<PropertyDefinition> failures = validateProperties(properties);
        if (failures.size() > 0) {
            logFailuresAndThrowRuntime(failures);
        } else {
            logValues(properties);
        }
    }
}
