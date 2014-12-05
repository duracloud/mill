/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Daniel Bernstein
 *         Date: Dec 4, 2014
 */
public class PropertyVerifier {

    private Collection<PropertyDefinition> propDefs;
    
    public PropertyVerifier(Collection<PropertyDefinition> propDefs){
        if(propDefs == null || propDefs.size() == 0){
            throw new IllegalArgumentException("propDefs must contain at least one property definition.");
        }
        this.propDefs = propDefs;
    }
    
    /**
     * 
     * @param properties
     * @return A list of failed properties, including properties that were
     *         required and missing, or optional properties with invalid values.
     *         If the returned collection is empty, validation was successful.
     */
    public Collection<PropertyDefinition> validateProperties(Properties properties){
        List<PropertyDefinition> failedDefinitions = new LinkedList<>();
        for(PropertyDefinition def : propDefs){
            String value =  properties.getProperty(def.getName());
            if(def.isRequired() && null == value){
                failedDefinitions.add(def);
                continue;
            }
            
            if(def.getValidValues().size() > 0 && null != value){
                if(!def.getValidValues().contains(value)){
                    failedDefinitions.add(def);
                }
            }
        }
        return failedDefinitions;
    }
    
    public void printFailuresAndExit(Collection<PropertyDefinition> failures){
        PrintStream writer = System.err;
        writer.print("ERROR:  The property file is missing required field and/or contains invalid values. See below for details:\n");
        //Failures
        for(PropertyDefinition def : failures){
            String validValues = "";
            
            if(def.getValidValues().size() > 0){
                validValues = ", valid values: (" + StringUtils.join(def.getValidValues(), " | ") + ")";
            }
            writer.print(MessageFormat.format("    property:  {0}, required: {1} {2}\n", def.getName(), def.isRequired(), validValues));
        }
        
        System.exit(1);
    }
 
    
    public void printValues(Properties props){
        PrintStream writer = System.out;
        writer.print("Current configuration:\n");
        //Failures
        for(PropertyDefinition def : this.propDefs){
            String value =  props.getProperty(def.getName());
            if(def.isSensitive()){
                value = "*****************";
            }
            writer.print(MessageFormat.format("    {0}: {1}\n", def.getName(), value));
        }
    }


    /**
     * @param properties
     */
    public void verify(Properties properties) {
        Collection<PropertyDefinition> failures = validateProperties(properties);
        if(failures.size() > 0){
           printFailuresAndExit(failures); 
        }else{
            printValues(properties);
        }
    }
}
