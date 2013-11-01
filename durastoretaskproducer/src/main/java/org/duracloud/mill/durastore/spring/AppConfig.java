/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore.spring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.duracloud.mill.dup.DuplicationPolicy;
import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.durastore.DurastoreTaskProducerConfigurationManager;
import org.duracloud.mill.durastore.MessageListenerContainerManager;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.aws.SQSTaskQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Daniel Bernstein
 *	       Date: Oct 30, 2013
 */
@Configuration
public class AppConfig {

    @Bean(initMethod="init")
    public DurastoreTaskProducerConfigurationManager configurationManager(){
        return new DurastoreTaskProducerConfigurationManager();
    }

    @Bean 
    public TaskQueue duplicationTaskQueue (DurastoreTaskProducerConfigurationManager configurationManager) {
        return new SQSTaskQueue(configurationManager.getDuplicationQueueName());
    }
    
    @Bean
    public DuplicationPolicyManager duplicationPolicyManager(DurastoreTaskProducerConfigurationManager configurationManager){
        String file = configurationManager.getDuplicationPolicyFile();
        if(file != null){
            //return new DuplicationPolicyManager(file);
        }

        DuplicationPolicyManager policy = new DuplicationPolicyManager(){
          /* (non-Javadoc)
             * @see org.duracloud.mill.dup.DuplicationPolicyManager#getDuplicationAccounts()
             */
            @Override
            public Set<String> getDuplicationAccounts() {
                return new HashSet<String>(Arrays.asList(System.getProperty("config.domain")));
            }  
            
            /* (non-Javadoc)
             * @see org.duracloud.mill.dup.DuplicationPolicyManager#getDuplicationPolicy(java.lang.String)
             */
            @Override
            public DuplicationPolicy getDuplicationPolicy(String account) {
                DuplicationPolicy policy = new DuplicationPolicy();
                DuplicationStorePolicy dupStore = new DuplicationStorePolicy();
                dupStore.setSrcStoreId(System.getProperty("config.primaryStoreId"));
                dupStore.setDestStoreId(System.getProperty("config.secondaryStoreId"));
                policy.addDuplicationStorePolicy(System.getProperty("config.space"), dupStore);
                return policy;
            }
        };

        return policy; 
        

    }
    
    @Bean(initMethod="init", destroyMethod="destroy")
    public MessageListenerContainerManager messageListenerContainerManager(
            TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager) {
        return new MessageListenerContainerManager(duplicationTaskQueue,
                                                   duplicationPolicyManager);
    }
}
