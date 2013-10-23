/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a basic unit of work. In essence it describes "what" is to be
 * done. It knows nothing of the "how".
 * 
 * @author Daniel Bernstein
 * 
 */
public final class Task {

    public static final String KEY_TYPE = "type";
    public enum Type {
        BIT, DUP;
    }

    private Type type;
    private Map<String, String> properties = new HashMap<>();

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public String removeProperty(String key) {
        return properties.remove(key);
    }
}
