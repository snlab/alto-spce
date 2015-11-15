/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.scheduler;

public class DataTransferRequest {
    private int mSeq;
    private int priority;
    private long arrivalTime;
    private int destination;
    private long volume;

    public DataTransferRequest(int mSeq, int priority, long arrivalTime, int destination, long volume) {
        this.mSeq = mSeq;
        this.priority = priority;
        this.arrivalTime = arrivalTime;
        this.destination = destination;
        this.volume = volume;
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
}
