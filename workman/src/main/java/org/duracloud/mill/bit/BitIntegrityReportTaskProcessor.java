/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.bit;

import java.text.MessageFormat;

import org.duracloud.mill.bitlog.BitLogStore;
import org.duracloud.mill.workman.TaskExecutionFailedException;
import org.duracloud.mill.workman.TaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Bernstein
 *	       Date: May 7, 2014
 */
public class BitIntegrityReportTaskProcessor implements TaskProcessor {
    private static Logger log = 
            LoggerFactory.getLogger(BitIntegrityReportTaskProcessor.class);
    private BitIntegrityCheckReportTask task;
    private BitLogStore bitLogStore;
    
    /**
     * @param bitTask
     * @param bitLogStore
     */
    public BitIntegrityReportTaskProcessor(BitIntegrityCheckReportTask task,
            BitLogStore bitLogStore) {
        this.task = task;
        this.bitLogStore = bitLogStore;
    }

    /* (non-Javadoc)
     * @see org.duracloud.mill.workman.TaskProcessor#execute()
     */
    @Override
    public void execute() throws TaskExecutionFailedException {
        log.info(MessageFormat.format("Not currently implemented: {0} not actually processed.",this.task));
    }

}
