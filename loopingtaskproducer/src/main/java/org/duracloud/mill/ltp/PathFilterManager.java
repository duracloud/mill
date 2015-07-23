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

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows users to flexibly include and exclude accounts, stores and spaces
 * in various looping task production contexts.  The include and exclude patterns must 
 * follow this pattern:  /(account|*)/(store|*)/(spaceId|*).  
 * 
 * Inclusions are checked before exclusions.  If no inclusions are specified, everything that is not excluded is assumed to be included.
 * 
 * @author Daniel Bernstein Date: May 5, 2014
 */
public class PathFilterManager {
    private static final Logger log = LoggerFactory
            .getLogger(PathFilterManager.class);
    private List<String> exclusions;
    private List<String> inclusions;

    public PathFilterManager() {
    }

    /**
     * @param file
     */
    public void setExclusions(File file) {
        checkFileParam(file);
        this.exclusions = loadPatterns(file, "exclusion");
    }

    /**
     * @param file
     */
    public void setInclusions(File file) {
        checkFileParam(file);
        this.inclusions = loadPatterns(file, "inclusion");
    }

    private List<String> loadPatterns(File file,
                                       String type) {
        try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            LineIterator it = new LineIterator(r);
            List<String> patterns = new ArrayList<>();
            while (it.hasNext()) {
                String pattern = it.next();
                if(!pattern.startsWith("#")){
                    addPattern(type, patterns, pattern);
                }
            }

            return patterns;
        } catch (Exception ex) {
            throw new DuraCloudRuntimeException(ex);
        }
    }

    private void
            addPattern(String type, List<String> patterns, String pattern) {
        pattern = validateAndScrubPattern(pattern);
        patterns.add(pattern);
        log.info("Added " + type + " pattern: {}", pattern);
    }

    /**
     * @param pattern
     */
    private String validateAndScrubPattern(String pattern) {
        String regex = "(/([*]|[A-Za-z0-9_.-]+[*]?)){3}";
        if(!StringUtils.isEmpty(pattern)){
            if(pattern.matches(regex)){
                return pattern.trim();
            }
        }
        
        throw new DuraCloudRuntimeException(
                                            "pattern \""+pattern+"\" is invalid.  " +
                                            "Must be start with a forward slash, contain three slashes, " +
                                            "and after each slash must be at least one alpha numeric charactor or * character.");
    }

    private void checkFileParam(File file) {
        if (file == null)
            throw new IllegalArgumentException("file must be non-null");
        if (!file.exists())
            throw new IllegalArgumentException(file.getAbsolutePath()
                    + " does not exist");
    }

    /**
     * Returns true if the path is included and not excluded. A path is included
     * if the inclusions list is null or empty or the path matches an item in
     * the list.
     * 
     * @param path
     * @return
     */
    public boolean isExcluded(String path) {
        if (inclusions != null && !inclusions.isEmpty()) {
            if (!matchesList(path, inclusions, false)) {
                log.debug("{} does not match an inclusion: skipping...", path);
                return true;
            }
        }

        if (exclusions == null) {
            return false;
        }

        if (matchesList(path, exclusions,true)) {
            log.debug("{} matches exclusions: skipping...", path);
            return true;
        }
        return false;
    }

    private boolean matchesList(String path, List<String> list, boolean matchAllSegments) {
        for (String pattern : list) {
            if(matches(pattern, path, matchAllSegments)){
                return true;
            }
        }
        return false;
    }

    /**
     * @param pattern
     * @param path
     * @param matchAllSegments 
     * @return
     */
    private boolean matches(String pattern, String path, boolean matchAllSegments) {
        String[] pathSegments = path.substring(1).split("/");
        String[] patternSegments = pattern.substring(1).split("/");
        
        
        for(int i = 0; i < pathSegments.length; i++){
            String pathSegment = pathSegments[i].trim();
            String patternSegment = patternSegments[i].trim();
            boolean endsWithWildCard = patternSegment.endsWith("*");
            boolean entireSegmentIsWild = endsWithWildCard && patternSegment.length()==1;
            if(entireSegmentIsWild){
                continue;
            }else if(endsWithWildCard){
                //strip off wildcard
                patternSegment = patternSegment.substring(0, patternSegment.length()-1);
                if(!pathSegment.startsWith(patternSegment)){
                    return false;
                }
            }else{
                if(!pathSegment.equals(patternSegment)){
                    return false;
                }
            }
        }
        
        if(matchAllSegments) {
            if(pathSegments.length != patternSegments.length){
                int diff = patternSegments.length - pathSegments.length;
                int lowerBound = patternSegments.length - diff;
                for(int i = patternSegments.length-1; i >= lowerBound; i--){
                    if(!patternSegments[i].equals("*")){
                        return false;
                    }
                }
            }
        }
        
        return true;
        
    }
}
