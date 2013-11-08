/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.storage.aop.ContentMessage.ACTION;
import org.duracloud.storage.aop.ContentMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.util.ErrorHandler;


/**
 * This class is responsible for managing the lifecycles of the message listener
 * containers associated with each of the subdomains.
 * 
 * @author Daniel Bernstein Date: Oct 30, 2013
 */
public class MessageListenerContainerManager {
    private static Logger log = LoggerFactory
            .getLogger(MessageListenerContainerManager.class);

    public static final String DEFAULT_CONNECTION_FACTORY_TEMPLATE = "tcp://{0}.duracloud.org:61617";

    private List<Map<String, SimpleMessageListenerContainer>> containers;

    private DuplicationPolicyManager duplicationPolicyManager;
    private TaskQueue duplicationTaskQueue;
    private String connectionFactoryURLTemplate;

    public MessageListenerContainerManager(TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager,
            String connectionFactoryURLTemplate) {
        this.duplicationPolicyManager = duplicationPolicyManager;
        this.duplicationTaskQueue = duplicationTaskQueue;
        this.connectionFactoryURLTemplate = connectionFactoryURLTemplate;
    }

    public MessageListenerContainerManager(TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager) {
        this(duplicationTaskQueue, duplicationPolicyManager,
                DEFAULT_CONNECTION_FACTORY_TEMPLATE);
    }

    public void init() {
        initializeMessageContainers();
        start();
    }
    
    private class MessageListenerErrorHandler implements ExceptionListener, ErrorHandler {

        @Override
        public void onException(JMSException ex) {
            log.error(ex.getMessage(), ex);
        }

        @Override
        public void handleError(Throwable t) {
            log.error(t.getMessage(), t);
        }
    }

    private void initializeMessageContainers() {
        log.info("initializing message containers...");
        ACTION[] actions = { 
                ACTION.DELETE, 
                ACTION.COPY, 
                ACTION.INGEST,
                ACTION.UPDATE };

        ContentMessageConverter converter = new ContentMessageConverter();
        MessageListenerErrorHandler errorHandler = new MessageListenerErrorHandler();
        containers = new ArrayList<>();
        for (String subdomain : duplicationPolicyManager
                .getDuplicationAccounts()) {
            ConnectionFactory connectionFactory = jmsFactory(subdomain);

            ContentMessageListener listener = new ContentMessageListener(
                    duplicationTaskQueue, duplicationPolicyManager, subdomain);

            // add a container for each topic
            for (ACTION action : actions) {
                SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
                container.setAutoStartup(false);
                container.setExceptionListener(errorHandler);
                container.setErrorHandler(errorHandler);
                container.setConnectionFactory(connectionFactory);
                String destination = "org.duracloud.topic.change.content."
                        + action.name().toLowerCase();
                container.setDestination(new ActiveMQTopic(destination));
                MessageListenerAdapter adapter = new MessageListenerAdapter(
                        listener);
                adapter.setDefaultListenerMethod("onMessage");
                adapter.setMessageConverter(converter);
                log.info(
                        "created message listener container for subdomain {}: "
                                + "destination: {}, connectionFactory: {}, converter:{}",
                        subdomain, destination, connectionFactory, converter);
                container.setMessageListener(adapter);
                Map<String, SimpleMessageListenerContainer> map = new HashMap<>();
                map.put(subdomain, container);
                containers.add(map);
            }
        }

        log.info("message containers initialized.");

    }

    private ConnectionFactory jmsFactory(String subdomain) {
        String url = MessageFormat.format(connectionFactoryURLTemplate,
                subdomain);
        log.info("creating connection factory for {}", url);
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL("failover:"+ url);
        PooledConnectionFactory pooledFactory = new PooledConnectionFactory(factory);
        return pooledFactory;
    }

    private void start() {
        for (Map<String, SimpleMessageListenerContainer> map : containers) {
            for (Map.Entry<String, SimpleMessageListenerContainer> e : map
                    .entrySet()) {
                SimpleMessageListenerContainer container = e.getValue();
                container.start();
                log.info(
                        "started container subscribed to {} on connection {} on subdomain: {}",
                        container.getDestination(),
                        container.getConnectionFactory(), e.getKey());
            }
        }
    }

    public void destroy() {
        for (Map<String, SimpleMessageListenerContainer> map : containers) {
            for (Map.Entry<String, SimpleMessageListenerContainer> e : map
                    .entrySet()) {
                SimpleMessageListenerContainer container = e.getValue();
                container.stop();
                log.info(
                        "stopped container subscribed to {} on connection {} on subdomain: {}",
                        container.getDestination(),
                        container.getConnectionFactory(), e.getKey());
            }
        }
    }

}
