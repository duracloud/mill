/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.durastore;

import org.duracloud.storage.aop.ContentMessage;

/**
 * A message listener interface used by all listeners of durastore messages.
 * @author Daniel Bernstein
 *	       Date: Jan 7, 2014
 */
public interface MessageListener {
    public void onMessage(ContentMessage message);
}
