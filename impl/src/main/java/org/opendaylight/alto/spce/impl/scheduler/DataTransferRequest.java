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

public class DataTransferRequest {
    private int mSeq;
    private int priority;
    private long arrivalTime;
    private int destination;
    private long volume;
    private List<DataTransferFlow> flow;

    public DataTransferRequest(int mSeq, int priority, long arrivalTime,
                               int destination, long volume, List<DataTransferFlow> flow) {
        this.mSeq = mSeq;
        this.priority = priority;
        this.arrivalTime = arrivalTime;
        this.destination = destination;
        this.volume = volume;
        this.flow = flow;
    }

    public int getmSeq() {
        return this.mSeq;
    }

    public int getPriority() {
        return this.priority;
    }

    public long getArrivalTime() {
        return this.arrivalTime;
    }

    public int getDestination() {
        return this.destination;
    }

    public long getVolume() {
        return this.volume;
    }

    public List<DataTransferFlow> getFlow() { return this.flow;}

    public List<DataTransferFlow> getActiveFlow() {
        List<DataTransferFlow> activeFlow = new LinkedList<DataTransferFlow>();
        for (int i = 0; i < this.flow.size(); i++) {
            if (this.flow.get(i).getFlowStatus()) {
                activeFlow.add(this.flow.get(i));
            }
        }
        return activeFlow;
    }

}
