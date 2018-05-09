/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit;

import java.io.IOException;
import java.util.Map;

import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.json.JaxbJsonSerializer;

/**
 * @author Daniel Bernstein
 * Date: May 14, 2014
 */
public class AuditLogStoreUtil {

    private static JaxbJsonSerializer<Map<String, String>> MAP_SERIALIZER =
        new JaxbJsonSerializer(Map.class);

    private AuditLogStoreUtil() {
        // Ensures no instances are made of this class, as there are only static members.
    }

    /**
     * @param props
     * @return
     */
    public static String serialize(Map<String, String> props) {
        try {
            return MAP_SERIALIZER.serialize(props);
        } catch (IOException e) {
            throw new DuraCloudRuntimeException(e);
        }
    }

    public static Map<String, String> deserialize(String string) {
        try {
            return MAP_SERIALIZER.deserialize(string);
        } catch (IOException e) {
            throw new DuraCloudRuntimeException(e);
        }
    }

}
