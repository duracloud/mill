/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import org.apache.commons.cli.Option;
import org.duracloud.mill.ltp.CommandLineOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 23, 2014
 */
public class DuplicationOptions extends CommandLineOptions {
    public static final String DUPLICATION_QUEUE_OPTION     = "d";
    public static final String LOCAL_DUPLICATION_DIR_OPTION = "l";
    public static final String POLICY_BUCKET_SUFFIX         = "p";


    public DuplicationOptions(){
        super();
        Option duplicationQueueName = new Option(DUPLICATION_QUEUE_OPTION,
                "duplication-queue", true, "Name of the duplication queue.");
        duplicationQueueName.setArgs(1);
        duplicationQueueName.setArgName("name");
        addOption(duplicationQueueName);

        Option localDuplicationDir = new Option(LOCAL_DUPLICATION_DIR_OPTION,
                "local-duplication-dir", true,
                "Indicates that a local duplication policy "
                        + "directory should be used.");
        localDuplicationDir.setArgs(1);
        localDuplicationDir.setArgName("file");
        addOption(localDuplicationDir);

        Option policyBucketSuffix = new Option(POLICY_BUCKET_SUFFIX,
                "policy-bucket-suffix", true,
                "The last portion of the name of the S3 bucket where "
                        + "duplication policies can be found.");
        policyBucketSuffix.setRequired(false);
        addOption(policyBucketSuffix);

    }
}
