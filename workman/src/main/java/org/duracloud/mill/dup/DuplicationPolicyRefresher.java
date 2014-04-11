/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.dup;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Daniel Bernstein
 *	       Date: Apr 11, 2014
 */
public class DuplicationPolicyRefresher {
    private Timer timer;
    private Long policyManagerRefreshFrequencyMs;
    private DuplicationPolicyManager policyManager;
    /**
     * @param policyManagerRefreshFrequencyMs
     */
    public DuplicationPolicyRefresher(Long policyManagerRefreshFrequencyMs, DuplicationPolicyManager policyManager) {
        this.policyManagerRefreshFrequencyMs = policyManagerRefreshFrequencyMs;
        this.policyManager = policyManager;
    }

    public void init() {
        timer = new Timer();
        timer.schedule(new TimerTask(){
            /* (non-Javadoc)
             * @see java.util.TimerTask#run()
             */
            @Override
            public void run() {
                policyManager.clearPolicyCache();
            }
        }, policyManagerRefreshFrequencyMs, policyManagerRefreshFrequencyMs);        
    }
    
    public void destroy(){
        timer.cancel();
    }
}
