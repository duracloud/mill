/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ErrorHandler;

/**
 * A simple error handler for messages received from DuraCloud.
 * @author Daniel Bernstein 
 *         Date: Jan 10, 2014
 */
public class MessageListenerErrorHandler implements ExceptionListener,
        ErrorHandler {
    private static Logger log = LoggerFactory
                                      .getLogger(MessageListenerErrorHandler.class);

    @Override
    public void onException(JMSException ex) {
        log.error(ex.getMessage(), ex);
    }

    @Override
    public void handleError(Throwable t) {
        log.error(t.getMessage(), t);
    }
}
