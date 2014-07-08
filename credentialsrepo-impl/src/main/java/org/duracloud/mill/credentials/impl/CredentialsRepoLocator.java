/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.impl;

import org.duracloud.mill.credentials.CredentialsRepo;
import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
 * 
 * @author Daniel Bernstein
 *
 */
public class CredentialsRepoLocator {
    public static CredentialsRepo get(){
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jpa-config.xml");
        return ctx.getBean(CredentialsRepo.class);

    }
}
