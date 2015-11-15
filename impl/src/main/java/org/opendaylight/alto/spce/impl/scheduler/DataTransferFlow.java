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
    private int mSeq;
    private int source;
    private int destination;
    private long volume;
    private List<Integer> path;
    private long minBandwidth;
    private long maxBandwidth;

    public DataTransferFlow(int kSeq, int mSeq, int source, int destination, long volume, List<Integer> path, long minBandwidth, long maxBandwidth) {
        this.kSeq = kSeq;
        this.mSeq = mSeq;
        this.source = source;
        this.destination = destination;
        this.volume = volume;
        this.path = path;
        this.minBandwidth = minBandwidth;
        this.maxBandwidth = maxBandwidth;
    }

    public int getkSeq() {
        return this.kSeq;
    }

    public int getmSeq() {
        return this.mSeq;
    }

    public int getSource() {
        return this.source;
    }

    public int getDestination() {
        return this.destination;
    }

    public long getVolume() {
        return this.volume;
    }

    public List<Integer> getPath() {
        return this.path;
    }

    public long getMinBandwidth() { return this.minBandwidth;}

    public long getMaxBandwidth() { return this.maxBandwidth;}
}
