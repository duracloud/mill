/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

/**
 * DuplicationStorePolicy defines the source store to duplicate from and which
 * destination store to duplicate to.  This class is used in DuplicationPolicy,
 * each space to be duplicated as defined in DuplicationPolicy will have a list
 * of one or more DuplicationStorePolicy defining the duplication order for the
 * space.
 * @author Erik Paulsson
 *         Date: 10/29/13
 */
public class DuplicationStorePolicy {

    private String srcStoreId;
    private String destStoreId;

    public String getSrcStoreId() {
        return srcStoreId;
    }

    public void setSrcStoreId(String srcStoreId) {
        this.srcStoreId = srcStoreId;
    }

    public String getDestStoreId() {
        return destStoreId;
    }

    public void setDestStoreId(String destStoreId) {
        this.destStoreId = destStoreId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DuplicationStorePolicy that = (DuplicationStorePolicy) o;

        if (!destStoreId.equals(that.destStoreId)) {
            return false;
        }
        if (!srcStoreId.equals(that.srcStoreId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = srcStoreId.hashCode();
        result = 31 * result + destStoreId.hashCode();
        return result;
    }
}
