/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import net.sf.ehcache.Cache;
import org.duracloud.common.collection.IteratorSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * An Ehcache backed IteratorSource.  Useful for providing a way to iterate
 * over collections of arbitrarily large size that may not fit into memory.
 *
 * @author Erik Paulsson
 *         Date: 4/25/14
 */
public class EhcacheIteratorSource<T> implements IteratorSource<T> {

    private Cache cache;
    private Iterator keyIterator;
    private int chunkSize = 200;

    public EhcacheIteratorSource(Cache cache) {
        this(cache, 200);
    }

    /**
     *
     * @param cache
     * @param chunkSize The max length of the collection returned by
     *                  method getNext (size of the chunk)
     */
    public EhcacheIteratorSource(Cache cache, int chunkSize) {
        this.cache = cache;
        this.chunkSize = chunkSize;
        keyIterator = cache.getKeys().iterator();
    }

    @Override
    public Collection<T> getNext() {
        if(keyIterator.hasNext()) {
            int count = 0;
            Collection<T> chunk = new ArrayList<T>(chunkSize);
            while(keyIterator.hasNext() && count <= chunkSize) {
                chunk.add((T)keyIterator.next());
                count++;
            }
            return chunk;
        } else {
            keyIterator = null;
            cache.removeAll();
            cache.getCacheManager().removeCache(cache.getName());
            return null;
        }
    }
}
