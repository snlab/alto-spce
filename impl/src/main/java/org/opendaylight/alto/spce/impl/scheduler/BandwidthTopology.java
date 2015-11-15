/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.scheduler;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

public class BandwidthTopology {
    private long BandwidthMap[][];

    public BandwidthTopology(Topology topology) {
        this.BandwidthMap = null;
    }

    public long getBandwidth(int src, int dst) {
        return this.BandwidthMap[src][dst];
    }

    public void setBandwidth(int src, int dst, long bandwidth) {
        this.BandwidthMap[src][dst] = bandwidth;
    }

    //public long[][] getTopology() {return this.BandwidthMap;}

    public int getTopologySize() {return this.BandwidthMap.length;}
}

