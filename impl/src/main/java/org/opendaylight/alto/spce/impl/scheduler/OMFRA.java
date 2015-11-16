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
        this.fileSlicingOption = ConfigurationOptions.FILE_SLICING_ENABLED;
    }

    public void disableFileSlicing() {
        this.fileSlicingOption = ConfigurationOptions.FILE_SLICING_DISABLED;
    }

    public void enablePathSelection() {
        this.pathSelectionOption = ConfigurationOptions.PATH_SELECTION_ENABLED;
    }

    public void disablePathSelection() {
        this.pathSelectionOption = ConfigurationOptions.PATH_SELECTION_DISABLED;
    }

    public boolean setReplicationSelectionMode(int replicationSelectionMode) {
        switch (replicationSelectionMode) {
            case ConfigurationOptions.MINHOP_REPLICA:
                this.replicaSelectionMode = ConfigurationOptions.MINHOP_REPLICA;
                return true;
            case ConfigurationOptions.ENUM_REPLICA:
                this.replicaSelectionMode = ConfigurationOptions.ENUM_REPLICA;
                return true;
            case ConfigurationOptions.HERUISTIC_REPLICA:
                this.replicaSelectionMode = ConfigurationOptions.HERUISTIC_REPLICA;
                return true;
            default:
                return false;
        }
    }

    public boolean setSchedulingMode(int schedulingMode) {
        switch (schedulingMode) {
            case ConfigurationOptions.ONLINE_OMFRA:
                this.schedulingMode = ConfigurationOptions.ONLINE_OMFRA;
                return true;
            case ConfigurationOptions.OFFLINE_OMFRA:
                this.schedulingMode = ConfigurationOptions.OFFLINE_OMFRA;
                return true;
            default:
                return false;
        }
    }

    public List<OMFRAAllocPolicy> Scheduler(BandwidthTopology topology,
                                            List<DataTransferRequest> request,
                                            int newRequestIdx,
                                            List<DataTransferFlow> flow,
                                            int newFlowIdx) {
        int num_vertex = topology.getTopologySize();
        int num_flow = flow.size();
        int num_request = request.size();
        List<OMFRAAllocPolicy> AllocPolicy = new LinkedList<OMFRAAllocPolicy>();
        //AllocPolicy.add(new OMFRAAllocPolicy(num_vertex, num_flow));

        if (!this.fileSlicingOption) {
            List<DataTransferFlow> chosenNewFlow =
                    replicaSelector(topology, request, newRequestIdx, flow, newFlowIdx);
            //TODO: update flow before scheduling
        }

        switch (this.schedulingMode) {
            case ConfigurationOptions.ONLINE_OMFRA:
                AllocPolicy = OMFRA_Core(topology, request, flow);
                return AllocPolicy;
            case ConfigurationOptions.OFFLINE_OMFRA:
                AllocPolicy = OMFRA_Offline(topology, request, flow);
                return AllocPolicy;
        }
        //TODO: catch exception needed??
        return AllocPolicy;
    }

    public List<DataTransferFlow> replicaSelector(BandwidthTopology topology,
                                                   List<DataTransferRequest> request,
                                                   int newRequestIdx,
                                                   List<DataTransferFlow> flow,
                                                   int newFlowIdx) {
        int num_vertex = topology.getTopologySize();
        int num_flow = flow.size();
        int num_request = request.size();

        List<DataTransferFlow> chosenNewFlow = new LinkedList<DataTransferFlow>();

        switch (this.replicaSelectionMode) {
            case ConfigurationOptions.MINHOP_REPLICA:
                chosenNewFlow =
                        minHopReplica(flow, newFlowIdx);
                return chosenNewFlow;
            case ConfigurationOptions.ENUM_REPLICA:
                chosenNewFlow =
                        enumReplica(topology, request, newRequestIdx, flow, newFlowIdx);
                return chosenNewFlow;
            case ConfigurationOptions.HERUISTIC_REPLICA:
                chosenNewFlow =
                        heruisticReplica(topology, request, newRequestIdx, flow, newFlowIdx);
                return chosenNewFlow;
        }

        return chosenNewFlow;
    }

    private List<DataTransferFlow> minHopReplica(List<DataTransferFlow> flow,
                                                 int newFlowIdx) {
        List<DataTransferFlow> chosenNewFlow = new LinkedList<DataTransferFlow>();

        return chosenNewFlow;
    }

    private List<DataTransferFlow> enumReplica(BandwidthTopology topology,
                                                   List<DataTransferRequest> request,
                                                   int newRequestIdx,
                                                   List<DataTransferFlow> flow,
                                                   int newFlowIdx) {
        List<DataTransferFlow> chosenNewFlow = new LinkedList<DataTransferFlow>();

        return chosenNewFlow;
    }

    private List<DataTransferFlow> heruisticReplica(BandwidthTopology topology,
                                                List<DataTransferRequest> request,
                                                int newRequestIdx,
                                                List<DataTransferFlow> flow,
                                                int newFlowIdx) {
        List<DataTransferFlow> chosenNewFlow = new LinkedList<DataTransferFlow>();

        return chosenNewFlow;
    }

    private List<OMFRAAllocPolicy> OMFRA_Core(BandwidthTopology topology,
                                        List<DataTransferRequest> request,
                                        List<DataTransferFlow> flow) {
        int num_vertex = topology.getTopologySize();
        int num_flow = flow.length;
        int num_request = request.length;
        int maxPri = getMaxPriority(request);

        List<OMFRAAllocPolicy> AllocPolicy = new LinkedList<OMFRAAllocPolicy>();

        for (int priority=maxPri; i>=0; i--) {

        }



        return AllocPolicy; //TODO
    }

    private List<OMFRAAllocPolicy> OMFRA_Offline(BandwidthTopology topology,
                                              List<DataTransferRequest> request,
                                              List<DataTransferFlow> flow) {
        int num_vertex = topology.getTopologySize();
        int num_flow = flow.length;
        int num_request = request.length;
        List<OMFRAAllocPolicy> AllocPolicy = new LinkedList<OMFRAAllocPolicy>();

        return AllocPolicy; //TODO
    }

    private int getMaxPriority(List<DataTransferRequest> request) {
        int maxPri = 0;
        for (int i=0; i<request.size(); i++) {
            if (request.get(i).getPriority() > maxPri)
                maxPri = request.get(i).getPriority();
        }
        return maxPri;
    }

    private List<DataTransferRequest> getReuqestbyPriority(List<DataTransferRequest> request,
                                                           int priority) {
        List<DataTransferRequest> resultRequest = new LinkedList<DataTransferRequest>();
        for (int i=0; i<request.size(); i++) {
            if (request.get(i).getPriority() == priority)
                resultRequest.add(request.get(i));
        }
        return resultRequest;
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
