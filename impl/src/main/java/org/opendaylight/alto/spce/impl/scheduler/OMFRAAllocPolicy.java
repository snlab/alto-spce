/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.alto.spce.impl.scheduler;

/**
 * Created by qiao on 11/15/15.
 */
public class OMFRAAllocPolicy {
    private long F_alloc[][][];
    private long R_alloc[];
    private long FlowSeq[];

    public void OMFRAAllocPolicy(int num_vertex, int num_flow) {
        F_alloc = new long[num_vertex][num_vertex][num_flow];
        R_alloc = new long[num_flow];
        FlowSeq = new long[num_flow];

        for (int k = 0; k < num_flow; k++) {
            for (int i = 0; i < num_vertex; i++)
                for (int j = 0; j < num_vertex; j++)
                    F_alloc[i][j][k] = 0;
            R_alloc[k] = 0;
            FlowSeq[k] = 0;
        }
    }

    public void setF_allocbyFlow() {}

    public void setR_alloc() {}

    public long[][][] getF_alloc() {return F_alloc;}

    public long[] getR_alloc() {return R_alloc;}
}
