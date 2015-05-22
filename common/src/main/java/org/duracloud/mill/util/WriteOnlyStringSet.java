/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import gnu.trove.set.hash.THashSet;

import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author Daniel Bernstein
 *         Date: May 22, 2015
 */
public class WriteOnlyStringSet {
    private THashSet<String> set; 
    public WriteOnlyStringSet(int capacity){
        set = new THashSet<String>(capacity);
    }
    
    private static final MessageDigest MD5 = DigestUtils.getMd5Digest();

    public void add(String string){
        if(string == null){
            return;
        }
        
        set.add(getMd5String(string));
    }
    
    private String getMd5String(String string) {
        return new String(MD5.digest(string.getBytes()));
    }

    public boolean contains(String string){
        if(string == null) return false;
        return set.contains(getMd5String(string));
    }
    
    public int size(){
        return set.size();
    }

    /**
     * @param concat
     */
    public boolean remove(String string) {
        return set.remove(getMd5String(string));
    }
}
