/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.notification;

/**
 * @author Daniel Bernstein
 * Date: Dec 31, 2013
 */
public interface NotificationManager {

    /**
     * Send a generic notification to the mill's admin
     *
     * @param subject message subject
     * @param body    message body
     */
    void sendEmail(String subject, String body);

}
