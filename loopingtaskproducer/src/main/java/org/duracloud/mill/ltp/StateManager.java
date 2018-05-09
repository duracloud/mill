/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.io.File;
import java.util.Date;
import java.util.LinkedHashSet;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for serializing the state to and from disk.
 *
 * @author Daniel Bernstein
 * Date: Nov 5, 2013
 */
public class StateManager<T extends Morsel> {
    private static Logger log = LoggerFactory.getLogger(StateManager.class);
    private File stateFile;
    private State<T> state = new State<>();
    private Class<T> klazz;

    /**
     *
     */
    public StateManager(String path, Class<T> klazz) {
        stateFile = new File(path);
        if (stateFile.exists() && stateFile.length() > 0) {
            ObjectMapper m = new ObjectMapper();
            JavaType type = m.getTypeFactory().constructParametricType(State.class, klazz);

            try {
                this.state = m.readValue(stateFile, type);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            state = new State<>();
        }
    }

    private void flush() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(this.stateFile, this.state);
            log.debug("saved {} to {}", this.state, this.stateFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("failed to save " + this.state + " to "
                                       + this.stateFile.getAbsolutePath(), e);
        }
    }

    /**
     * @return
     */
    public LinkedHashSet<T> getMorsels() {
        return state.getMorsels();
    }

    /**
     * @param morsels
     */
    public void setMorsels(LinkedHashSet<T> morsels) {
        this.state.setMorsels(morsels);
        flush();
    }

    /**
     * @return
     */
    public Date getCurrentRunStartDate() {
        return this.state.getCurrentRunStartDate();
    }

    /**
     * @param time
     */
    public void setCurrentRunStartDate(Date time) {
        this.state.setCurrentRunStartDate(time);
        flush();
    }

    /**
     * @return
     */
    public Date getNextRunStartDate() {
        return this.state.getNextRunStartDate();
    }

    /**
     * @param time
     */
    public void setNextRunStartDate(Date time) {
        this.state.setNextRunStartDate(time);
        flush();
    }
}
