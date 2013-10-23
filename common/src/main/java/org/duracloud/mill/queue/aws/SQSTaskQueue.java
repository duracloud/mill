/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.queue.aws;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiptHandleIsInvalidException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskNotFoundException;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

/**
 * @author Erik Paulsson
 *         Date: 10/21/13
 */
public class SQSTaskQueue implements TaskQueue {
    private static Logger log = LoggerFactory.getLogger(SQSTaskQueue.class);

    private AmazonSQSClient sqsClient;
    private String queueUrl;
    private Integer visibilityTimeout;  // in seconds

    public enum MsgProp {
        MSG_ID, RECEIPT_HANDLE;
    }

    /**
     * Creates a SQSTaskQueue that serves as a handle to interacting with a remote
     * Amazon SQS Queue.
     * The AmazonSQSClient will search for Amazon credentials on the system as
     * described here:
     * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/sqs/AmazonSQSClient.html#AmazonSQSClient()
     */
    public SQSTaskQueue(String queueUrl, Integer visibilityTimeout) {
        sqsClient = new AmazonSQSClient();
        this.queueUrl = queueUrl;
        this.visibilityTimeout = visibilityTimeout;
    }

    protected Task marshallTask(String msgBody) {
        Properties props = new Properties();
        Task task = null;
        try {
            props.load(new StringReader(msgBody));

            if(props.containsKey(Task.KEY_TYPE)) {
                task = new Task();
                for(final String key: props.stringPropertyNames()) {
                    if(key.equals(Task.KEY_TYPE)) {
                        task.setType(Task.Type.valueOf(props.getProperty(key)));
                    } else {
                        task.addProperty(key, props.getProperty(key));
                    }
                }
            } else {
                log.error("SQS message from " + queueUrl +" does not contain a 'task type'");
            }
        } catch(IOException ioe) {
            log.error("Error creating Task", ioe);
        }
        return task;
    }

    protected String unmarshallTask(Task task) {
        Properties props = new Properties();
        props.setProperty(Task.KEY_TYPE, task.getType().name());
        for(String key: task.getProperties().keySet()) {
            props.setProperty(key, task.getProperty(key));
        }
        StringWriter sw = new StringWriter();
        String msgBody = null;
        try {
            props.store(sw, null);
            msgBody = sw.toString();
        } catch(IOException ioe) {
            log.error("Error unmarshalling Task", ioe);
        }
        return msgBody;
    }

    public void put(Task task) {
        String msgBody = unmarshallTask(task);
        sqsClient.sendMessage(new SendMessageRequest(queueUrl, msgBody));
    }

    public Task take() throws TimeoutException {
        ReceiveMessageResult result = sqsClient.receiveMessage(
            new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(1)
                .withVisibilityTimeout(visibilityTimeout));
        if(result.getMessages() != null && result.getMessages().size() > 0) {
            Message msg = result.getMessages().get(0);
            Task task = marshallTask(msg.getBody());
            return task;
        } else {
            throw new TimeoutException("No tasks available from queue: " + queueUrl);
        }
    }

    public void extendVisibilityTimeout(Task task, Integer seconds) throws TaskNotFoundException {
        try {
            sqsClient.changeMessageVisibility(new ChangeMessageVisibilityRequest()
                                                  .withQueueUrl(queueUrl)
                                                  .withReceiptHandle(task.getProperty(MsgProp.RECEIPT_HANDLE.toString()))
                                                  .withVisibilityTimeout(seconds));
        } catch(ReceiptHandleIsInvalidException rhe) {
            throw new TaskNotFoundException(rhe);
        }
    }

    public void deleteTask(Task task) throws TaskNotFoundException {
        try {
            sqsClient.deleteMessage(new DeleteMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withReceiptHandle(
                        task.getProperty(MsgProp.RECEIPT_HANDLE.name())));
        } catch(ReceiptHandleIsInvalidException rhe) {
            throw new TaskNotFoundException(rhe);
        }
    }
}
