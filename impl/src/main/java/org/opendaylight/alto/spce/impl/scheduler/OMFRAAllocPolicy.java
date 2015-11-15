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

    public void OMFRAAllocPolicy(int num_vertex, int num_flow) {
        F_alloc = new long[num_vertex][num_vertex][num_flow];
        R_alloc = new long[num_flow];
    }

    public void setF_allocbyFlow() {}

    public void setR_alloc() {}
}
