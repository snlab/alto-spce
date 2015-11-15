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
import java.util.List;

public class OMFRA {

    private static final boolean DEBUG = false;

    /*File slicing mode*/
    private static final boolean FILE_SLICING_DISABLED = false;
    private static final boolean FILE_SLICING_ENABLED = true;

    /*Path selection mode*/
    private static final boolean PATH_SELECTION_DISABLED = false;
    private static final boolean PATH_SELECTION_ENABLED = true;

    /*Replica selection mode, only needed if file slicing is disabled*/
    private static final int MINHOP_REPLICA = 0;
    private static final int ENUM_REPLICA = 1;
    private static final int HERUISTIC_REPLICA = 2;
    //TODO: use enum

    /*private enum ReplicaSelection {
        MINHOP_REPLICA, ENUM_REPLICA, HERUISTIC_REPLICA
    }
    */

    /*Scheduling mode*/
    private static final int ONLINE_OMFRA = 0;
    private static final int OFFLINE_OMFRA = 1;

    private boolean fileSlicingOption;
    private boolean pathSelectionOption;
    private int replicaSelectionMode;
    private int schedulingMode;

    public OMFRA(boolean fileSlicingOption, boolean pathSelectionOption,
                 int replicaSelectionMode, int schedulingMode) {
        this.fileSlicingOption = fileSlicingOption;
        this.pathSelectionOption = pathSelectionOption;
        this.replicaSelectionMode = replicaSelectionMode;
        this.schedulingMode = schedulingMode;
    }

    public boolean getFileSlicingOption() { return this.fileSlicingOption; }
    public boolean getPathSelectionOption() { return this.pathSelectionOption; }
    public int getReplicaSelectionMode() { return this.replicaSelectionMode; }
    public int getSchedulingMode() { return this.schedulingMode; }

    public void enableFileSlicing() {
        this.fileSlicingOption = FILE_SLICING_ENABLED;
    }

    public void disableFileSlicing() {
        this.fileSlicingOption = FILE_SLICING_DISABLED;
    }

    public void enablePathSelection() {
        this.pathSelectionOption = PATH_SELECTION_ENABLED;
    }

    public void disablePathSelection() {
        this.pathSelectionOption = PATH_SELECTION_DISABLED;
    }

    public boolean setReplicationSelectionMode(int replicationSelectionMode) {
        switch (replicationSelectionMode) {
            case MINHOP_REPLICA:
                this.replicaSelectionMode = MINHOP_REPLICA;
                return true;
            case ENUM_REPLICA:
                this.replicaSelectionMode = ENUM_REPLICA;
                return true;
            case HERUISTIC_REPLICA:
                this.replicaSelectionMode = HERUISTIC_REPLICA;
                return true;
            default:
                return false;
        }
    }

    public boolean setSchedulingMode(int schedulingMode) {
        switch (schedulingMode) {
            case ONLINE_OMFRA:
                this.schedulingMode = ONLINE_OMFRA;
                return true;
            case OFFLINE_OMFRA:
                this.schedulingMode = OFFLINE_OMFRA;
                return true;
            default:
                return false;
        }
    }

    public OMFRAAllocPolicy[] onlineScheduler(BandwidthTopology topology,
                                            DataTransferRequest[] request,
                                            int newRequestIdx,
                                            DataTransferFlow[] flow, 
                                            int newFlowIdx) {
        int num_vertex = topology.getTopologySize();
        int num_flow = flow.length;
        int num_request = request.length;
        OMFRAAllocPolicy AllocPolicy[];


        return new OMFRAAllocPolicy()[1]; //TODO
    }

    private OMFRAAllocPolicy OMFRA_Core(BandwidthTopology topology,
                                        DataTransferRequest[] request,
                                        DataTransferFlow[] flow) {
        int num_vertex = topology.getTopologySize();
        int num_flow = flow.length;
        int num_request = request.length;

        return new OMFRAAllocPolicy(); //TODO
    }

    private boolean FindResidualPath(BandwidthTopology tmpTopology,
                                     DataTransferRequest request,
                                     DataTransferFlow[] flow) {
        int num_flow = flow.length;

        for (int k=0; k<num_flow; k++) {
            if (flow[k].getPath().isEmpty()) {//Flow path is not fixed
                if (FindPath(tmpTopology, flow[k].getSource(), request.getDestination())) {
                    return true;
                }
            }
            else {//Flow path is fixed
                List<Integer> path = flow[k].getPath();
                boolean found = true;
                for (int i=0; i<path.size()-1; i++) {
                    if (tmpTopology.getBandwidth(path.get(i), path.get(i+1)) == 0) {
                        found = false;
                        break;
                    }
                    else if (tmpTopology.getBandwidth(path.get(i), path.get(i+1)) < flow[i].getMinBandwidth()) {
                        found = false;
                        break;
                    }
                }
                if (found)
                    return true;
            }
        }

        return false;
    }

    private boolean FindPath(BandwidthTopology topology, int src, int dst) {
        boolean exitflag = false;

        int numVertex = topology.getTopologySize();
        boolean[] visited = new boolean[numVertex];
        Arrays.fill(visited, false);

        LinkedList<Integer> vertices = new LinkedList<Integer>();
        vertices.add(src);

        while (!vertices.isEmpty()) {
            for (int i=0; i<numVertex; i++) {
                if ((topology.getBandwidth(vertices.getFirst(), i) >0)
                        && (vertices.getFirst() != i)
                        && (!visited[i])) {
                    visited[i]=true;
                    vertices.add(i);
                    if (i==dst) {
                        exitflag = true;
                        return exitflag;
                    }
                }
            }
            vertices.remove();
        }

        return exitflag;
    }

    private long[] FeasibleSolution () { //TODO
        long [] list = new long [2];
        return list;
    }

    public double [] MMFSolver(BandwidthTopology topology, DataTransferRequest[] unsatDataTransferRequests, DataTransferRequest[] satDataTransferRequests, DataTransferFlow[] flow, double[] zSat) { //TODO::handle Routing

        //List of variables : fij || rk || z
        int numVertex = topology.getTopologySize();
        int numFlow = flow.length;
        int numUnsat = unsatDataTransferRequests.length;
        int numSat = satDataTransferRequests.length;
        int numVariables = numVertex * numVertex * numFlow + numFlow + 1;

        int numExp = numVariables;
        double[] c = new double[numExp];
        for (int i = 0; i < numExp; i++) c[i] = 0.;

        int posZ = numExp - 1; // position of variable z
        c[posZ] = -1.;

        //inequalities constraints
        numExp = numVertex * numVertex + numUnsat + numUnsat + numSat + 2 * numFlow * numVertex * numVertex;
        double[][] G = new double[numExp][numVariables];
        double[] h = new double[numExp];
        for (int i = 0; i < numExp; i++) {
            h[i] = 0;
            for (int j = 0; j < numVariables; j++) G[i][j] = 0;
        }

        int current = 0; // edit constraints at current line
        //Build coefficient matrix for link capacity
        for (int i = 0; i < numVertex; i++)
            for (int j = 0; j < numVertex; j++) {
                for (int k = 0; k < numFlow; k++)
                    G[current][k * numVertex * numVertex + i * numVertex + j] = 1.;

                h[current++] = topology.getBandwidth(i, j);
            }

        //Build coefficient matrix for unsaturated requests
        for (int i = 0; i < numUnsat; i++) {
            for (int j = 0; j < numFlow; j++)
                if (flow[j].getmSeq() == unsatDataTransferRequests[i].getmSeq())
                    G[current][numVertex * numVertex * numFlow + flow[j].getkSeq()] = -1.;

            G[current++][posZ] = unsatDataTransferRequests[i].getVolume();
        }

        //Build coefficient matrix for (4h)
        for (int i = 0; i < numSat; i++) {
            for (int j = 0; j < numFlow; j++)
                if (flow[j].getmSeq() == satDataTransferRequests[i].getmSeq())
                    G[current][numVertex * numVertex * numFlow + flow[j].getkSeq()] = 1.;

            h[current++] = satDataTransferRequests[i].getVolume();
        }

        for (int i = 0; i < numUnsat; i++) {
            for (int j = 0; j < numFlow; j++)
                if (flow[j].getmSeq() == unsatDataTransferRequests[i].getmSeq())
                    G[current][numVertex * numVertex * numFlow + flow[j].getkSeq()] = 1.;

            h[current++] =  unsatDataTransferRequests[i].getVolume();
        }

        //Build coefficient matrix for flow path constraint
        for (int k = 0; k < numFlow; k++) {
            for (int i = 0; i < numVertex; i++)
                for (int j = 0; j < numVertex; j++) {
                    G[current + k * numVertex * numVertex + i * numVertex + j][k * numVertex * numVertex + i * numVertex +j] = 1.;

                    G[current + numFlow * numVertex * numVertex + k * numVertex * numVertex + i * numVertex + j][k * numVertex * numVertex + i * numVertex +j] = -1.;
                }

            List<Integer> list = flow[k].getPath();

            for (int p = 0; p < list.size() - 1; p++) {
                int i = list.get(p), j = list.get(p + 1);
                h[current + k * numVertex * numVertex + i * numVertex + j] = flow[k].getMaxBandwidth();

                h[current + numFlow * numVertex * numVertex + k * numVertex * numVertex + i * numVertex + j] = -flow[k].getMinBandwidth();
            }
        }

        assert(current + 2 * numFlow * numVertex * numVertex == numExp);

        //equalities constraints
        numExp = numVertex * numFlow + numSat;
        double[][] A = new double[numExp][numVariables];
        double[] b = new double[numExp];
        for (int i = 0; i < numExp; i++) {
            b[i] = 0;
            for (int j = 0; j < numVariables; j++) A[i][j] = 0;
        }

        //Build coefficient matrix for flow conservation
        current = 0;
        for (int k = 0; k < numFlow; k++)
            for (int i = 0; i < numVertex; i++) {
                for (int j = 0; j < numVertex; j++) {
                    if (topology.getBandwidth(i, j) > 0)
                        A[current][k * numVertex * numVertex + i * numVertex + j] = 1.;

                    if (topology.getBandwidth(j, i) > 0)
                        A[current][k * numVertex * numVertex + j * numVertex + i] = -1.;
                }

                if (i == flow[k].getSource()) {
                    A[current++][numVertex * numVertex * numFlow + k] = -1.;
                } else {
                    A[current++][numVertex * numVertex + numFlow + k] = (i == flow[k].getDestination()) ? 1. : 0;
                }

            }

        //Build coefficient matrix for saturated requests
        for (int i = 0; i < numSat; i++) {
            for (int j = 0; j < numFlow; j++) {
                if (flow[j].getmSeq() == satDataTransferRequests[i].getmSeq())
                    A[current][numVertex * numVertex * numFlow + flow[j].getkSeq()] = 1.;
            }

            b[current++] = satDataTransferRequests[i].getVolume() * zSat[i];
        }

        assert(current == numExp);

        //Bounds on variables
        numExp = numVariables;
        double[] lb = new double[numExp];
        //double[] ub;
        for (int i = 0; i < numExp; i++) lb[i] = 0;

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

        return opt.getOptimizationResponse().getSolution();
    }
}
