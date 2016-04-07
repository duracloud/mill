/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.impl;

import org.duracloud.mill.credentials.CredentialsRepo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
/**
 * 
 * @author Daniel Bernstein
 *
 */
public class ApplicationContextLocator {
    private static ApplicationContext ctx;
    
    static {
        ctx = new AnnotationConfigApplicationContext("org.duracloud.mill.db.repo",
                                                     "org.duracloud.account.db.repo",
                                                     "org.duracloud.account.db.config",
                                                     "org.duracloud.mill.credentials.impl");
        
    }
    public static CredentialsRepo getCredentialsRepo(){
        return ctx.getBean(CredentialsRepo.class);

    }

    public static ApplicationContext get(){
        return ctx;

    }

}
