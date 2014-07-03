package org.duracloud.mill.credentials.impl;

import org.duracloud.mill.credentials.CredentialsRepo;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CredentialsRepoLocator {
    public static CredentialsRepo get(){
        System.setProperty("db.user", "ama");
        System.setProperty("db.pass", "ama");
        System.setProperty("db.name", "ama");
        System.setProperty("db.host", "localhost");
        System.setProperty("db.port", "3306");
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jpa-config.xml");
        return ctx.getBean(CredentialsRepo.class);

    }
}
