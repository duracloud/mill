/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import javax.jms.ConnectionFactory;

import org.apache.activemq.command.ActiveMQTopic;
import org.duracloud.storage.aop.ContentMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

/**
 * A factory class responsible for creating new message listener containers.
 * @author Daniel Bernstein
 *	       Date: Jan 10, 2014
 */
public class MessageListenerContainerFactory {
    private static Logger log = LoggerFactory
            .getLogger(MessageListenerContainerFactory.class);

    /**
     * Creates a new message listener container.
     * @param converter
     * @param errorHandler
     * @param subdomain
     * @param connectionFactory
     * @param listener
     * @param destination
     * @return
     */
    public SimpleMessageListenerContainer create(ContentMessageConverter converter,
            MessageListenerErrorHandler errorHandler,
            String subdomain,
            ConnectionFactory connectionFactory,
            MessageListener listener,
            String destination) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setAutoStartup(false);
        container.setExceptionListener(errorHandler);
        container.setErrorHandler(errorHandler);
        container.setConnectionFactory(connectionFactory);
        container.setDestination(new ActiveMQTopic(destination));
        MessageListenerAdapter adapter = new MessageListenerAdapter(listener);
        adapter.setDefaultListenerMethod("onMessage");
        adapter.setMessageConverter(converter);
        log.info("created message listener container for subdomain {}: "
                + "destination: {}, connectionFactory: {}, converter:{}",
                subdomain, destination, connectionFactory, converter);
        container.setMessageListener(adapter);
        return container;
    }
}
