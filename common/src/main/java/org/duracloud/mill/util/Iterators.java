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
import org.duracloud.common.collection.StreamingIterator;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Static utility methods pertaining to Iterator instances.
 *
 * @author Erik Paulsson
 *         Date: 4/24/14
 */
public class Iterators {

    private static final CacheManager cacheManager = CacheManager.create();

    /**
     * Returns an Iterator contain the difference of elements contained in the
     * provided Iterators. The returned Iterator contains all elements that are
     * contained by iterA and not contained by iterB. iterB may also contain
     * elements not present in iterA; these are simply ignored.
     * @param iterA
     * @param iterB
     * @return an Iterator containing elements in iterA but not in iterB
     */
    public static <E> Iterator<E> difference(Iterator<E> iterA, Iterator<?> iterB) {
        Cache cache = new Cache("compare-" + System.currentTimeMillis(),
                                100*1000, true, true, 60*10,60*10);
        cacheManager.addCache(cache);

        while(iterA.hasNext()) {
            E item = iterA.next();
            cache.put(new Element(item, item));
        }

        while(iterB.hasNext()) {
            cache.remove(iterB.next());
        }

        if(cache.getSize() > 0) {
            return new StreamingIterator<E>(new EhcacheIteratorSource<E>(cache));
        } else {
            cacheManager.removeCache(cache.getName());
            return new ArrayList<E>(0).iterator();
        }
    }
}
