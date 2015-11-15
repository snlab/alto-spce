/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.alto.spce.impl.scheduler;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by qiao on 11/14/15.
 */
public class OMFRA {

    private boolean FindResidualPath() {
        boolean exitflag = false;

        return exitflag;
    }

    private boolean FindPath(BandwidthTopology topology, int src, int dst) {
        boolean exitflag = false;

        int numVertex = topology.getTopologySize();
        boolean[] visited = new boolean[numVertex];
        Arrays.fill(visited, false);

        LinkedList<Integer> vertice = new LinkedList<Integer>();
        //Queue vertice = new LinkedList();
        vertice.add(src);

        while (!vertice.isEmpty()) {
            for (int i=0; i<numVertex; i++) {
                if ((topology.getBandwidth(vertice.getFirst(), i) >0)
                        && (vertice.getFirst() != i)
                        && (!visited[i])) {
                    visited[i]=true;
                    vertice.add(i);
                    if (i==dst) {
                        exitflag = true;
                        return exitflag;
                    }
                }
            }
            vertice.remove();
        }

        return exitflag;
    }

    private long[] FeasibleSolution () {return }
}
