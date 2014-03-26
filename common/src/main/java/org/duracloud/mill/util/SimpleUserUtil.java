/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.util;

import org.duracloud.common.error.NoUserLoggedInException;
import org.duracloud.common.util.UserUtil;

/**
 * @author Bill Branan
 *         Date: 3/25/14
 */
public class SimpleUserUtil implements UserUtil {

    public static final String USER = "root";

    @Override
    public String getCurrentUsername() throws NoUserLoggedInException {
        return USER;
    }

}
