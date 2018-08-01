/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * A set of data showing, for a single DuraCloud account, which spaces
 * from which providers should be duplicated to another space on another
 * provider.
 *
 * This class also handles the work of loading this data set from a file
 * stream, allowing it to be stored in a DuraCloud account and be read here.
 *
 * @author Bill Branan
 * Date: 10/18/13
 */
public class DuplicationPolicy {

    private static final ObjectMapper objMapper = new ObjectMapper();

    private Map<String, LinkedHashSet<DuplicationStorePolicy>>
        spaceDuplicationStorePolicies = new HashMap<>();

    private LinkedHashSet<DuplicationStorePolicy> defaultPolicies = new LinkedHashSet<>();

    private List<String> spacesToIgnore = new LinkedList<>();

    /**
     * A set of default policies.
     *
     * @return
     */
    public LinkedHashSet<DuplicationStorePolicy> getDefaultPolicies() {
        return defaultPolicies;
    }

    /**
     * A list of spaces that should be ignored by the duplication task generator.
     *
     * @return
     */
    public List<String> getSpacesToIgnore() {
        return spacesToIgnore;
    }

    public Map<String, LinkedHashSet<DuplicationStorePolicy>> getSpaceDuplicationStorePolicies() {
        return spaceDuplicationStorePolicies;
    }

    /**
     * A set of spaces which have policies associated with them.
     *
     * @return
     */
    @JsonIgnore
    public Set<String> getSpaces() {
        return spaceDuplicationStorePolicies.keySet();
    }

    /**
     * Retrieve the duplication store policies associated with a space.   If no policies are set
     * explicitly for that space, the method returns the default store policies.
     *
     * @param spaceId
     * @return
     */
    public Set<DuplicationStorePolicy> getDuplicationStorePolicies(String spaceId) {
        if (!spaceId.startsWith("x-") && !spacesToIgnore.contains(spaceId)) {
            LinkedHashSet<DuplicationStorePolicy> policies = spaceDuplicationStorePolicies.get(spaceId);
            return policies != null && !policies.isEmpty() ? policies : defaultPolicies;
        }
        return null;
    }

    /**
     * Adds a DuplicationStorePolicy for the specified space ID.
     *
     * @param spaceId  the space ID to add the DuplicationStorePolicy for
     * @param dupStore the DuplicationStorePolicy
     * @return true if added, false otherwise.  False will be returned if the
     * Set<DuplicationStorePolicy> for the specified space already contains the
     * dupStore.
     */
    public boolean addDuplicationStorePolicy(String spaceId,
                                             DuplicationStorePolicy dupStore) {
        LinkedHashSet<DuplicationStorePolicy> dupStores =
            spaceDuplicationStorePolicies.get(spaceId);
        if (dupStores == null) {
            dupStores = new LinkedHashSet<>();
            spaceDuplicationStorePolicies.put(spaceId, dupStores);
        }
        return dupStores.add(dupStore);
    }

    /**
     * Marshals a json stream into a duplication policy object.
     *
     * @param policyStream
     * @return
     * @throws IOException
     */
    public static DuplicationPolicy unmarshall(InputStream policyStream)
        throws IOException {
        return objMapper.readValue(policyStream, DuplicationPolicy.class);
    }

    /**
     * Unmarshals a duplication policy into a json string.
     *
     * @param duplicationPolicy
     * @return
     * @throws IOException
     */
    public static String marshall(DuplicationPolicy duplicationPolicy)
        throws IOException {
        return objMapper.writeValueAsString(duplicationPolicy);
    }
}
