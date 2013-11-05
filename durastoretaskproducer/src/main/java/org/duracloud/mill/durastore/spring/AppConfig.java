/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore.spring;

import org.duracloud.mill.dup.DuplicationPolicyManager;
import org.duracloud.mill.dup.repo.DuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.LocalDuplicationPolicyRepo;
import org.duracloud.mill.dup.repo.S3DuplicationPolicyRepo;
import org.duracloud.mill.durastore.DurastoreTaskProducerConfigurationManager;
import org.duracloud.mill.durastore.MessageListenerContainerManager;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.aws.SQSTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Daniel Bernstein
 *	       Date: Oct 30, 2013
 */
@Configuration
public class AppConfig {
    private static Logger log = LoggerFactory.getLogger(AppConfig.class);
    @Bean(initMethod="init")
    public DurastoreTaskProducerConfigurationManager configurationManager(){
        return new DurastoreTaskProducerConfigurationManager();
    }

    @Bean 
    public TaskQueue duplicationTaskQueue (
        DurastoreTaskProducerConfigurationManager configurationManager) {
        return new SQSTaskQueue(configurationManager.getDuplicationQueueName());
    }
    
    @Bean
    public DuplicationPolicyManager duplicationPolicyManager(
        DurastoreTaskProducerConfigurationManager configurationManager){
        String dupPolicyDir = configurationManager.getDuplicationPolicyDir();
        DuplicationPolicyRepo policyRepo;
        if(null != dupPolicyDir) {
            policyRepo = new LocalDuplicationPolicyRepo(dupPolicyDir);
        } else {
            policyRepo = new S3DuplicationPolicyRepo();
        }
        return new DuplicationPolicyManager(policyRepo);
    }
    
    @Bean(initMethod="init", destroyMethod="destroy")
    public MessageListenerContainerManager messageListenerContainerManager(
            TaskQueue duplicationTaskQueue,
            DuplicationPolicyManager duplicationPolicyManager, DurastoreTaskProducerConfigurationManager configurationManager) {
         
        String template = configurationManager.getJMSConnectionUrlTemplate();
        if(template != null){
            log.info("using jms connection url template {}", template);
            return new MessageListenerContainerManager(duplicationTaskQueue,
                    duplicationPolicyManager, 
                    template);
        }else{
            log.info("Using default jms connection url template...");
            return new MessageListenerContainerManager(duplicationTaskQueue,
                    duplicationPolicyManager);
        }
    }
}
