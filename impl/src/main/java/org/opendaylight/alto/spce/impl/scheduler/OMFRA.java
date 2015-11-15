/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.alto.spce.impl.scheduler;

import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;

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

    private long[] FeasibleSolution () { //TODO
        long [] list = new long [2];
        return list;
    }

    public double MMFSolver(BandwidthTopology topology, DataTransferRequest[] unsatDataTransferRequests, DataTransferRequest[] satDataTransferRequests, DataTransferFlow[] flow, double[] zSat) { //TODO::handle Routing

        //List of variables : fij || rk || z
        int numVertex = topology.getTopologySize();
        int numFlow = flow.length;
        int numUnsat = unsatDataTransferRequests.length;
        int numSat = satDataTransferRequests.length;
        int numVariables = numVertex * numVertex * numFlow + numFlow + 1;

        double[] c = new double[numVariables];
        for (int i = 0; i < c.length; i++) c[i] = 0.;

        int posZ = c.length - 1; // postion of variable z
        c[posZ] = -1.;

        //inequalities constraints
        double[][] G = new double[numVertex * numVertex + numUnsat][numVariables];
        double[] h = new double[numVertex * numVertex + numUnsat];
        for (int i = 0; i < numVertex; i++) {
            h[i] = 0;
            for (int j = 0; j < numVariables; j++) G[i][j] = 0;
        }

        int current = 0; // edit constraints at current line
        //Build coefficient matrix for link capacity
        for (int i = 0; i < numVertex; i++)
            for (int j = 0; j < numVertex; j++) {
                current++;

                for (int k = 0; k < numFlow; k++)
                    G[current][k * numVertex * numVertex + i * numVertex + j] = 1.;

                h[current] = topology.getBandwidth(i, j);
            }

        //Build coefficient matrix for unsaturated requests
        for (int i = 0; i < numUnsat; i++) {
            current++;

            for (int j = 0; j < numFlow; j++)
                if (flow[j].getmSeq() == unsatDataTransferRequests[i].getmSeq())
                    G[current][((int) (numVertex * numVertex * numFlow + flow[j].getkSeq()))] = -1.;

            G[current][posZ] = unsatDataTransferRequests[i].getVolume();
        }


        //equalities constraints
        double[][] A = new double[numVertex * numFlow + numSat][numVariables];
        double[] b = new double[numVertex * numFlow + numSat];
        for (int i = 0; i < numVertex * numFlow + numSat; i++) {
            b[i] = 0;
            for (int j = 0; j < numVariables; j++) A[i][j] = 0;
        }

        //Build coefficient matrix for flow conservation
        current = 0;
        for (int k = 0; k < numFlow; k++)
            for (int i = 0; i < numVertex; i++) {
                current++;

                for (int j = 0; j < numVertex; j++) {
                    if (topology.getBandwidth(i, j) > 0)
                        A[current][k * numVertex * numVertex + i * numVertex + j] = 1.;

                    if (topology.getBandwidth(j, i) > 0)
                        A[current][k * numVertex * numVertex + j * numVertex + i] = -1.;
                }

                if (i == flow[k].getSource()) {
                    A[current][numVertex * numVertex * numFlow + k] = -1.;
                } else {
                    A[current][numVertex * numVertex + numFlow + k] = (i == flow[k].getDestination()) ? 1. : 0;
                }
            }

        //Build coefficient matrix for saturated requests
        for (int i = 0; i < numSat; i++) {
            current++;

            for (int j = 0; j < numFlow; j++) {
                if (flow[j].getmSeq() == satDataTransferRequests[i].getmSeq())
                    A[current][((int) (numVertex * numVertex * numFlow + flow[j].getkSeq()))] = 1.; //TODO::flow.getkSeq() is long now, but I think it should be int
            }

            b[current] = satDataTransferRequests[i].getVolume() * zSat[i];
        }

        //Bounds on variables
        double[] lb = new double[numVariables];
        //double[] ub;
        for (int i = 0; i < numVariables; i++) lb[i] = 0;

        //optimization problem
        LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(G);
        or.setH(h);
        or.setA(A);
        or.setB(b);
        or.setLb(lb);
        //or.setUb(ub);
        or.setDumpProblem(true);

        //optimization
        LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        try {
            int returnCode = opt.optimize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        double[] sol = opt.getOptimizationResponse().getSolution();

        return sol[posZ];
    }
}
