package org.duracloud.mill.workman.spring;

import org.duracloud.mill.credential.CredentialRepo;
import org.duracloud.mill.credential.simpledb.SimpleDBCredentialRepo;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.queue.TaskNotFoundException;
import org.duracloud.mill.queue.TaskQueue;
import org.duracloud.mill.queue.TimeoutException;
import org.duracloud.mill.workman.RootTaskProcessorFactory;
import org.duracloud.mill.workman.TaskWorkerFactoryImpl;
import org.duracloud.mill.workman.TaskWorkerManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = { "org.duracloud.mill" })
@Configuration
public class AppConfig {
    
    @Bean
    public RootTaskProcessorFactory rootTaskProcessorFactory() {
        return new RootTaskProcessorFactory();
    }

    @Bean
    public CredentialRepo credentialRepo() {
        return new SimpleDBCredentialRepo();
    }

    @Bean(initMethod="init")
    public TaskWorkerManager taskWorkerManager(
            RootTaskProcessorFactory factory, TaskQueue taskQueue) {
        return new TaskWorkerManager(new TaskWorkerFactoryImpl(taskQueue,
                factory));
    }
    
    @Bean 
    public TaskQueue taskQueue(){
        return new TaskQueue () {

            @Override
            public void put(Task task) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public Task take() throws TimeoutException {
                throw new TimeoutException();
            }

            @Override
            public long getDefaultVisibilityTimeout() {
                // TODO Auto-generated method stub
                return 10000;
            }

            @Override
            public void extendVisibilityTimeout(Task task, long milliseconds)
                    throws TaskNotFoundException {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void deleteTask(Task task) {
                // TODO Auto-generated method stub
                
            }
            
        };
    }
}
