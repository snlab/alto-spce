/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.util;

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

        import static org.opendaylight.controller.md.sal.binding.impl.BindingDOMMountPointServiceAdapter.LOG;

public class BandwidthTopology {
    private long BandwidthMap[][];
    private Map<NodeId, Integer> tpIdMap = new HashMap<>();
    private int nodeNum = 0;
    private int switchNum = 0;

    public String getTpIdMap() {
        StringBuffer result = new StringBuffer("{");
        for (NodeId key: tpIdMap.keySet()) {
            LOG.info(key.getValue() + ":" + tpIdMap.get(key));
            result.append('\"' + key.getValue() + '\"' + ':' + tpIdMap.get(key).toString() + ',');
        }
        result.deleteCharAt(result.lastIndexOf(","));
        result.append('}');
        return result.toString();
    }
    public String getBandwidthMap() {
        StringBuffer result = new StringBuffer("[");
        for (int i = 0; i < switchNum; ++i) {
            result.append('[');
            for (int j = 0; j < switchNum; ++j) {
                result.append(BandwidthMap[i][j]);
                result.append(',');
            }
            result.deleteCharAt(result.lastIndexOf(","));
            result.append(']');
            result.append(',');
        }
        result.deleteCharAt(result.lastIndexOf(","));
        result.append(']');
        return result.toString();
    }

    public BandwidthTopology(Topology topology, NetworkTrackerService networkTrackerService) {
        nodeNum = topology.getNode().toArray().length;
        //Map<NodeId, Integer> tpIdMap = new HashMap<>();
        int switchIndex = 0;
        for (int i = 0; i < nodeNum; i++) {
            NodeId nodeId = topology.getNode().get(i).getNodeId();
            if (!(nodeId.getValue().contains("host"))) {
                tpIdMap.put(nodeId, switchIndex);
                ++switchNum;
                ++switchIndex;
            }
        }
        this.BandwidthMap = new long[switchNum][switchNum];
        for (int i = 0; i < switchNum; i++) {
            for (int j = 0; j < switchNum; j++) {
                this.BandwidthMap[i][j] = 0;
            }
        }
        for (Link link : topology.getLink()) {
            Integer src = tpIdMap.get(link.getSource().getSourceNode());
            Integer dst = tpIdMap.get(link.getDestination().getDestNode());
            if (null != src && null != dst)
                this.BandwidthMap[src][dst] = getBandwidthByTp(link.getSource().getSourceTp(), networkTrackerService);
        }
    }

    // Construct Function for Test
    public BandwidthTopology(long bandwidthMap[][]) {
        this.BandwidthMap = bandwidthMap.clone();
    }

    public void setResidualBandwidthTopology(long usedBandwidthMap[][]) {
        for (int src=0; src<this.BandwidthMap.length; src++)
            for (int dst=0; dst<this.BandwidthMap.length; dst++)
                this.BandwidthMap[src][dst] -= usedBandwidthMap[src][dst];
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
