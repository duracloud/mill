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
import java.util.List;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.storage.aop.ContentMessage.ACTION;
import org.duracloud.storage.aop.ContentMessageConverter;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

/**
 * This class is responsible for managing the lifecycles of the message listener
 * containers associated with each of the subdomains.
 * 
 * @author Daniel Bernstein Date: Oct 30, 2013
 */
public class MessageListenerContainerManager {

    private static final String DEFAULT_CONNECTION_FACTORY_TEMPLATE = "tcp://{0}.duracloud.org:61617";

    private List<DefaultMessageListenerContainer> containers;

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

    private void initializeMessageContainers() {
        ACTION[] actions = {ACTION.DELETE, ACTION.COPY, ACTION.INGEST, ACTION.UPDATE};

        ContentMessageConverter converter = new ContentMessageConverter();
        containers = new ArrayList<>();
        for (String subdomain : duplicationPolicyManager
                .getDuplicationAccounts()) {
            ConnectionFactory connectionFactory = jmsFactory(subdomain);
            
            
            ContentMessageListener listener = 
                    new ContentMessageListener(duplicationTaskQueue, 
                                               duplicationPolicyManager, 
                                               subdomain);
            
            //add a container for each topic
            for(ACTION action : actions){
                DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
                container.setConnectionFactory(connectionFactory);
                String destination = "org.duracloud.topic.change.content."
                        + action.name().toLowerCase();
                container.setDestination(new ActiveMQTopic(destination));
                MessageListenerAdapter adapter = new MessageListenerAdapter(listener);
                adapter.setDefaultListenerMethod("onMessage");
                adapter.setMessageConverter(converter);
                containers.add(container);
            }
        }
    }

    private ConnectionFactory jmsFactory(String subdomain) {
        String url = MessageFormat.format(connectionFactoryURLTemplate,
                subdomain);
        return new PooledConnectionFactory(new ActiveMQConnectionFactory(url));
    }

    private void start() {
        for (DefaultMessageListenerContainer container : containers) {
            container.start();
        }
    }

    public void destroy() {
        for (DefaultMessageListenerContainer container : containers) {
            container.stop();
        }
    }

}
