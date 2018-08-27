/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.audit.taskgenerator;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.client.ContentStoreManagerImpl;
import org.duracloud.common.model.Credential;
import org.duracloud.mill.util.DriverSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 */

public class AuditTaskGeneratorDriver extends DriverSupport {

    private static Logger log = LoggerFactory.getLogger( AuditTaskGeneratorDriver.class );

    public AuditTaskGeneratorDriver() {
        super( new AuditTaskGeneratorOptions() );
    }

    public static void main(String[] args) {
        new AuditTaskGeneratorDriver().execute( args );
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

            boolean dryRun = Boolean.valueOf( cmd.hasOption( "d" ) );
            String host = cmd.getOptionValue( "host" );
            String port = cmd.getOptionValue( "port", "443" );
            String username = cmd.getOptionValue( "u" );
            String password = cmd.getOptionValue( "p" );
            String spaceId = cmd.getOptionValue( "s" );
            String storeId = cmd.getOptionValue( "t" );
            String auditQueueName = cmd.getOptionValue( "q" );

            String account = host.split( "[.]" )[0];
            ContentStoreManager storeManager = new ContentStoreManagerImpl( host, port );
            storeManager.login( new Credential( username, password ) );
            Map<String, ContentStore> contentStoreMap = storeManager.getContentStores();

            final ContentStore store;

            if (storeId != null) {
                store = storeManager.getContentStore( storeId );
            } else {
                store = storeManager.getPrimaryContentStore();
            }

            AuditTaskGenerator generator =
                new AuditTaskGenerator( store, spaceId, dryRun, account, auditQueueName, username );
            generator.execute();
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        } finally {
            log.info( "exiting..." );
        }
    }

    private static class AuditTaskGeneratorOptions extends Options {
        public AuditTaskGeneratorOptions() {
            super();

            addOption( "u", "username", true,
                       "DuraCloud username" );
            addOption( "p", "password", true,
                       "DuraCloud password" );
            addOption( "h", "host", true,
                       "DuraCloud host" );
            addOption( "r", "port", true,
                       "DuraCloud port: 443 by default", false );
            addOption( "s", "space", true,
                       "The space ID", true );
            addOption( "t", "store-id", true,
                       "The storage provider ID", false );
            addOption( "q", "audit-queue", true,
                       "AWS SQS Queue Name" );

            addOption( "d", "dry-run", false,
                       "Do not put tasks on the queue - only show what tasks will be added.", false );
        }

        /* (non-Javadoc)
         * @see org.apache.commons.cli.Options#addOption(java.lang.String, java.lang.String, boolean, java.lang.String)
         */
        @Override
        public Options addOption(String opt,
                                 String longOpt,
                                 boolean hasArg,
                                 String description) {
            return addOption( opt, longOpt, hasArg, description, true );
        }

        public Options addOption(String opt,
                                 String longOpt,
                                 boolean hasArg,
                                 String description,
                                 boolean required) {
            Option option = new Option( opt, longOpt, hasArg, description );
            option.setRequired( required );
            return super.addOption( option );
        }
    }

}
