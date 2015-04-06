/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.taskreadertool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.duracloud.common.queue.task.Task;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.duracloud.common.queue.aws.SQSTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * A client for reading tasks from a queue.
 * @author Bill Branan 
 *         Date: Jan 22, 2014
 */
public class Driver {

    private static final Logger log = LoggerFactory.getLogger(Driver.class);

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

        Option queue = new Option("q", "queue", true, "A task queue name");
        queue.setArgs(1);
        queue.setArgName("queue");
        queue.setRequired(true);
        options.addOption(queue);

        Option username = new Option("u", "username", true,
                "The queue service username");
        username.setArgs(1);
        username.setArgName("username");
        queue.setRequired(true);
        options.addOption(username);

        Option password = new Option("p", "password", true,
                "The queue service password");
        password.setArgs(1);
        password.setArgName("password");
        queue.setRequired(true);
        options.addOption(password);

        Option output = new Option("o", "output-file-name", true,
                "The name of the output file");
        password.setArgs(1);
        password.setArgName("output");
        options.addOption(output);

        Option delete = new Option("d", "delete", false,
                "Indicates that items read from the queue should be deleted");
        options.addOption(delete);

        return options;
    }

    private static void writeTask(BufferedWriter writer, Task task) {
        try {
            writer.write("Task Type: " + task.getType().toString());
            writer.newLine();
            writer.write("Task Properties: ");
            writer.newLine();
            Map<String, String> taskProps = task.getProperties();
            for(String key : taskProps.keySet()) {
                if(!key.equals("RECEIPT_HANDLE")) { // Leave out receipt handle
                    writer.write("  " + key + "=" + taskProps.get(key));
                    writer.newLine();
                }
            }
            writer.newLine();
        } catch(IOException e) {
            log.error("Unable to write to file due to: " + e.getMessage() +
                      " Task content: " + task.toString());
        }
    }

    public static void main(String[] args) {
        try {
            CommandLine cmd = parseArgs(args);
            
            String queue = cmd.getOptionValue("q");
            String username = cmd.getOptionValue("u");
            String password = cmd.getOptionValue("p");
            boolean delete = cmd.hasOption("d");
            String outputValue = cmd.getOptionValue("o");

            System.setProperty("aws.accessKeyId", username);
            System.setProperty("aws.secretKey", password);

            String outputFilename;
            if(null != outputValue && !outputValue.isEmpty()) {
                outputFilename = outputValue;
            } else {
                outputFilename =
                    queue + "-contents-" + System.currentTimeMillis() + ".txt";
            }
            BufferedWriter writer =
                new BufferedWriter(new FileWriter(outputFilename, true));

            TaskQueue taskQueue = new SQSTaskQueue(queue);

            int readCount = 0;
            int taskQueueSize = taskQueue.size();
            log.warn("TaskQueue size: " + taskQueue.size());

            for(int i=0; i< taskQueueSize; i++) {
                try {
                    Task task = taskQueue.take();
                    writeTask(writer, task);
                    if(delete) {
                        taskQueue.deleteTask(task);
                    }
                    readCount++;
                } catch(TimeoutException e) {
                    log.warn(
                        "Timeout exception, queue is empty: " + e.getMessage());
                }
            }
            log.warn("Read " + readCount + " tasks from queue.");

            writer.close();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}