/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.io.File;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * This class is responsible for serializing the state to and from disk.
 * @author Daniel Bernstein
 *	       Date: Nov 5, 2013
 */
public class StateManager {
    private File stateFile;
    private State state;
    /**
     * @param absoluteFile
     */
    public StateManager(String path) {
        stateFile = new File(path);
        if(stateFile.exists()){
            ObjectMapper m = new ObjectMapper();
            try {
                this.state = m.readValue(stateFile,
                        new TypeReference<State>() {
                        });
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }            
        }else{
            state = new State();
        }
    }


    
    private void flush(){
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(this.stateFile, this.state);
        } catch (Exception e) {
            throw new RuntimeException("failed to save " + this.state + " to "
                    + this.stateFile.getAbsolutePath(), e);
        }
    }

    /**
     * @return
     */
    public Set<Morsel> getMorsels() {
        return state.getMorsels();
    }

    /**
     * @param morsels
     */
    public void setMorsels(Set<Morsel> morsels) {
        this.state.setMorsels(morsels);
        flush();
        
    }

}
