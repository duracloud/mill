/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.manifest.builder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.client.ContentStoreManagerImpl;
import org.duracloud.common.model.Credential;
import org.duracloud.mill.db.util.MillJpaPropertiesVerifier;
import org.duracloud.mill.util.CommonCommandLineOptions;
import org.duracloud.mill.util.DriverSupport;
import org.duracloud.mill.util.PropertyDefinition;
import org.duracloud.mill.util.PropertyDefinitionListBuilder;
import org.duracloud.mill.util.PropertyVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Daniel Bernstein
 * 
 */

public class ManifestBuilderDriver extends DriverSupport {

    private static Logger log = LoggerFactory
            .getLogger(ManifestBuilderDriver.class);

    private static class ManifestBuilderOptions extends CommonCommandLineOptions{
        public ManifestBuilderOptions() {
            super();
            
            addOption("u", "username", true, "DuraCloud username");
            addOption("p", "password", true, "DuraCloud password");
            addOption("h", "host", true, "DuraCloud host");
            addOption("r", "port", true, "DuraCloud port: 443 by default",false);
            addOption("s", "space-list", true, "A list of spaces to be included", false);
            addOption("t", "store-list", true, "A list of storage providers to be included",false);
            addOption("d", "dry-run", false, "Do not modify the manifest - only show what updates will be made.",false);
            addOption("C", "clean", false, "Indicates that the manifest database should be cleared before performing updates.",false);
            addOption("T", "threads", true, "The number of threads to be used. Default: 10",false);
        }
        
        /* (non-Javadoc)
         * @see org.apache.commons.cli.Options#addOption(java.lang.String, java.lang.String, boolean, java.lang.String)
         */
        @Override
        public Options addOption(String opt,
                                 String longOpt,
                                 boolean hasArg,
                                 String description) {
            return addOption(opt, longOpt, hasArg, description,true);
        }
        
        public Options addOption(String opt,
                                 String longOpt,
                                 boolean hasArg,
                                 String description, 
                                 boolean required) {
            Option option = new Option(opt, longOpt,hasArg,description);
            option.setRequired(required);
            return super.addOption(option);
        }
    }
    
    public ManifestBuilderDriver(){
        super(new ManifestBuilderOptions());
    }
    
    public static void main(String[] args) {
        new ManifestBuilderDriver().execute(args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.util.DriverSupport#executeImpl(org.apache.commons.
     * cli.CommandLine)
     */
    @Override
    protected void executeImpl(CommandLine cmd) {
        try {
            
            
            List<PropertyDefinition> defintions = 
                    new PropertyDefinitionListBuilder().addMillDb()
                                                       .build();
            PropertyVerifier verifier = new PropertyVerifier(defintions);
            verifier.verify(System.getProperties());
            new MillJpaPropertiesVerifier().verify();
            ApplicationContext context = 
                    new AnnotationConfigApplicationContext("org.duracloud.mill.db", 
                                                           "org.duracloud.mill.manifest.builder");
            log.info("context initialized");

            boolean dryRun = Boolean.valueOf(cmd.hasOption("d"));
            boolean clean = Boolean.valueOf(cmd.hasOption("C"));
            String host = cmd.getOptionValue("host");
            String port = cmd.getOptionValue("port","443");
            String username = cmd.getOptionValue("u");
            String password = cmd.getOptionValue("p");
            List<String> spaceList = new LinkedList<>();
            if(cmd.hasOption("s")){
                String spaces = cmd.getOptionValue("s", "");
                if(!StringUtils.isEmpty(spaces)){
                    spaceList = Arrays.asList(spaces.split("[,]"));                
                }
            }

            List<String> storeList = new LinkedList<>();

            if(cmd.hasOption("t")){
                String stores = cmd.getOptionValue("t", "");
                if(!StringUtils.isEmpty(stores)){
                    storeList = Arrays.asList(stores.split("[,]"));                
                }
            }
            
            int threads = 0;
            if (cmd.hasOption("T")) {
                threads = Integer.parseInt(cmd.getOptionValue("T", "10"));
            }
            
            String account = host.split("[.]")[0];
            ContentStoreManager storeManager = new ContentStoreManagerImpl(host, port);
            storeManager.login(new Credential(username, password));
            Map<String,ContentStore> contentStoreMap = storeManager.getContentStores();           
            List<ContentStore> contentStores = new LinkedList<>();
            for(String storeId : contentStoreMap.keySet()){
                if(storeList.isEmpty() || storeList.contains(storeId)){
                    ContentStore contentStore = contentStoreMap.get(storeId);
                    contentStores.add(contentStore); 
                }
            }
            
            ManifestBuilder builder = (ManifestBuilder)context.getBean(ManifestBuilder.class);
            builder.init(account, contentStores, spaceList, clean, dryRun, threads);
            builder.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            log.info("exiting...");
        }
    }

}
