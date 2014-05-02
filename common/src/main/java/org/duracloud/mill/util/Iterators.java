/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.Searchable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.duracloud.common.collection.StreamingIterator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Static utility methods pertaining to Iterator instances.
 *
 * @author Erik Paulsson
 *         Date: 4/24/14
 */
public class Iterators {

    private static final CacheManager cacheManager = CacheManager.newInstance(
    Iterators.class.getResource("/ehcache.xml"));

    /**
     * Returns an Iterator contain the difference of elements contained in the
     * provided Iterators. The returned Iterator contains all elements that are
     * contained by iterA and not contained by iterB. iterB may also contain
     * elements not present in iterA; these are simply ignored.
     * @param iterA
     * @param iterB
     * @return an Iterator containing elements in iterA but not in iterB
     */
    public static Iterator<String> difference(Iterator<String> iterA, Iterator<String> iterB)
        throws IOException {
        String cacheName = "compare-" + System.currentTimeMillis();
        cacheManager.addCache(cacheName);
        Cache cache = cacheManager.getCache(cacheName);

        while(iterB.hasNext()) {
            String item = iterB.next();
            cache.put(new Element(item, null));
        }

        int diffCnt = 0;
        File diffFile = new File(System.getProperty("java.io.tmpdir") +
                                 File.separator + "diff-" +
                                 System.currentTimeMillis() + ".txt");
        FileWriter fileWriter = new FileWriter(diffFile);
        while(iterA.hasNext()) {
            String item = iterA.next();
            if(! cache.isKeyInCache(item)) {
                // write item to file
                fileWriter.write(item + "\n");
                diffCnt++;
                if(diffCnt % 100 == 0) {
                    fileWriter.flush();
                }
            }
        }
        fileWriter.close();

        // All done with the cache, clean it up
        cache.removeAll();
        cacheManager.removeCache(cache.getName());

        if(diffCnt > 0) {
            return new FileLineIterator(diffFile);
        } else {
            // nothing written to the file so not needed, delete it
            diffFile.delete();
            return new ArrayList<String>(0).iterator();
        }
    }
}
