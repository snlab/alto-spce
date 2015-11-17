/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.scheduler;

import java.util.List;

public class DataTransferFlow {
    private int kSeq;
    private int source;
    private List<Integer> path;
    private long minBandwidth;
    private long maxBandwidth;
    private boolean status;

    public DataTransferFlow(int kSeq, int source, List<Integer> path,
                            long minBandwidth, long maxBandwidth, boolean status) {
        this.kSeq = kSeq;
        this.source = source;
        this.path = path;
        this.minBandwidth = minBandwidth;
        this.maxBandwidth = maxBandwidth;
        this.status = status;
    }

    public int getkSeq() {
        return this.kSeq;
    }

    public int getSource() {
        return this.source;
    }

    public List<Integer> getPath() {
        return this.path;
    }

    public long getMinBandwidth() { return this.minBandwidth; }

    public long getMaxBandwidth() { return this.maxBandwidth; }

    public boolean getFlowStatus() { return this.status; }


}
