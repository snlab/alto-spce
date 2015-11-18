/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

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
 * Created by qiao on 11/16/15.
 */
public class RequestAlloc {
    private int mSeq;
    private List<FlowAlloc> flow_alloc;

    public RequestAlloc (int mSeq) {
        this.mSeq = mSeq;
        this.flow_alloc = new LinkedList<FlowAlloc>();
    }

    public int getmSeq() {return this.mSeq; }

    public List<FlowAlloc> getAllFlowAlloc () { return this.flow_alloc;}

    public FlowAlloc getFlowAllocbyIndex (int index) {
        return this.flow_alloc.get(index);
    }

    public FlowAlloc getFlowbykSeq (int kSeq) {
        for (int i=0; i<this.flow_alloc.size(); i++) {
            if (this.flow_alloc.get(i).getkSeq() == kSeq)
                return this.flow_alloc.get(i);
        }
        return new FlowAlloc(Integer.MAX_VALUE, 1, false);
    }

    public void setmSeq(int mSeq) { this.mSeq = mSeq; }
    public void setAllFlowAlloc(List<FlowAlloc> flow_alloc) {
        this.flow_alloc = flow_alloc;
    }


}
