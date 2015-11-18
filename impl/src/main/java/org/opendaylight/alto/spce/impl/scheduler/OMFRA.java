/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.alto.spce.impl.scheduler;

//import com.joptimizer.optimizers.LPOptimizationRequest;
//import com.joptimizer.optimizers.LPPrimalDualMethod;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.GlpkException;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

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

    public void setReplicationSelectionMode(int replicationSelectionMode) {
        switch (replicationSelectionMode) {
            case ConfigurationOptions.MINHOP_REPLICA:
                this.replicaSelectionMode = ConfigurationOptions.MINHOP_REPLICA;
                return;
            case ConfigurationOptions.ENUM_REPLICA:
                this.replicaSelectionMode = ConfigurationOptions.ENUM_REPLICA;
                return;
            case ConfigurationOptions.HERUISTIC_REPLICA:
                this.replicaSelectionMode = ConfigurationOptions.HERUISTIC_REPLICA;
                return;
            case ConfigurationOptions.RANDOM_REPLICA:
                this.replicaSelectionMode = ConfigurationOptions.RANDOM_REPLICA;
                return;
            case ConfigurationOptions.OPPORTUNISTIC_REPLICA:
                this.replicaSelectionMode = ConfigurationOptions.OPPORTUNISTIC_REPLICA;
                return;
            default:
                this.replicaSelectionMode = ConfigurationOptions.OPPORTUNISTIC_REPLICA;
                return;
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
            replicaSelector(topology, request, newRequestIdx);
        }
        else
            processedRequest = request;

        switch (this.schedulingMode) {
            case ConfigurationOptions.ONLINE_OMFRA:
                AllocPolicy = OMFRA_Core(topology, request);
                return AllocPolicy;
            case ConfigurationOptions.OFFLINE_OMFRA:
                AllocPolicy = OMFRA_Offline(topology, request);
                return AllocPolicy;
        }
        //TODO: catch exception needed??
        return AllocPolicy;
    }

    public void replicaSelector(BandwidthTopology topology,
                                                   List<DataTransferRequest> request,
                                                   int newRequestIdx) {

        //List<DataTransferRequest> processedRequest = new LinkedList<DataTransferRequest>();

        switch (this.replicaSelectionMode) {
            case ConfigurationOptions.MINHOP_REPLICA:
                //processedRequest =
                minHopReplica(request, newRequestIdx);
                return;
                //return request;
            case ConfigurationOptions.ENUM_REPLICA:
                //processedRequest =
                enumReplica(topology, request, newRequestIdx);
                return;
                //return request;
            case ConfigurationOptions.HERUISTIC_REPLICA:
                //processedRequest =
                heruisticReplica(topology, request, newRequestIdx);
                return;
                //return request;
            case ConfigurationOptions.RANDOM_REPLICA:
                randomReplica(request, newRequestIdx);
                return;
                //return
            case ConfigurationOptions.OPPORTUNISTIC_REPLICA:
                opportunisticReplica(request, newRequestIdx);
                return;
            default:
                opportunisticReplica(request, newRequestIdx);
                return;
                //return request;
        }
//        return request;
    }

    private void opportunisticReplica(List<DataTransferRequest> request,
                                                    int newRequestIdx) {
        int num_request = request.size();
        int num_flow;
        for (int i = newRequestIdx; i < num_request; i++) {
            num_flow = request.get(i).getFlow().size();
            for (int j = 0; j < request.get(i).getFlow().size(); j++) {
                if (j==0)
                    request.get(i).getFlow().get(j).setFlowStatus(true);
                else
                    request.get(i).getFlow().get(j).setFlowStatus(false);
            }
        }
    }

    private void randomReplica(List<DataTransferRequest> request,
                                       int newRequestIdx) {
        Random random = new Random();
        int num_request = request.size();
        int num_flow;
        int chosenFlowIdx;

        for (int i = newRequestIdx; i < num_request; i++) {
            num_flow = request.get(i).getFlow().size();
            chosenFlowIdx = random.nextInt(num_flow);
            for (int j = 0; j < num_flow; j++) {
                if (j==chosenFlowIdx)
                    request.get(i).getFlow().get(j).setFlowStatus(true);
                else
                    request.get(i).getFlow().get(j).setFlowStatus(false);
            }
        }
    }

    private void minHopReplica(List<DataTransferRequest> request,
                                                 int newRequestIdx) {
        int num_request = request.size();
        int num_flow;
        int chosenFlowIdx, minHopcount;

        for (int i = newRequestIdx; i < num_request; i++) {
            num_flow = request.get(i).getFlow().size();
            minHopcount = request.get(i).getFlow().get(0).getPath().size();
            chosenFlowIdx = 0;
            for (int j = 1; j < num_flow; j++) {
                if (minHopcount > request.get(i).getFlow().get(j).getPath().size()) {
                    minHopcount = request.get(i).getFlow().get(j).getPath().size();
                    chosenFlowIdx = j;
                }
            }

            for (int j = 0; j < num_flow; j++) {
                if (j==chosenFlowIdx)
                    request.get(i).getFlow().get(j).setFlowStatus(true);
                else
                    request.get(i).getFlow().get(j).setFlowStatus(false);
            }
        }
    }

    private void enumReplica(BandwidthTopology topology,
                                                   List<DataTransferRequest> request,
                                                   int newRequestIdx
                                                   ) {

    }

    private void heruisticReplica(BandwidthTopology topology,
                                                List<DataTransferRequest> request,
                                                int newRequestIdx
                                                ) {

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


        for (int priority=maxPri; priority>=0; priority--) {
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
                               List<Double> zSat) {
        OMFRAAllocPolicy Sol = new OMFRAAllocPolicy();
        SWIGTYPE_p_int ind;
        SWIGTYPE_p_double val;
        glp_prob MMF = GLPK.glp_create_prob();
        System.out.println("Problem created");
        GLPK.glp_set_prob_name(MMF, "MMF");

        int unsat_num_request = unsatDataTransferRequests.size();
        int sat_num_request = satDataTransferRequests.size();
        int num_vertex = topology.getTopologySize();
        int num_unsat_flow = 0;
        int num_sat_flow = 0;
        int num_flow;
        int tmp_flow_count;
        int routing_constraint_idx;

        for (int i = 0; i < unsat_num_request; i++)
            num_unsat_flow += unsatDataTransferRequests.get(i).getActiveFlow().size();

        for (int i = 0; i < sat_num_request; i++)
            num_sat_flow += satDataTransferRequests.get(i).getActiveFlow().size();

        num_flow = num_unsat_flow + num_sat_flow;

        //Add column for each decision variable

        GLPK.glp_add_cols(MMF, num_vertex*num_vertex*num_flow+num_flow+1);

        //Set name for each column/decision variable
        //IMPORTANT: column index starts from 1!!!
        for (int k = 0; k < num_flow; k++) {
            for (int i = 0; i < num_vertex; i++)
                for (int j = 0; j < num_vertex; j++) {
                    GLPK.glp_set_col_name(MMF,
                            k*num_vertex*num_vertex+i*num_vertex+j+1,
                            "f" + Integer.toString(k) + "s" + Integer.toString(i)
                            + "d" + Integer.toString(j));
                    GLPK.glp_set_col_kind(MMF,
                            k*num_vertex*num_vertex+i*num_vertex+j+1,
                            GLPKConstants.GLP_CV);
                    GLPK.glp_set_col_bnds(MMF,
                            k*num_vertex*num_vertex+i*num_vertex+j+1,
                            GLPKConstants.GLP_LO, 0, Double.MAX_VALUE);
                }
            GLPK.glp_set_col_name(MMF,
                                num_vertex*num_vertex*num_flow+k+1,
                                "rf"+Integer.toString(k));
            GLPK.glp_set_col_kind(MMF,
                    num_vertex*num_vertex*num_flow+k+1,
                    GLPKConstants.GLP_CV);
            tmp_flow_count = 0;
            for (int m = 0; m < unsat_num_request + sat_num_request; m++) {
                if (m < unsat_num_request)
                    tmp_flow_count += unsatDataTransferRequests.get(m).getActiveFlow().size();
                else
                    tmp_flow_count += satDataTransferRequests.get(m-unsat_num_request).getActiveFlow().size();
                if (tmp_flow_count > k) {
                    if (m < unsat_num_request)
                        GLPK.glp_set_col_bnds(MMF,
                                num_vertex*num_vertex*num_flow+k+1,
                                GLPKConstants.GLP_DB, 0,
                                (double)satDataTransferRequests.get(m).getVolume());
                    else
                        GLPK.glp_set_col_bnds(MMF,
                                num_vertex*num_vertex*num_flow+k+1,
                                GLPKConstants.GLP_DB, 0,
                                (double)satDataTransferRequests.get(m-unsat_num_request).getVolume());
                    break;
                }
            }
        }

        GLPK.glp_set_col_name(MMF,
                            num_vertex*num_vertex*num_flow+num_flow+1,
                            "z");
        GLPK.glp_set_col_kind(MMF,
                num_vertex*num_vertex*num_flow+num_flow+1,
                GLPKConstants.GLP_CV);
        GLPK.glp_set_col_bnds(MMF,
                num_vertex*num_vertex*num_flow+num_flow+1,
                GLPKConstants.GLP_DB, 0, 1);


        //Build coefficient matrix for link capacity
        ind = GLPK.new_intArray(num_flow);
        val = GLPK.new_doubleArray(num_flow);

        for (int i = 0; i < num_vertex; i++)
            for (int j = 0; j < num_vertex; j++) {
                GLPK.glp_add_rows(MMF, 1);
                GLPK.glp_set_row_name(MMF, i*num_vertex+j+1,
                        "capacity s" + Integer.toString(i) + "d" + Integer.toString(j));
                for (int k = 0; k < num_flow; k++) {
                    GLPK.intArray_setitem(ind, k+1,
                                        k*num_vertex*num_vertex+i*num_vertex+j+1);
                    GLPK.doubleArray_setitem(val, 1, 1.);
                }
                GLPK.glp_set_mat_row(MMF, i*num_vertex+j+1, 1, ind, val);
                GLPK.glp_set_row_bnds(MMF, i*num_vertex+j+1,
                        GLPKConstants.GLP_DB, 0, (double)topology.getBandwidth(i, j));
            }

        GLPK.delete_intArray(ind);
        GLPK.delete_doubleArray(val);

        //So far we already have num_vertex*num_vertex rows
        //Build coefficient matrix for unsaturated requests
        tmp_flow_count = 0;
        for (int i = 0; i < unsat_num_request; i++) {
            int num_flow_per_request = unsatDataTransferRequests.get(i).getActiveFlow().size();
            GLPK.glp_add_rows(MMF, 1);
            GLPK.glp_set_row_name(MMF, num_vertex*num_vertex+i+1,
                    "unsat m" + Integer.toString(i));
            ind = GLPK.new_intArray(num_flow_per_request+1);
            val = GLPK.new_doubleArray(num_flow_per_request+1);
            for (int k = 0; k < num_flow_per_request; k++) {
                GLPK.intArray_setitem(ind, k+1,
                        num_vertex*num_vertex*num_flow+tmp_flow_count+k+1);
                GLPK.doubleArray_setitem(val, k+1, 1.);
            }
            GLPK.intArray_setitem(ind, num_flow_per_request+1,
                                num_vertex*num_vertex+num_flow+1);
            GLPK.doubleArray_setitem(val, num_flow_per_request+1,
                                (double)unsatDataTransferRequests.get(i).getVolume() * (double)-1);

            GLPK.glp_set_mat_row(MMF, num_vertex*num_vertex+i+1,
                    num_flow_per_request+1, ind, val);
            GLPK.glp_set_row_bnds(MMF, num_vertex*num_vertex+i+1,
                    GLPKConstants.GLP_LO, 0, Double.MAX_VALUE);
            GLPK.delete_intArray(ind);
            GLPK.delete_doubleArray(val);
            tmp_flow_count += num_flow_per_request;
        }


        //So far we already have num_vertex*num_vertex+unsat_num_request rows
        //Build coefficient matrix for flow conservation
        for (int k = 0; k < num_flow; k++) {
            //double flowVol=0.;
            int flowSrc = 0;
            int flowDst = 0;
            int last_tmp_flow_count = 0;
            tmp_flow_count = 0;
            for (int m = 0; m < unsat_num_request + sat_num_request; m++) {
                if (m < unsat_num_request) {
                    last_tmp_flow_count = tmp_flow_count;
                    tmp_flow_count += unsatDataTransferRequests.get(m).getActiveFlow().size();
                }
                else {
                    last_tmp_flow_count = tmp_flow_count;
                    tmp_flow_count += satDataTransferRequests.get(m - unsat_num_request).getActiveFlow().size();
                }

                if (tmp_flow_count > k) {
                    if (m < unsat_num_request) {
                        //flowVol = (double) unsatDataTransferRequests.get(m).getVolume();
                        flowDst = unsatDataTransferRequests.get(m).getDestination();
                        flowSrc = unsatDataTransferRequests.get(m).getActiveFlowbyIndex(k-last_tmp_flow_count).getSource();
                    }
                    else {
                        //flowVol = (double) satDataTransferRequests.get(m-unsat_num_request).getVolume();
                        flowDst = satDataTransferRequests.get(m-unsat_num_request).getDestination();
                        flowSrc = satDataTransferRequests.get(m-unsat_num_request).getActiveFlowbyIndex(k-last_tmp_flow_count).getSource();
                    }
                    break;
                }
            }

            for (int i = 0; i < num_vertex; i++) {
                GLPK.glp_add_rows(MMF, 1);
                GLPK.glp_set_row_name(MMF, num_vertex*num_vertex+unsat_num_request+k*num_vertex+i+1,
                        "f" + Integer.toString(k) + "v" +Integer.toString(i));
                ind = GLPK.new_intArray(2*num_vertex-1);
                val = GLPK.new_doubleArray(2*num_vertex-1);
                int tmpIdx = 0;
                for (int j = 0; j < num_vertex; j++) {
                    if (topology.getBandwidth(i, j) > 0) {
                        GLPK.intArray_setitem(ind, tmpIdx+1,
                                k*num_vertex*num_vertex+i*num_vertex+j+1);
                        GLPK.doubleArray_setitem(val, tmpIdx+1, 1.);
                        tmpIdx++;
                    }
                    if (topology.getBandwidth(j, i) > 0) {
                        GLPK.intArray_setitem(ind, tmpIdx+1,
                                k*num_vertex*num_vertex+i*num_vertex+j+1);
                        GLPK.doubleArray_setitem(val, tmpIdx+1, -1.);
                        tmpIdx++;
                    }
                }

                if (i == flowSrc) {
                    GLPK.intArray_setitem(ind, tmpIdx+1,
                                num_vertex*num_vertex+k+1);
                    GLPK.doubleArray_setitem(val, tmpIdx+1, -1);
                    tmpIdx++;
                } else if (i == flowDst) {
                    GLPK.intArray_setitem(ind, tmpIdx+1,
                            num_vertex*num_vertex+k+1);
                    GLPK.doubleArray_setitem(val, tmpIdx+1, 1);
                    tmpIdx++;
                }

                GLPK.glp_set_mat_row(MMF, num_vertex*num_vertex+unsat_num_request+k*num_vertex+i+1,
                        tmpIdx, ind, val);
                GLPK.glp_set_row_bnds(MMF, num_vertex*num_vertex+i+1,
                        GLPKConstants.GLP_FX, 0, Double.MAX_VALUE);
                GLPK.delete_intArray(ind);
                GLPK.delete_doubleArray(val);
            }
        }

        //So far we already have
        //num_vertex*num_vertex+unsat_num_request+num_flow*num_vertex rows
        //Build coefficient matrix for saturated requests
        tmp_flow_count = 0;
        for (int i = 0; i < sat_num_request; i++) {
            int num_flow_per_request = satDataTransferRequests.get(i).getActiveFlow().size();
            GLPK.glp_add_rows(MMF, 1);
            GLPK.glp_set_row_name(MMF,
                    num_vertex*num_vertex+unsat_num_request+num_flow*num_vertex+i+1,
                    "sat m" + Integer.toString(i));
            ind = GLPK.new_intArray(num_flow_per_request);
            val = GLPK.new_doubleArray(num_flow_per_request);
            for (int k = 0; k < num_flow_per_request; k++) {
                GLPK.intArray_setitem(ind, k+1,
                        num_vertex*num_vertex*num_flow+num_unsat_flow+tmp_flow_count+k+1);
                GLPK.doubleArray_setitem(val, k+1, 1.);
            }


            GLPK.glp_set_mat_row(MMF,
                    num_vertex*num_vertex+unsat_num_request+num_flow*num_vertex+i+1,
                    num_flow_per_request, ind, val);
            GLPK.glp_set_row_bnds(MMF,
                    num_vertex*num_vertex+unsat_num_request+num_flow*num_vertex+i+1,
                    GLPKConstants.GLP_FX,
                    (double)satDataTransferRequests.get(i).getVolume() * zSat.get(i),
                    Double.MAX_VALUE);
            GLPK.delete_intArray(ind);
            GLPK.delete_doubleArray(val);
            tmp_flow_count += num_flow_per_request;
        }

        //So far we already have
        //num_vertex*num_vertex+unsat_num_request+num_flow*num_vertex+sat_num_request rows
        //Build coefficient matrix for paths
        tmp_flow_count = 0;
        routing_constraint_idx = 0;
        for (int i = 0; i < unsat_num_request; i++) {
            for (int j = 0; j < unsatDataTransferRequests.get(i).getActiveFlow().size(); j++) {
                if (unsatDataTransferRequests.get(i).getActiveFlowbyIndex(j+1).getPath().size() > 0) {
                    GLPK.glp_add_rows(MMF, 1);
                    GLPK.glp_set_row_name(MMF,
                            num_vertex*num_vertex+unsat_num_request+num_flow*num_vertex+sat_num_request+routing_constraint_idx+1,
                            "unsat routing m" + Integer.toString(i) + "f" + Integer.toString(j));
                    ind = GLPK.new_intArray(num_vertex*num_vertex);
                    val = GLPK.new_doubleArray(num_vertex*num_vertex);

                    for (int v_i = 0; v_i < num_vertex; v_i++)
                        for (int v_j = 0; v_j < num_vertex; v_j++) {
                            if (linkOnPath(unsatDataTransferRequests.get(i).getActiveFlowbyIndex(j+1).getPath(),
                                    v_i, v_j) {

                            }
                        }

                }
            }
        }

        tmp_flow_count = 0;

        for (int i = 0; i < sat_num_request; i++) {

        }


        return Sol;
        /*try
        {
            System.loadLibrary( "glpk_java" );
        }
        catch (UnsatisfiedLinkError e)

        {

            System.out.print("Cannot load glpk-java\n");
            return Sol;
        }*/







    }


    private boolean linkOnPath(List<Integer> Path, int src, int dst) {
        for (int i = 0; i < Path.size()-1; i++) {
            if ((Path.get(i) == src) && (Path.get(i+1) == dst))
                return true;
        }
        return false;
    }



}
