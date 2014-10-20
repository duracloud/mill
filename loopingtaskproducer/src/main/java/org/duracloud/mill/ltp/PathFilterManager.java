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
import org.apache.commons.lang3.StringUtils;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein Date: May 5, 2014
 */
public class PathFilterManager {
    private static final Logger log = LoggerFactory
            .getLogger(PathFilterManager.class);
    private List<Pattern> exclusions;
    private List<Pattern> inclusions;

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

    private List<Pattern> loadPatterns(File file,
                                       String type) {
        try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            LineIterator it = new LineIterator(r);
            List<Pattern> patterns = new ArrayList<>();
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
            addPattern(String type, List<Pattern> patterns, String pattern) {
        patterns.add(Pattern.compile(pattern));
        log.info("Added " + type + " pattern: {}", pattern);
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
            if (!matchesList(path, inclusions)) {
                log.debug("{} does not match an inclusion: skipping...", path);
                return true;
            }

        }

        if (exclusions == null) {
            return false;
        }

        if (matchesList(path, exclusions)) {
            log.debug("{} matches exclusions: skipping...", path);
            return true;
        }
        return false;
    }

    private boolean matchesList(String path, List<Pattern> list) {
        for (Pattern pattern : list) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }
}
