/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Supplier;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;

/**
 * Static utility methods pertaining to Iterator instances.
 *
 * @author Erik Paulsson
 * Date: 4/24/14
 */
public class Iterators {

    private static final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    private static final CacheConfiguration<String, String> cacheConfig =
        CacheConfigurationBuilder.newCacheConfigurationBuilder(
            String.class, String.class,
            ResourcePoolsBuilder.newResourcePoolsBuilder().heap(150, MemoryUnit.MB))
                                 .withExpiry(new ExpiryPolicy<>() {
                                     @Override
                                     public Duration getExpiryForCreation(String s, String s2) {
                                         return Duration.ofSeconds(86400);
                                     }

                                     @Override
                                     public Duration getExpiryForAccess(String s, Supplier<? extends String> supplier) {
                                         return Duration.ofSeconds(86400);
                                     }

                                     @Override
                                     public Duration getExpiryForUpdate(String s, Supplier<? extends String> supplier,
                                                                        String s2) {
                                         return Duration.ofSeconds(86400);
                                     }
                                 })
                                 .build();

    private Iterators() {
        // Ensures no instances are made of this class, as there are only static members.
    }

    /**
     * Returns an Iterator contain the difference of elements contained in the
     * provided Iterators. The returned Iterator contains all elements that are
     * contained by iterA and not contained by iterB. iterB may also contain
     * elements not present in iterA; these are simply ignored.
     *
     * @param iterA
     * @param iterB
     * @return an Iterator containing elements in iterA but not in iterB
     */
    public static Iterator<String> difference(Iterator<String> iterA, Iterator<String> iterB)
        throws IOException {
        String cacheName = "compare-" + System.currentTimeMillis();
        Cache<String, String> cache = cacheManager.createCache(cacheName, cacheConfig);

        while (iterB.hasNext()) {
            String item = iterB.next();
            cache.put(new Element(item, null));
        }

        int diffCnt = 0;
        File diffFile = new File(System.getProperty("java.io.tmpdir") +
                                 File.separator + "diff-" +
                                 System.currentTimeMillis() + ".txt");
        FileWriter fileWriter = new FileWriter(diffFile);
        while (iterA.hasNext()) {
            String item = iterA.next();
            if (!cache.containsKey(item)) {
                // write item to file
                fileWriter.write(item + "\n");
                diffCnt++;
                if (diffCnt % 100 == 0) {
                    fileWriter.flush();
                }
            }
        }
        fileWriter.close();

        // All done with the cache, clean it up
        cache.clear();
        cacheManager.removeCache(cacheName);

        if (diffCnt > 0) {
            return new FileLineIterator(diffFile);
        } else {
            // nothing written to the file so not needed, delete it
            diffFile.delete();
            return new ArrayList<String>(0).iterator();
        }
    }
}
