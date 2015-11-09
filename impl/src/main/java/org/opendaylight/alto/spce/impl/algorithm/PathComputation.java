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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PathComputation {

    private NetworkTrackerService networkTrackerService;

    public PathComputation(NetworkTrackerService networkTrackerService) {
        this.networkTrackerService = networkTrackerService;
    }

    private class Path {
        TpId src;
        TpId dst;
        Long bandwidth;
    }

    public List<TpId> shortestPath(TpId srcTpId, TpId dstTpId, Topology topology, List<ConstraintMetric> constraintMetrics) {
        String src = srcTpId.getValue();
        String dst = dstTpId.getValue();
        Graph<String, PathComputation.Path> networkGraph = new SparseMultigraph();
        for (Node eachNode : topology.getNode()) {
            networkGraph.addVertex(eachNode.getNodeId().getValue());
        }
        for (Link eachLink : topology.getLink()) {
            String linkSrcNode = eachLink.getSource().getSourceNode().getValue();
            String linkDstNode = eachLink.getDestination().getDestNode().getValue();
            TpId linkSrcTp = eachLink.getSource().getSourceTp();
            TpId linkDstTp = eachLink.getDestination().getDestTp();
            Path srcPath = new Path();
            srcPath.src = linkSrcTp;
            srcPath.dst = linkDstTp;
            networkGraph.addEdge(srcPath, linkSrcNode, linkDstNode, EdgeType.DIRECTED);

            Path dstPath = new Path();
            dstPath.src = linkDstTp;
            dstPath.dst = linkSrcTp;
            networkGraph.addEdge(dstPath, linkDstNode, linkSrcNode, EdgeType.DIRECTED);
        }
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
        Graph<String, PathComputation.Path> networkGraph = new SparseMultigraph();
        for (Node eachNode : topology.getNode()) {
            networkGraph.addVertex(eachNode.getNodeId().getValue());
        }
        for (Link eachLink : topology.getLink()) {
            String linkSrcNode = eachLink.getSource().getSourceNode().getValue();
            String linkDstNode = eachLink.getDestination().getDestNode().getValue();
            TpId linkSrcTp = eachLink.getSource().getSourceTp();
            TpId linkDstTp = eachLink.getDestination().getDestTp();
            Path srcPath = new Path();
            srcPath.src = linkSrcTp;
            srcPath.dst = linkDstTp;
            srcPath.bandwidth = getBandwidthByTp(srcPath.src.getValue()).longValue();

            networkGraph.addEdge(srcPath, linkSrcNode, linkDstNode, EdgeType.DIRECTED);

            Path dstPath = new Path();
            dstPath.src = linkDstTp;
            dstPath.dst = linkSrcTp;
            dstPath.bandwidth = getBandwidthByTp(dstPath.src.getValue()).longValue();
            networkGraph.addEdge(dstPath, linkDstNode, linkSrcNode, EdgeType.DIRECTED);
        }
        List<Path> path = maxBandwidth(networkGraph, extractNodeId(src), extractNodeId(dst));
        List<TpId> output = new LinkedList<>();
        for (Path eachPath : path) {
            output.add(eachPath.src);
        }
        return output;
    }

    private List<Path> maxBandwidth(Graph<String, Path> networkGraph, String src, String dst) {
        LinkedList<String> queue = new LinkedList<>();
        Map<String, Long> maxBw = new HashMap<>();
        Map<String, Path> pre = new HashMap<>();
        queue.addLast(src);
        maxBw.put(src, Long.MAX_VALUE);
        while (!queue.isEmpty()) {
            String now = queue.pop();
            Long provideBw = maxBw.get(now);
            for (Path egressPath : networkGraph.getOutEdges(now)) {
                Long bw = (egressPath.bandwidth < provideBw) ? egressPath.bandwidth : provideBw;
                String dstNode = egressPath.dst.getValue();
                if (maxBw.containsKey(dstNode)) {
                    Long currentBw = maxBw.get(dstNode);
                    if (bw > currentBw) {
                        maxBw.put(dstNode, bw);
                        queue.addLast(dstNode);
                        pre.put(dstNode, egressPath);
                    }
                } else {
                    maxBw.put(dstNode, bw);
                    queue.addLast(dstNode);
                    pre.put(dstNode, egressPath);
                }
            }
        }
        List<Path> output = new LinkedList<>();
        output.add(0, pre.get(dst));
        while (!output.get(0).src.getValue().equals(src)) {
            dst = output.get(0).src.getValue();
            output.add(0, pre.get(dst));
        }
        return output;
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
        return nodeConnectorId.replaceAll(":[0-9]+$", "");
    }

}
