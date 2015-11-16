/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.alto.spce.impl.scheduler;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by qiao on 11/15/15.
 */
public class OMFRAAllocPolicy {
    private List<RequestAlloc> request_alloc;
    private double z; //this variable is only used to store the solution of MMF_solver

    public void OMFRAAllocPolicy(double z) {
        this.z = z;
        this.request_alloc = new LinkedList<RequestAlloc>();
    }

    public List<RequestAlloc> getAllRequestAlloc() { return this.request_alloc;}

    public RequestAlloc getRequestAllocbyIndex (int index) {
        return this.request_alloc.get(index);
    }

    public RequestAlloc getRequestAllocbymSeq (int mSeq) {
        for (int i=0; i<this.request_alloc.size(); i++) {
            if (this.request_alloc.get(i).getmSeq() == mSeq)
                return this.request_alloc.get(i);
        }
    }

    public void setAllRequestAlloc(List<RequestAlloc> request_alloc) {
        this.request_alloc = request_alloc;
    }
}
