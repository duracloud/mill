/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.storagestats.aws;

/**
 * @author Bill Branan 
 * Date: 10/29/2015
 */
public class BucketStats {

    private String bucketName;

    private long totalItems;

    private long totalBytes;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    @Override
    public String toString() {
        return "{" + "bucketName='" + bucketName + ", totalItems=" + totalItems
                + ", totalBytes=" + totalBytes + '}';
    }
}
