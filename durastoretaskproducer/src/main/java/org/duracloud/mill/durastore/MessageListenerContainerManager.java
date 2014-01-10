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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.notification.NotificationManager;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.storage.aop.ContentMessage.ACTION;
import org.duracloud.storage.aop.ContentMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.listener.SimpleMessageListenerContainer;


/**
 * This class is responsible for managing the lifecycles of the message listener
 * containers associated with each of the subdomains.
 * 
 * @author Daniel Bernstein 
 *         Date: Oct 30, 2013
 */
public class MessageListenerContainerManager {
    private static Logger log = LoggerFactory
            .getLogger(MessageListenerContainerManager.class);

    public static final String DEFAULT_CONNECTION_FACTORY_TEMPLATE = 
                                            "tcp://{0}.duracloud.org:61617";

    private static final ACTION[] ACTIONS = {   ACTION.DELETE, 
                                                ACTION.COPY, 
                                                ACTION.INGEST,
                                                ACTION.UPDATE };

    public static final long DEFAULT_POLICY_UPDATE_FREQUENCY_MS = 5*60*1000;
    
    private Map<String,List<SimpleMessageListenerContainer>> containers;

    private DuplicationPolicyManager duplicationPolicyManager;
    private TaskQueue duplicationTaskQueue;
    private String connectionFactoryURLTemplate;

    private NotificationManager notificationManager;

    private ContentMessageConverter converter;

    private MessageListenerErrorHandler errorHandler;
    
    private MessageListenerContainerFactory messageListenerContainerFactory;
    private Timer timer;

    private long policyUpdateFrequencyMs = DEFAULT_POLICY_UPDATE_FREQUENCY_MS;

    public MessageListenerContainerManager(TaskQueue duplicationTaskQueue,
                                           DuplicationPolicyManager duplicationPolicyManager,
                                           String connectionFactoryURLTemplate, 
                                           NotificationManager notificationManager,
                                           MessageListenerContainerFactory messageListenerContainerFactory,
                                           long policyUpdateFrequencyMs) {
        this.duplicationPolicyManager = duplicationPolicyManager;
        this.duplicationTaskQueue = duplicationTaskQueue;
        this.connectionFactoryURLTemplate = connectionFactoryURLTemplate;
        this.notificationManager = notificationManager;
        this.converter = new ContentMessageConverter();
        this.errorHandler = new MessageListenerErrorHandler();
        this.messageListenerContainerFactory = messageListenerContainerFactory;
        this.containers = new HashMap<>();
        this.policyUpdateFrequencyMs = policyUpdateFrequencyMs;
        this.timer = new Timer();
    }
    
    public MessageListenerContainerManager(TaskQueue duplicationTaskQueue,
                                           DuplicationPolicyManager duplicationPolicyManager, 
                                           NotificationManager notificationManager,
                                           MessageListenerContainerFactory messageListenerContainerFactory,
                                           long policyUpdateFrequencyMs) {
        this(duplicationTaskQueue, 
             duplicationPolicyManager,
             DEFAULT_CONNECTION_FACTORY_TEMPLATE, 
             notificationManager,
             messageListenerContainerFactory,
             policyUpdateFrequencyMs);
    }
    
    

    public void init() {
        initializeMessageContainers();
    }
    

    private void initializeMessageContainers() {
        log.info("initializing message containers...");
        updateMessageListenerContainers();
        log.info("message containers initialized.");
        
        timer.schedule(new TimerTask(){
            /* (non-Javadoc)
             * @see java.util.TimerTask#run()
             */
            @Override
            public void run() {
                updateMessageListenerContainers();
            }
        }, policyUpdateFrequencyMs, policyUpdateFrequencyMs);
    }

    private synchronized void updateMessageListenerContainers() {

        //ensure you have the latest policies
        this.duplicationPolicyManager.clearPolicyCache();
        
        Set<String> currentPolicies = this.duplicationPolicyManager.getDuplicationAccounts();
        Set<String> newSubdomains = new HashSet<>(currentPolicies);

        newSubdomains.removeAll(this.containers.keySet());

        if(newSubdomains.size() > 0){
            log.info("adding new subdomains: {}", 
                     ArrayUtils.toString(newSubdomains.toArray()));
        }
        
        for (String subdomain : newSubdomains) {
            attachListenersToSubdomain(subdomain);
        }

        Set<String> removedSubdomains = new HashSet<>(this.containers.keySet());
        removedSubdomains.removeAll(currentPolicies);

        if(removedSubdomains.size() > 0){
            log.info("removed subdomains: {}", 
                     ArrayUtils.toString(removedSubdomains.toArray()));
        }

        for (String subdomain : removedSubdomains) {
            stopContainers(subdomain, 
                           this.containers.remove(subdomain));
        }
    }

    /**
     * @param subdomain
     */
    private void attachListenersToSubdomain(String subdomain) {
        
        ConnectionFactory connectionFactory = jmsFactory(subdomain);

        ContentMessageListener contentListener = new ContentMessageListener(
                duplicationTaskQueue, duplicationPolicyManager, subdomain);

        SpaceDeleteMessageListener spaceDeleteListener = 
                new SpaceDeleteMessageListener(duplicationTaskQueue, 
                                               duplicationPolicyManager, 
                                               subdomain);

        SpaceCreateMessageListener spaceCreateListener = 
                new SpaceCreateMessageListener(subdomain, 
                                               notificationManager);
        // add a container for each topic
        for (ACTION action : ACTIONS) {
            String destination = "org.duracloud.topic.change.content."
                    + action.name().toLowerCase();    
            addListener(converter, 
                        errorHandler, 
                        subdomain,
                        connectionFactory, 
                        contentListener, 
                        destination);
        }
        
        addListener(converter, 
                    errorHandler, 
                    subdomain,
                    connectionFactory, 
                    spaceCreateListener, 
                    "org.duracloud.topic.change.space.create");

        addListener(converter, 
                    errorHandler, 
                    subdomain,
                    connectionFactory, 
                    spaceDeleteListener, 
                    "org.duracloud.topic.change.space.delete");
        
        startContainers(subdomain, this.containers.get(subdomain));
    }

    /**
     * @param converter
     * @param errorHandler
     * @param subdomain
     * @param connectionFactory
     * @param listener
     * @param destination
     */
    private void addListener(ContentMessageConverter converter,
            MessageListenerErrorHandler errorHandler,
            String subdomain,
            ConnectionFactory connectionFactory,
            MessageListener listener,
            String destination) {
        SimpleMessageListenerContainer container = 
                this.messageListenerContainerFactory.create(converter, 
                                                   errorHandler, 
                                                   subdomain, 
                                                   connectionFactory,
                                                   listener, 
                                                   destination);
        List<SimpleMessageListenerContainer> containerList = containers.get(subdomain);
        if(containerList == null){
            containerList = new ArrayList<SimpleMessageListenerContainer>();
            containers.put(subdomain, containerList);
        }
        containerList.add(container);
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

    private void startContainers(String subdomain, 
                                 List<SimpleMessageListenerContainer> containerList) {
        for (SimpleMessageListenerContainer container : containerList){
            if(!container.isRunning()) {
                container.start();
                log.info(
                        "started container subscribed to {} on connection {} on subdomain: {}",
                        container.getDestination(),
                        container.getConnectionFactory(), subdomain);
            }
        }
    }

    public void destroy() {
        for(String subdomain : containers.keySet()){
            stopContainers(subdomain,containers.get(subdomain));
        }
        timer.cancel();
    }
    
    private synchronized void stopContainers(String subdomain, 
                                             List<SimpleMessageListenerContainer> containerList){
        for (SimpleMessageListenerContainer container : containerList) {
                if(container.isRunning()){
                    container.stop();
                    container.shutdown();
                    log.info(
                            "stopped container subscribed to {} on connection {} on subdomain: {}",
                            container.getDestination(),
                            container.getConnectionFactory(), subdomain);
                }
        }
        containerList.clear();
    }
}
