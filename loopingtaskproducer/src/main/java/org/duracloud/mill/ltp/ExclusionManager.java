/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.LineIterator;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: May 5, 2014
 */
public class ExclusionManager {
    private static final Logger log = LoggerFactory.getLogger(ExclusionManager.class);
    private List<Pattern> exclusions;
    
    public ExclusionManager(){}
    
    /**
     * @param file
     */
    public ExclusionManager(File file) {
        try(Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
            LineIterator it = new LineIterator(r);
            exclusions = new ArrayList<>();
            while(it.hasNext()){
                String pattern = it.next();
                this.exclusions.add(Pattern.compile(pattern));
                log.info("Added exclusion pattern: {}", pattern);
            }
        } catch(Exception ex){
            throw new DuraCloudRuntimeException(ex);
        }
    }

    /**
     * Returns true if the path matches one of the exclusions.
     * @param path
     * @return
     */
    public boolean isExcluded(String path){
        if(exclusions == null) return false;
        for(Pattern exclusion : exclusions){
            Matcher matcher = exclusion.matcher(path);
            if(matcher.matches()){
                log.debug("{} matches exclusion list: skipping...", path);
                return true;
            }
        }
        return false;
    }
}
