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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
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
    private File tempStateFile;
    private State<T> state = new State<>();
    private Class<T> klazz;

    /**
     *
     */
    public StateManager(String path, Class<T> klazz) {
        //this temp file is used to prevent state file corruption. See flush() method below
        //for details.
        tempStateFile = new File(path + ".tmp");
        tempStateFile.deleteOnExit();
        stateFile = new File(path);
        final boolean exists = stateFile.exists();
        final long length = stateFile.length();

        if (stateFile.exists() && stateFile.length() > 0) {
            ObjectMapper m = new ObjectMapper();
            JavaType type = m.getTypeFactory().constructParametricType(State.class, klazz);

            try {
                this.state = m.readValue(stateFile, type);
                log.info("State file ( {} ) successfully read.", path);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            log.info("State file ( {} ) could not be read. Creating new state file. File exists: {}, file.length: {}",
                     path, exists, length);
            state = new State<>();
        }
    }

    private void flush() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            //first write results to the temp file
            mapper.writeValue(this.tempStateFile, this.state);
            log.debug("wrote {} to {}", this.state, this.tempStateFile.getAbsolutePath());
            //once results have been flushed to the temp file
            //delete the state file
            this.stateFile.delete();
            //move the temp state file into its place.
            FileUtils.moveFile(this.tempStateFile,  this.stateFile);
            log.debug("updated state:  moved  {} to {}", this.tempStateFile.getAbsolutePath(),
                    this.stateFile.getAbsolutePath());
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
