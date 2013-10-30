/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.noop;

import org.duracloud.mill.credentials.CredentialsRepo;
import org.duracloud.mill.domain.Task;
import org.duracloud.mill.workman.TaskProcessor;
import org.duracloud.mill.workman.TaskProcessorFactoryBase;

/**
 * A processor factory for a noop task.
 * 
 * @author Daniel Bernstein 
 *         Date: Oct 24, 2013
 */
public class NoopTaskProcessorFactory extends TaskProcessorFactoryBase {
    /**
     * @param credentialRepo
     */
    public NoopTaskProcessorFactory(CredentialsRepo credentialRepo) {
        super(credentialRepo);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.workman.TaskProcessorFactoryBase#createImpl(org.duracloud
     * .mill.domain.Task)
     */
    @Override
    protected TaskProcessor createImpl(Task task) {
        return new NoopTaskProcessor(task);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.mill.workman.TaskProcessorFactoryBase#isSupported(org.duracloud
     * .mill.domain.Task)
     */
    @Override
    protected boolean isSupported(Task task) {
        return task.getType().equals(Task.Type.NOOP);
    }
}
