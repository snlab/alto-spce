/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetTxBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.NetworkTrackerService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.controller.md.sal.binding.impl.BindingDOMMountPointServiceAdapter.LOG;


public class TpBandwidthMap {
    private HashMap<TpId, Long> tpBandwidthMap = new HashMap();

    public TpBandwidthMap(Topology topology, NetworkTrackerService networkTrackerService) {
        for (Link link : topology.getLink()) {
            TpId tpId = link.getSource().getSourceTp();
            this.tpBandwidthMap.put(tpId,new Long(getBandwidthByTp(link.getSource().getSourceTp(), networkTrackerService)));
        }
    }

    public String getTpBandwidthMap() {
        StringBuffer result = new StringBuffer("{");
        for (TpId key: tpBandwidthMap.keySet()) {
            LOG.info(key.getValue() + ":" + tpBandwidthMap.get(key));
            if (!key.getValue().contains("host"))
                result.append('\"' + key.getValue() + '\"' + ':' + tpBandwidthMap.get(key).toString() + ',');
        }
        result.deleteCharAt(result.lastIndexOf(","));
        result.append('}');
        return result.toString();
    }

    private long getBandwidthByTp(TpId txTpId, NetworkTrackerService networkTrackerService) {
        long availableBandwidth = 0;
        AltoSpceGetTxBandwidthInput input = new AltoSpceGetTxBandwidthInputBuilder().setTpId(txTpId.getValue()).build();
        Future<RpcResult<AltoSpceGetTxBandwidthOutput>> result = networkTrackerService.altoSpceGetTxBandwidth(input);
        try {
            AltoSpceGetTxBandwidthOutput output = result.get().getResult();
            availableBandwidth = output.getSpeed().longValue();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
        return availableBandwidth;
    }
}
