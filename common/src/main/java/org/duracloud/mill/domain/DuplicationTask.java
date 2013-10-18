/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.domain;

/**
 * @author Bill Branan
 *         Date: 10/18/13
 */
public class DuplicationTask implements Task {

    private String sourceStoreId;
    private String destStoreId;
    private String spaceId;
    private String contentId;

    public String getSourceStoreId() {
        return sourceStoreId;
    }

    public void setSourceStoreId(String sourceStoreId) {
        this.sourceStoreId = sourceStoreId;
    }

    public String getDestStoreId() {
        return destStoreId;
    }

    public void setDestStoreId(String destStoreId) {
        this.destStoreId = destStoreId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

}
