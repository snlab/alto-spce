/*
 * Copyright © 2015 Copyright (c) 2015 SNLAB and others. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;
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
        return null;
    }

    private BigInteger getBandwidthByTp(String txTpId) {
        BigInteger availableBandwidth = null;
        AltoSpceGetTxBandwidthInput input = new AltoSpceGetTxBandwidthInputBuilder().setTpId(txTpId).build();
        Future<RpcResult<AltoSpceGetTxBandwidthOutput>> result = this.networkTrackerService.altoSpceGetTxBandwidth(input);
        try {
            AltoSpceGetTxBandwidthOutput output = result.get().getResult();
            availableBandwidth = output.getSpeed();
        } catch (InterruptedException | ExecutionException e) {
        }
        return availableBandwidth;
    }

    public static String extractNodeId(String nodeConnectorId) {
        return nodeConnectorId.replaceAll(":[0-9]+$", "");
    }

}
