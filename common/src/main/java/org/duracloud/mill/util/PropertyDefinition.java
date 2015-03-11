/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Bernstein Date: Dec 4, 2014
 */
public class PropertyDefinition {
    private String name;
    private Set<String> validValues;
    private boolean required;
    private boolean sensitive; // indicates that the property's values should
                               // not be printed to any logs

    /**
     * @param name
     * @param validValues
     * @param required
     * @param sensitive
     */
    public PropertyDefinition(String name,
                              String[] validValues,
                              boolean required,
                              boolean sensitive) {
        this.name = name;
        if(validValues == null){
            validValues = new String[0];
        }
        this.validValues = new HashSet<>(Arrays.asList(validValues));
        this.required = required;
        this.sensitive = sensitive;
    }
    
    /**
     * @param name
     * @param required
     */
    public PropertyDefinition(String name,
                              boolean required) {
        this(name, null, required, false);
    }

    public String getName() {
        return name;
    }

    public Set<String> getValidValues() {
        return validValues;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isSensitive() {
        return sensitive;
    }   
    
    

}
