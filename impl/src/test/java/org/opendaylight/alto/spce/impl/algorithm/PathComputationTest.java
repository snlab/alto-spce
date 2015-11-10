/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.algorithm;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

import java.util.List;

public class PathComputationTest {
    private PathComputation pathComputer;

    @Before
    public void prepare() {
        pathComputer = new PathComputation(null);
    }

    @Test
    public void onTestMaxBandwidth() {
        Graph<String, PathComputation.Path> networkGraph = new SparseMultigraph<>();
        for (int i = 0; i < 5; ++i) {
            networkGraph.addVertex("openflow:"+i);
        }
        addEdge(networkGraph, getTp(0, 0), getTp(1, 0), (long) 5);
        addEdge(networkGraph, getTp(1, 1), getTp(2, 0), (long) 1);
        addEdge(networkGraph, getTp(1, 2), getTp(4, 0), (long) 2);
        addEdge(networkGraph, getTp(4, 1), getTp(3, 2), (long) 3);
        addEdge(networkGraph, getTp(2, 1), getTp(3, 0), (long) 8);
        addEdge(networkGraph, getTp(3, 1), getTp(5, 1), (long) 5);
        List<PathComputation.Path> output
                = pathComputer.maxBandwidth(networkGraph, getNode(0), getNode(5));
        System.out.println(output);
    }

    private <T> String getTp(T i, T j) {
        return "openflow:" + i + ":" + j;
    }

    private <T> String getNode(T i) {
        return "openflow:" + i;
    }

    private void addEdge (Graph<String, PathComputation.Path> networkGraph,
                          String src, String dst, Long bw) {
        PathComputation.Path p = pathComputer.new Path();
        p.src = TpId.getDefaultInstance(src);
        p.dst = TpId.getDefaultInstance(dst);
        p.bandwidth = bw;
        networkGraph.addEdge(p, PathComputation.extractNodeId(src), PathComputation.extractNodeId(dst),
                EdgeType.DIRECTED);
        p = pathComputer.new Path();
        p.src = TpId.getDefaultInstance(dst);
        p.dst = TpId.getDefaultInstance(src);
        p.bandwidth = bw;
        networkGraph.addEdge(p, PathComputation.extractNodeId(dst), PathComputation.extractNodeId(src),
                EdgeType.DIRECTED);
    }

}
