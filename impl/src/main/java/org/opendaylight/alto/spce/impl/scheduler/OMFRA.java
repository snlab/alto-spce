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

import java.util.*;

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
                                            int newRequestIdx) {
        //int num_vertex = topology.getTopologySize();
        //int num_flow = flow.size();
        //int num_request = request.size();
        List<OMFRAAllocPolicy> AllocPolicy = new LinkedList<OMFRAAllocPolicy>();
        List<DataTransferRequest> processedRequest = new LinkedList<DataTransferRequest>();
        //AllocPolicy.add(new OMFRAAllocPolicy(num_vertex, num_flow));

        if (!this.fileSlicingOption) {
            processedRequest =
                    replicaSelector(topology, request, newRequestIdx);
            //TODO: update flow before scheduling
        }
        else
            processedRequest = request;

        switch (this.schedulingMode) {
            case ConfigurationOptions.ONLINE_OMFRA:
                AllocPolicy = OMFRA_Core(topology, processedRequest);
                return AllocPolicy;
            case ConfigurationOptions.OFFLINE_OMFRA:
                AllocPolicy = OMFRA_Offline(topology, processedRequest);
                return AllocPolicy;
        }
        //TODO: catch exception needed??
        return AllocPolicy;
    }

    public List<DataTransferRequest> replicaSelector(BandwidthTopology topology,
                                                   List<DataTransferRequest> request,
                                                   int newRequestIdx) {

        List<DataTransferRequest> processedRequest = new LinkedList<DataTransferRequest>();

        switch (this.replicaSelectionMode) {
            case ConfigurationOptions.MINHOP_REPLICA:
                processedRequest =
                        minHopReplica(request, newRequestIdx);
                return processedRequest;
            case ConfigurationOptions.ENUM_REPLICA:
                processedRequest =
                        enumReplica(topology, request, newRequestIdx);
                return processedRequest;
            case ConfigurationOptions.HERUISTIC_REPLICA:
                processedRequest =
                        heruisticReplica(topology, request, newRequestIdx);
                return processedRequest;
        }

        return processedRequest;
    }

    private List<DataTransferRequest> minHopReplica(List<DataTransferRequest> request,
                                                 int newFlowIdx) {
        List<DataTransferFlow> chosenNewFlow = new LinkedList<DataTransferFlow>();

        return chosenNewFlow;
    }

    private List<DataTransferRequest> enumReplica(BandwidthTopology topology,
                                                   List<DataTransferRequest> request,
                                                   int newRequestIdx,
                                                   ) {
        List<DataTransferFlow> chosenNewFlow = new LinkedList<DataTransferFlow>();

        return chosenNewFlow;
    }

    private List<DataTransferRequest> heruisticReplica(BandwidthTopology topology,
                                                List<DataTransferRequest> request,
                                                int newRequestIdx,
                                                ) {
        List<DataTransferFlow> chosenNewFlow = new LinkedList<DataTransferFlow>();

        return chosenNewFlow;
    }

    private List<OMFRAAllocPolicy> OMFRA_Core(BandwidthTopology topology,
                                        List<DataTransferRequest> request) {
        int maxPri = getMaxPriority(request);
        int mIdx;
        OMFRAAllocPolicy Sol;// = new OMFRAAllocPolicy();
        OMFRAAllocPolicy tmpSol;// = new OMFRAAllocPolicy();
        List<OMFRAAllocPolicy> AllocPolicy = new LinkedList<OMFRAAllocPolicy>();
        List<DataTransferRequest> M_p = new LinkedList<DataTransferRequest>();
        List<DataTransferRequest> M_unsat = new LinkedList<DataTransferRequest>();
        List<DataTransferRequest> M_sat = new LinkedList<DataTransferRequest>();
        List<DataTransferRequest> tmp_M_unsat = new LinkedList<DataTransferRequest>();
        List<DataTransferRequest> tmp_M_sat = new LinkedList<DataTransferRequest>();
        List<Double> Z_sat = new LinkedList<Double>();
        List<Double> tmp_Z_sat = new LinkedList<Double>();


        for (int priority=maxPri; i>=0; i--) {
            M_p = getReuqestbyPriority(request, priority);


            if (!M_p.isEmpty()) {
                M_unsat = M_p;

                while (!M_unsat.isEmpty()) {
                    Sol = MMFSolver(topology, M_unsat, M_sat, Z_sat);

                    if (M_unsat.size() == 1) {
                        M_sat.add(M_unsat.get(0));
                        M_unsat.remove(0);
                        Z_sat.add(Sol.getZ());
                        continue;
                    }

                    mIdx = 0;
                    while (mIdx <= M_unsat.size()) {
                        if (!FindResidualPath(getResidualTopology(topology, Sol),
                                M_unsat.get(mIdx))) {
                            tmp_M_unsat.add(M_unsat.get(mIdx));
                            tmp_M_sat = M_sat;
                            tmp_Z_sat = Z_sat;
                            for (int i=0; i<M_unsat.size(); i++) {
                                if (i != mIdx) {
                                    tmp_M_sat.add(M_unsat.get(i));
                                    tmp_Z_sat.add(Sol.getZ());
                                }
                            }
                            tmpSol = MMFSolver(topology, tmp_M_unsat, tmp_M_sat, tmp_Z_sat);

                            if ((Sol.getZ() == tmpSol.getZ())
                                    || (Math.abs(Sol.getZ() - tmpSol.getZ()) <= 0.0001)) {
                                M_sat.add(M_unsat.get(mIdx));
                                M_unsat.remove(mIdx);
                                Z_sat.add(Sol.getZ());
                            }
                            else {
                                mIdx++;
                            }

                            tmp_M_unsat.clear();
                            tmp_M_sat.clear();
                            tmp_Z_sat.clear();
                        }
                    }
                }
            }

            AllocPolicy.addAll(getFeasibleAllocation(topology, M_sat, Z_sat));

            M_p.clear();
            M_unsat.clear();
            M_sat.clear();
            Z_sat.clear();

        }

        return AllocPolicy; //TODO
    }

    private List<OMFRAAllocPolicy> OMFRA_Offline(BandwidthTopology topology,
                                              List<DataTransferRequest> request) {
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

    private BandwidthTopology getResidualTopology(BandwidthTopology topology,
                                                  OMFRAAllocPolicy Alloc) {
        BandwidthTopology residualTopology = topology;
        List<RequestAlloc> requestAllocs = Alloc.getAllRequestAlloc();

        for (int i=0; i<requestAllocs.size(); i++) {
            for (int j=0; j<requestAllocs.get(i).getAllFlowAlloc().size(); j++) {
                if (requestAllocs.get(i).getAllFlowAlloc().get(j).getFlowAllocStatus()) {
                    residualTopology.setResidualBandwidthTopology(requestAllocs.get(i).getAllFlowAlloc().get(j).getAllF_alloc());
                }
            }
        }

        return residualTopology;
    }

    private boolean FindResidualPath(BandwidthTopology tmpTopology,
                                     DataTransferRequest request) {
        List<DataTransferFlow> activeFlow = request.getActiveFlow();
        int num_flow = activeFlow.size();

        if (num_flow ==0) {
            //TODO: add output error info
            return false;
        }

        for (int k=0; k<num_flow; k++) {
            if (activeFlow.get(k).getPath().isEmpty()) {//Flow path is not fixed
                if (FindPath(tmpTopology, activeFlow.get(k).getSource(), request.getDestination())) {
                    return true;
                }
            }
            else {//Flow path is fixed
                List<Integer> path = activeFlow.get(k).getPath();
                boolean found = true;
                for (int i=0; i<path.size()-1; i++) {
                    if (tmpTopology.getBandwidth(path.get(i), path.get(i+1)) == 0) {
                        found = false;
                        break;
                    }
                    else if (tmpTopology.getBandwidth(path.get(i), path.get(i+1)) < activeFlow.get(k).getMinBandwidth()) {
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

    private List<OMFRAAllocPolicy> getFeasibleAllocation (BandwidthTopology topology,
                                                     List<DataTransferRequest> satRequest,
                                                     List<Double> Z_sat) { //TODO
        List<OMFRAAllocPolicy> feasibleAlloc = new LinkedList<OMFRAAllocPolicy>();
        return feasibleAlloc;
    }

    public OMFRAAllocPolicy MMFSolver(BandwidthTopology topology,
                               List<DataTransferRequest> unsatDataTransferRequests,
                               List<DataTransferRequest> satDataTransferRequests,
                               List<Double> zSat) { //TODO::handle Routing

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
