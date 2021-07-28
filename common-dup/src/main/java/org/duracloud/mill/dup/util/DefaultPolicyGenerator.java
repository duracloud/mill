/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup.util;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.client.ContentStoreManagerImpl;
import org.duracloud.common.constant.Constants;
import org.duracloud.common.model.Credential;
import org.duracloud.error.ContentStoreException;
import org.duracloud.mill.config.ConfigConstants;
import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;

/**
 * The default policy generator will connect to a DuraCloud account and create
 * a default duplication policy by assuming that all spaces in the primary
 * store (except admin space of the x-* variety) should be duplicated to all
 * secondary stores.
 *
 * @author Bill Branan
 * Date: 11/7/13
 */
public class DefaultPolicyGenerator {

    private static final String DURACLOUD_PORT = "443";
    private static final String DURACLOUD_ROOT_USER = "root";

    public DuplicationPolicy createDefaultDupPolicy(String account,
                                                    String rootPass)
        throws ContentStoreException {
        ContentStoreManager storeManager = connectToDuraCloud(account, rootPass);
        Map<String, ContentStore> allStores = storeManager.getContentStores();
        ContentStore primaryStore = storeManager.getPrimaryContentStore();

        // Filter out the primary store from the stores list
        String primaryStoreId = primaryStore.getStoreId();
        List<String> secondaryStoreIds = new LinkedList<>();
        for (ContentStore store : allStores.values()) {
            String storeId = store.getStoreId();
            if (!primaryStoreId.equals(storeId)) {
                secondaryStoreIds.add(storeId);
            }
        }

        DuplicationPolicy defaultPolicy = new DuplicationPolicy();
        for (String spaceId : primaryStore.getSpaces()) {
            if (!Constants.SYSTEM_SPACES.contains(spaceId)) {
                for (String storeId : secondaryStoreIds) {
                    DuplicationStorePolicy dupStorePolicy =
                        new DuplicationStorePolicy(primaryStoreId, storeId);
                    defaultPolicy.addDuplicationStorePolicy(spaceId, dupStorePolicy);
                }
            }
        }
        return defaultPolicy;
    }

    /*
     * Connects to a DuraCloud instance as root
     */
    private ContentStoreManager connectToDuraCloud(String account,
                                                   String rootPass)
        throws ContentStoreException {

        String domain = System.getProperty(ConfigConstants.DURACLOUD_SITE_DOMAIN);
        if (domain == null) {
            domain = Constants.DEFAULT_DOMAIN;
        }
        String host = account + "." + domain;

        ContentStoreManager storeManager =
            new ContentStoreManagerImpl(host, DURACLOUD_PORT);
        Credential credential = new Credential(DURACLOUD_ROOT_USER, rootPass);
        storeManager.login(credential);
        return storeManager;
    }

    public static void main(String[] args) throws Exception {
        if (!(args.length == 3)) {
            throw new RuntimeException("Default Policy Generator expects " +
                                       "three parameters:" +
                                       "\n  Account subdomain" +
                                       "\n  Account root password" +
                                       "\n  Directory path to store policy");
        }

        String account = args[0];
        String rootPass = args[1];
        String dirPath = args[2];

        File outputDir = new File(dirPath);
        if (!outputDir.isDirectory()) {
            throw new RuntimeException("The path " + dirPath +
                                       " must point to a directory");
        }

        DefaultPolicyGenerator policyGenerator = new DefaultPolicyGenerator();
        DuplicationPolicy dupPolicy =
            policyGenerator.createDefaultDupPolicy(account, rootPass);

        String policyJson = DuplicationPolicy.marshall(dupPolicy);
        String policyName = account + DuplicationPolicyRepo.DUP_POLICY_SUFFIX;
        File policyFile = new File(outputDir, policyName);

        FileUtils.writeStringToFile(policyFile, policyJson);
    }

}
