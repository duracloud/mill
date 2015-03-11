/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.taskproducertool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.audit.task.AuditTask;
import org.duracloud.audit.task.AuditTask.ActionType;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.duracloud.common.queue.task.NoopTask;
import org.duracloud.common.queue.task.Task;
import org.duracloud.common.queue.task.Task.Type;
import org.duracloud.common.queue.task.TypedTask;
import org.duracloud.mill.task.DuplicationTask;

/**
 * A simple client for placing various kinds of messages (NOOP, DUP, and BIT) on a task queue.
 * @author Daniel Bernstein 
 *         Date: Oct 28, 2013
 */
public class Driver {

    private static void usage() {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(80);
        help.printHelp(Driver.class.getCanonicalName(), getOptions());
    }

    private static CommandLine parseArgs(String[] args) {
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (ParseException e) {
            System.err.println(e);
            die();
        }
        return cmd;
    }

    private static void die() {
        usage();
        System.exit(1);
    }

    private static Options getOptions() {
        Options options = new Options();
        Option subdomain = new Option("s", "subdomain", true,
                "A duracloud subdomain");
        subdomain.setArgs(1);
        subdomain.setArgName("subdomain");
        subdomain.setRequired(true);
        options.addOption(subdomain);

        Option queue = new Option("q", "queue", true, "A task queue name");
        queue.setArgs(1);
        queue.setArgName("queue");
        queue.setRequired(true);
        options.addOption(queue);

        Option username = new Option("u", "username", true,
                "The queue service username");
        username.setArgs(1);
        username.setArgName("username");
        options.addOption(username);

        Option password = new Option("p", "password", true,
                "The queue service password");
        password.setArgs(1);
        password.setArgName("password");
        options.addOption(password);

        Option type = new Option("t", "type", true,
                "The type of operation: NOOP, DUP, BIT, AUDIT");
        type.setArgs(1);
        type.setArgName("type");
        type.setRequired(true);
        options.addOption(type);

        Option sourceStorageProviderId = new Option("a", "source-provider-id",
                true, "The id of the source storage provider");
        sourceStorageProviderId.setArgs(1);
        sourceStorageProviderId.setArgName("id");
        options.addOption(sourceStorageProviderId);

        Option destStorageProviderId = new Option("b", "dest-provider-id",
                true, "The id of the destination storage provider");
        destStorageProviderId.setArgs(1);
        destStorageProviderId.setArgName("id");
        options.addOption(destStorageProviderId);

        Option spaceId = new Option("d", "space-id", true,
                "The id of the space");
        spaceId.setArgs(1);
        spaceId.setArgName("spaceId");
        options.addOption(spaceId);

        Option contentId = new Option("c", "content-id", true,
                "The id of the source content");
        contentId.setArgs(1);
        contentId.setArgName("contentId");
        options.addOption(contentId);

        return options;
    }

    public static void main(String[] args) {
        try {
            CommandLine cmd = parseArgs(args);
            
            String queue = cmd.getOptionValue("q");
            String username = cmd.getOptionValue("u");
            String password = cmd.getOptionValue("p");
            String subdomain = cmd.getOptionValue("s");
            Type taskType = Task.Type
                    .valueOf(cmd.getOptionValue("t").toUpperCase());
            String sourceStoreId = cmd.getOptionValue("a");
            String destStoreId = cmd.getOptionValue("b");
            String spaceId = cmd.getOptionValue("d");
            String contentId = cmd.getOptionValue("c");

            if(username != null){
                System.setProperty("aws.accessKeyId", username);
            }
            
            if(password != null){
                System.setProperty("aws.secretKey", password);
            }

            TaskQueue taskQueue = new SQSTaskQueue(queue);
            
            TypedTask typedTask;
            
            if(taskType.equals(Type.DUP)) {
                DuplicationTask dupTask = new DuplicationTask();
                dupTask.setAccount(subdomain);
                dupTask.setStoreId(sourceStoreId);
                dupTask.setSourceStoreId(sourceStoreId);
                dupTask.setDestStoreId(destStoreId);
                dupTask.setSpaceId(spaceId);
                dupTask.setContentId(contentId);
                typedTask = dupTask;
            } else if(taskType.equals(Type.NOOP)) {
                NoopTask noopTask = new NoopTask();
                noopTask.setAccount(subdomain);
                noopTask.setSpaceId(spaceId);
                noopTask.setContentId(contentId);
                noopTask.setStoreId(sourceStoreId);
                typedTask = noopTask;
            } else if(taskType.equals(Type.AUDIT)) {
                AuditTask auditTask = new AuditTask();
                auditTask.setAccount(subdomain);
                auditTask.setSpaceId(spaceId);
                auditTask.setContentId(contentId);
                auditTask.setStoreId(sourceStoreId);
                auditTask.setContentChecksum("checksum");
                auditTask.setAction(ActionType.ADD_CONTENT.name());
                auditTask.setContentSize("1000");
                auditTask.setContentMimetype("text/plain");
                typedTask = auditTask;
            } else {
               throw new RuntimeException("taskType " + taskType + " not supported.");
            }
            
            taskQueue.put(typedTask.writeTask());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}