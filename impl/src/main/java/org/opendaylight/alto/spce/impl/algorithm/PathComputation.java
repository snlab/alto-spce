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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.alto.spce.impl.algorithm.helper.DisjointSet;
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

public class PathComputation {

    private NetworkTrackerService networkTrackerService;

    public PathComputation(NetworkTrackerService networkTrackerService) {
        this.networkTrackerService = networkTrackerService;
    }

    private static class Path {
        public TpId src;
        public TpId dst;
        public long bandwidth;

        public Path() {}

        public Path(TpId src, TpId dst, long bandwidth) {
            this.src = src;
            this.dst = dst;
            this.bandwidth = bandwidth;
        }
    }

    public Graph<String, Path> createGraphFromTopology(Topology topology) {
        Graph<String, Path> graph = new SparseMultigraph<String, Path>();
        for (Node node: topology.getNode()) {
            graph.addVertex(node.getNodeId().getValue());
        }

        for (Link link: topology.getLink()) {
            String srcNode = link.getSource().getSourceNode().getValue();
            String dstNode = link.getDestination().getDestNode().getValue();

            Path path;

            TpId srcId = link.getSource().getSourceTp();
            TpId dstId = link.getDestination().getDestTp();

            path = new Path(srcId, dstId, getBandwidth(srcId, dstId));
            graph.addEdge(path, srcNode, dstNode, EdgeType.DIRECTED);

            //swap src and dst
            path = new Path(dstId, srcId, getBandwidth(dstId, srcId));
            graph.addEdge(path, dstNode, srcNode, EdgeType.DIRECTED);
        }
        return graph;
    }

    public List<TpId> findPathFromGraph(String src, String dst, Graph<String, Path> graph) {
        DijkstraShortestPath<String, Path> shortestPath = new DijkstraShortestPath<>(graph);

        List<TpId> output = new LinkedList<>();
        for (Path eachPath : shortestPath.getPath(src, dst)) {
            output.add(eachPath.src);
        }
        return output;
     }

    public List<TpId> shortestPath(TpId srcTpId, TpId dstTpId, Topology topology, List<ConstraintMetric> constraintMetrics) {
        String src = extractNodeId(srcTpId);
        String dst = extractNodeId(dstTpId);

        Graph<String, Path> graph = createGraphFromTopology(topology);
        return findPathFromGraph(src, dst, graph);
    }

    protected Graph<String, Path> findMaxBwGraph(String src, String dst, Graph<String, Path> origin) {
        Graph<String, Path> graph = new SparseMultigraph<String, Path>();

        ArrayList<Path> edges = new ArrayList<>(origin.getEdges());
        Collections.sort(edges, new Comparator<Path>() {
            @Override
            public int compare(Path x, Path y) {
                return (x.bandwidth == y.bandwidth ? 0 : (x.bandwidth > y.bandwidth ? -1 : 1));
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null)
                    return false;
                return (this.getClass().equals(obj.getClass()));
            }
        });

        DisjointSet<String> set = new DisjointSet<>();
        long bandwidth = -1;
        for (Path path: edges) {
            if (!set.disjointed(src, dst)) {
                if (path.bandwidth != bandwidth)
                    break;
            }
            bandwidth = path.bandwidth;

            String p1 = extractNodeId(path.src);
            String p2 = extractNodeId(path.dst);

            set.merge(p1, p2);
            graph.addEdge(path, p1, p2, EdgeType.DIRECTED);
        }

        if (set.disjointed(src, dst)) {
            // No path from src to dst
            return null;
        }

        return graph;
    }

    public List<TpId> maxBandwidthPath(TpId srcTpId, TpId dstTpId, Topology topology, List<ConstraintMetric> constraintMetrics) {
        String src = extractNodeId(srcTpId);
        String dst = extractNodeId(dstTpId);

        Graph<String, Path> graph = createGraphFromTopology(topology);
        Graph<String, Path> bwGraph = findMaxBwGraph(src, dst, graph);

        if (bwGraph == null) {
            //TODO: handle error here
            return null;
        }

        return findPathFromGraph(src, dst, bwGraph);
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

    private long getBandwidth(TpId src, TpId dst) {
        //TODO: how to compute the bandwidth?
        return 1000;
    }

    public static String extractNodeId(String nodeConnectorId) {
        return nodeConnectorId.replaceAll(":[0-9]+$", "");
    }

    public static String extractNodeId(TpId tpId) {
        return extractNodeId(tpId.getValue());
    }

}
