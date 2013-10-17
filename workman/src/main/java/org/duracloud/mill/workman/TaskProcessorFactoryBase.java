/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.mill.credential.CredentialRepo;
import org.duracloud.mill.domain.Task;

/**
 * An abstract base class for building TaskProcessor factories that require
 * access to credentials.
 * 
 * @author Daniel Bernstein
 * 
 */
public abstract class TaskProcessorFactoryBase implements TaskProcessorFactory {

    private CredentialRepo credentialRepo;

    public TaskProcessorFactoryBase(CredentialRepo credentialRepo) {
        this.credentialRepo = credentialRepo;
    }

    @Override
    public final TaskProcessor create(Task task)
            throws TaskNotSupportedException {
        if (!isSupported(task)) {
            throw new TaskNotSupportedException(task + " is not supported");
        }

        return createImpl(task);
    }

    protected CredentialRepo getCredentialRepo() {
        return credentialRepo;
    }

    protected abstract boolean isSupported(Task task);

    protected abstract TaskProcessor createImpl(Task task);
}
