/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Iterator's items are lines of a file.  The hasNext() method returns true
 * if the file contains anymore lines, false otherwise. The next() method
 * returns the next line in the file.
 * Once all lines have been iterated through and hasNext() returns false the
 * underlying file is deleted.
 *
 * @author Erik Paulsson
 * Date: 5/2/14
 */
public class FileLineIterator extends LineIterator {
    private static Logger log = LoggerFactory.getLogger(FileLineIterator.class);

    private File file;

    public FileLineIterator(File file) throws IOException {
        super(new FileReader(file));
        this.file = file;
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = super.hasNext();
        if (!hasNext) {
            this.close();
        }
        return hasNext;
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (IOException e) {
            log.error("Error closing iterator: " + e.getMessage(), e);
        }

        file.delete();
    }
}
