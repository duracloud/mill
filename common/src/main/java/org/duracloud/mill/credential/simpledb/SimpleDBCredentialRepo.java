/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credential.simpledb;

import java.io.File;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.duracloud.mill.credential.CredentialRepoBase;
import org.duracloud.mill.credential.CredentialsGroup;
/**
 * 
 * @author Daniel Bernstein
 *
 */
public class SimpleDBCredentialRepo extends CredentialRepoBase {
    private static final String CREDENTIALS_FILE_PATH = "credentials.file.path";
    private Map<String,CredentialsGroup> map;
    
    public SimpleDBCredentialRepo() {

        String path = System.getProperty(CREDENTIALS_FILE_PATH);

        if (path == null) {
            throw new RuntimeException("System property "
                    + CREDENTIALS_FILE_PATH + " not defined.");
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("File not found: "
                    + file.getAbsoluteFile());
        }
        
        ObjectMapper m = new ObjectMapper();
        try {
            this.map = m.readValue(file, new TypeReference<Map<String,CredentialsGroup>>() { });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @Override
    public CredentialsGroup getCredentialGroupByAccountId(String key) {
        return map.get(key);
    }
}
