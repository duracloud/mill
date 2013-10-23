/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import java.io.InputStream;

/**
 * A set of data showing, for a single DuraCloud account, which spaces
 * from which providers should be duplicated to another space on another
 * provider.
 *
 * This class also handles the work of loading this data set from a file
 * stream, allowing it to be stored in a DuraCloud account and be read here.
 *
 * @author Bill Branan
 *         Date: 10/18/13
 */
public class DuplicationPolicy {

    public static DuplicationPolicy parse(InputStream policyStream) {
        DuplicationPolicy policy = new DuplicationPolicy();

        // TODO implement unmarshalling of data

        return policy;
    }

}
