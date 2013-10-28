/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.file;

import java.io.File;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.CredentialRepoBase;

/**
 * A simple implementation of the Credential Repo based on a local configuration file.
 * 
 * @author Daniel Bernstein
 * 
 */
public class ConfigFileCredentialRepo extends CredentialRepoBase {
    private static final String CREDENTIALS_FILE_PATH = "credentials.file.path";
    private List<AccountCredentials> accountList;

    public ConfigFileCredentialRepo() {

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
            this.accountList = m.readValue(file,
                    new TypeReference<List<AccountCredentials>>() {
                    });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @Override
    public AccountCredentials getAccoundCredentialsBySubdomain(String subdomain) throws AccountCredentialsNotFoundException {
        for(AccountCredentials accountCreds : accountList){
            if(accountCreds.getSubdomain().equals(subdomain)){
                return accountCreds;
            }
        }
        
        throw new AccountCredentialsNotFoundException("No account found with subdomain \""+ subdomain + "\".");
        
    }
}
