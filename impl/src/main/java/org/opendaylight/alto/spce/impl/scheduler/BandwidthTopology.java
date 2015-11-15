/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.scheduler;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.NetworkTrackerService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BandwidthTopology {
    private long BandwidthMap[][];

    public BandwidthTopology(Topology topology, NetworkTrackerService networkTrackerService) {
        int nodeNum = topology.getNode().toArray().length;
        Map<NodeId, Integer> tpIdMap = new HashMap<>();
        for (int i = 0; i < nodeNum; i++) {
            NodeId nodeId = topology.getNode().get(i).getNodeId();
            tpIdMap.put(nodeId, i);
        }
        this.BandwidthMap = new long[nodeNum][nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                this.BandwidthMap[i][j] = 0;
            }
        }
        for (Link link : topology.getLink()) {
            int src = tpIdMap.get(link.getSource().getSourceNode());
            int dst = tpIdMap.get(link.getDestination().getDestNode());
            this.BandwidthMap[src][dst] = getBandwidthByTp(link.getSource().getSourceTp(), networkTrackerService);
        }
    }

    // Construct Function for Test
    public BandwidthTopology(long bandwidthMap[][]) {
        this.BandwidthMap = bandwidthMap.clone();
    }

    public long getBandwidth(int src, int dst) {
        return this.BandwidthMap[src][dst];
    }

    public void setBandwidth(int src, int dst, long bandwidth) {
        this.BandwidthMap[src][dst] = bandwidth;
    }

    public int getTopologySize() {return this.BandwidthMap.length;}

    private long getBandwidthByTp(TpId txTpId, NetworkTrackerService networkTrackerService) {
        long availableBandwidth = 0;
        AltoSpceGetTxBandwidthInput input = new AltoSpceGetTxBandwidthInputBuilder().setTpId(txTpId.getValue()).build();
        Future<RpcResult<AltoSpceGetTxBandwidthOutput>> result = networkTrackerService.altoSpceGetTxBandwidth(input);
        try {
            AltoSpceGetTxBandwidthOutput output = result.get().getResult();
            availableBandwidth = output.getSpeed().longValue();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
        return availableBandwidth;
    }
}

