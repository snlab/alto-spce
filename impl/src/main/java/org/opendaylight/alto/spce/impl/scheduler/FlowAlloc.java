/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */



package org.opendaylight.alto.spce.impl.scheduler;

/**
 * Created by qiao on 11/16/15.
 */
public class FlowAlloc {
    private int kSeq;
    private long R_alloc;
    private long F_alloc[][];
    private boolean active;

    public FlowAlloc(int kSeq, int num_vertex, boolean active) {
        this.kSeq = kSeq;
        this.R_alloc = 0;
        this.active = active;

        this.F_alloc = new long[num_vertex][num_vertex];
        for (int i = 0; i < num_vertex; i++)
            for (int j = 0; j < num_vertex; j++)
                F_alloc[i][j] = 0;
    }

    public int getkSeq() { return this.kSeq; }
    public long getR_alloc() { return this.R_alloc; }
    public long[][] getAllF_alloc() { return this.F_alloc; }
    public long getF_alloc_entry(int src, int dst) {
        return this.F_alloc[src][dst];
    }

    public boolean getFlowAllocStatus() { return this.active; }

    //public void setFlowAlloc(FlowAlloc object) {this = object;}
    public void setkSeq(int kSeq) { this.kSeq = kSeq; }
    public void setR_alloc(long R_alloc) { this.R_alloc = R_alloc; }
    public void setAllF_alloc(long[][] F_alloc) { this.F_alloc = F_alloc; }
    public void setF_alloc_entry(long F_alloc_entry, int src, int dst) {
        this.F_alloc[src][dst] = F_alloc_entry;
    }
}
