/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.algorithm;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.alto.spce.setup.input.ConstraintMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.NetworkTrackerService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PathComputation {

    private NetworkTrackerService networkTrackerService;
    private static final Logger logger = LoggerFactory.getLogger(PathComputation.class);
    public PathComputation(NetworkTrackerService networkTrackerService) {
        this.networkTrackerService = networkTrackerService;
    }

    public class Path {
        TpId src;
        TpId dst;
        Long bandwidth;

        @Override
        public String toString() {
            return "" + src + "->" + dst + "@" + bandwidth;
        }
    }

    public List<TpId> shortestPath(TpId srcTpId, TpId dstTpId, Topology topology, List<ConstraintMetric> constraintMetrics) {
        String src = srcTpId.getValue();
        String dst = dstTpId.getValue();
        Graph<String, Path> networkGraph = getGraphFromTopology(topology);
        DijkstraShortestPath<String, Path> shortestPath = new DijkstraShortestPath<>(networkGraph);
        List<Path> path = shortestPath.getPath(extractNodeId(src), extractNodeId(dst));
        List<TpId> output = new LinkedList<>();
        for (Path eachPath : path) {
            output.add(eachPath.src);
        }
        return output;
    }

    public List<TpId> maxBandwidthPath(TpId srcTpId, TpId dstTpId, Topology topology, List<ConstraintMetric> constraintMetrics) {
        String src = srcTpId.getValue();
        String dst = dstTpId.getValue();
        Graph<String, Path> networkGraph = getGraphFromTopology(topology);
        List<Path> path = maxBandwidth(networkGraph, extractNodeId(src), extractNodeId(dst));
        List<TpId> output = new LinkedList<>();
        for (Path eachPath : path) {
            output.add(eachPath.src);
        }
        return output;
    }

    public List<Path> maxBandwidth(Graph<String, Path> networkGraph, String src, String dst) {
        LinkedList<String> queue = new LinkedList<>();
        Map<String, Long> maxBw = new HashMap<>();
        Map<String, Path> pre = new HashMap<>();
        queue.addLast(src);
        maxBw.put(src, Long.MAX_VALUE);
        while (!queue.isEmpty()) {
            String now = queue.pop();
            Long provideBw = maxBw.get(now);
            if (networkGraph.getOutEdges(now) == null)
                continue;
            for (Path egressPath : networkGraph.getOutEdges(now)) {
                Long bw = (egressPath.bandwidth < provideBw) ? egressPath.bandwidth : provideBw;
                String dstNode = extractNodeId(egressPath.dst.getValue());
                if (maxBw.containsKey(dstNode)) {
                    Long currentBw = maxBw.get(dstNode);
                    if (bw > currentBw) {
                        maxBw.put(dstNode, bw);
                        if (!queue.contains(dstNode))
                            queue.addLast(dstNode);
                        pre.put(dstNode, egressPath);
                    }
                } else {
                    maxBw.put(dstNode, bw);
                    if(!queue.contains(dstNode))
                    {
                        queue.addLast(dstNode);
                    }
                    pre.put(dstNode, egressPath);
                }
            }
        }
        List<Path> output = new LinkedList<>();
        output.add(0, pre.get(dst));
        while (!extractNodeId(output.get(0).src.getValue()).equals(src)) {
            dst = extractNodeId(output.get(0).src.getValue());
            output.add(0, pre.get(dst));
        }
        return output;
    }

    private Graph<String, PathComputation.Path> getGraphFromTopology(Topology topology) {
        Graph<String, Path> networkGraph = new SparseMultigraph();
        for (Node eachNode : topology.getNode()) {
            networkGraph.addVertex(eachNode.getNodeId().getValue());
        }
        for (Link eachLink : topology.getLink()) {
            String linkSrcNode = extractNodeId(eachLink.getSource().getSourceNode().getValue());
            String linkDstNode = extractNodeId(eachLink.getDestination().getDestNode().getValue());
            if (linkSrcNode.contains("host") || linkDstNode.contains("host")) {
                continue;
            }
            TpId linkSrcTp = eachLink.getSource().getSourceTp();
            TpId linkDstTp = eachLink.getDestination().getDestTp();
            Path srcPath = new Path();
            srcPath.src = linkSrcTp;
            srcPath.dst = linkDstTp;
            srcPath.bandwidth = getBandwidthByTp(srcPath.src.getValue()).longValue();
            networkGraph.addEdge(srcPath, linkSrcNode, linkDstNode, EdgeType.DIRECTED);
        }
        return networkGraph;
    }

    private BigInteger getBandwidthByTp(String txTpId) {
        BigInteger availableBandwidth = null;
        AltoSpceGetTxBandwidthInput input = new AltoSpceGetTxBandwidthInputBuilder().setTpId(txTpId).build();
        Future<RpcResult<AltoSpceGetTxBandwidthOutput>> result = this.networkTrackerService.altoSpceGetTxBandwidth(input);
        try {
            AltoSpceGetTxBandwidthOutput output = result.get().getResult();
            availableBandwidth = output.getSpeed();
        } catch (InterruptedException | ExecutionException e) {
            return BigInteger.valueOf(0);
        }
        return availableBandwidth;
    }

    public static String extractNodeId(String nodeConnectorId) {
        String output =
            nodeConnectorId.split(":")[0] + ":" + nodeConnectorId.split(":")[1];
        return output;
    }

}
