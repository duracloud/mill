/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.impl;

import java.text.MessageFormat;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.duracloud.mill.util.PropertyFileHelper;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.dialect.MySQL5Dialect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
@Configuration
@EnableJpaRepositories(basePackages = { "org.duracloud.account.db" },
                       entityManagerFactoryRef = CredentialsRepoConfig.ENTITY_MANAGER_FACTORY_BEAN,
                       transactionManagerRef = CredentialsRepoConfig.TRANSACTION_MANAGER_BEAN)
public class CredentialsRepoConfig {
    private static final String CONFIG_PROPERTIES_PATH = "config.properties";
    public static final String ENTITY_MANAGER_FACTORY_BEAN = "credentialsRepoEntityManagerFactory";
    public static final String CREDENTIALS_REPO_DATA_SOURCE_BEAN = "credentialsRepoDataSource";
    public static final String TRANSACTION_MANAGER_BEAN = "credentialsJpaRepoTransactionManager";

    static {
        String defaultPath = "/" + System.getProperty("user.home")
                + "/duracloud-mc/mc-config.properties";
        PropertyFileHelper.loadFromSystemProperty(CONFIG_PROPERTIES_PATH,
                                                  defaultPath);
        new MCJpaPropertiesVerifier().verify();

    }

    @Bean(name = CREDENTIALS_REPO_DATA_SOURCE_BEAN, destroyMethod = "close")
    public BasicDataSource credentialsRepoDataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl(MessageFormat
                .format("jdbc:mysql://{0}:{1}/{2}?autoReconnect=true",
                        System.getProperty("db.host", "localhost"),
                        System.getProperty("db.port", "3306"),
                        System.getProperty("db.name", "mill")));
        dataSource.setUsername(System.getProperty("db.user", "mill"));
        dataSource.setPassword(System.getProperty("db.pass", "password"));

        return dataSource;
    }

    @Bean(name = TRANSACTION_MANAGER_BEAN)
    public PlatformTransactionManager
            credentialsRepoTransactionManager(@Qualifier(ENTITY_MANAGER_FACTORY_BEAN) EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager tm = new JpaTransactionManager(entityManagerFactory);
        tm.setJpaDialect(new HibernateJpaDialect());
        return tm;
    }

    @Bean(name = ENTITY_MANAGER_FACTORY_BEAN)
    public LocalContainerEntityManagerFactoryBean
            credentialsRepoEntityManagerFactory(@Qualifier(CREDENTIALS_REPO_DATA_SOURCE_BEAN) DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPersistenceUnitName("credentials-repo-pu");
        emf.setPackagesToScan("org.duracloud.account.db.model");

        String hbm2ddlAuto = System.getProperty("hibernate.hbm2ddl.auto");

        HibernateJpaVendorAdapter va = new HibernateJpaVendorAdapter();
        va.setGenerateDdl(hbm2ddlAuto != null);
        va.setDatabase(Database.MYSQL);
        emf.setJpaVendorAdapter(va);

        Properties props = new Properties();
        if (hbm2ddlAuto != null) {
            props.setProperty("hibernate.hbm2ddl.auto", hbm2ddlAuto);
        }
        props.setProperty("hibernate.dialect",
                          MySQL5Dialect.class.getName());
        props.setProperty("hibernate.ejb.naming_strategy",
                          ImprovedNamingStrategy.class.getName());
        props.setProperty("hibernate.cache.provider_class",
                          "org.hibernate.cache.HashtableCacheProvider");
        props.setProperty("jadira.usertype.autoRegisterUserTypes", "true");
        props.setProperty("jadira.usertype.databaseZone", "jvm");
        props.setProperty("hibernate.show_sql", "false");
        props.setProperty("hibernate.format_sql", "false");
        props.setProperty("hibernate.show_comments", "false");
        emf.setJpaProperties(props);
        return emf;
    }



}
