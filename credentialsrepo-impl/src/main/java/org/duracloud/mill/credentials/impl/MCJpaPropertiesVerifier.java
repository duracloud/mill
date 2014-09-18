/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.impl;

import org.duracloud.common.util.SystemPropertiesVerifier;

/**
 * @author Daniel Bernstein
 *         Date: Sep 12, 2014
 */
public class MCJpaPropertiesVerifier extends SystemPropertiesVerifier {
    public MCJpaPropertiesVerifier() {
        super(new String[] {
                "db.name",
                "db.host", 
                "db.port", 
                "db.user",
                "db.pass"});
    }
 
}
