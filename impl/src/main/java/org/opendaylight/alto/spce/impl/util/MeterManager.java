/*
 * Copyright © 2015 Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.endpoint.group.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.band.type.DropBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.MeterBandHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.meter.band.headers.MeterBandHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.meter.band.headers.MeterBandHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.meter.band.headers.meter.band.header.MeterBandTypesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class MeterManager {
    private static final Logger LOG = LoggerFactory.getLogger(MeterManager.class);
    private SalMeterService salMeterService;
    private final long MIN_METER_ID_PICA8 = 1;
    private final long MAX_METER_ID_PICA8 = 256;
    private final String DEFAULT_METER_NAME = "alto-spce rate limiting";
    private final String DEFAULT_METER_CONTAINER = "alto-spce rate limiting container";
    //private HashMap<NodeConnectorRef, AtomicLong> meterIdInSwitch = new HashMap<>();

    //List<Long> is used for allocating a new meter ID in one switch
    private HashMap<NodeConnectorRef, List<Boolean>> switchToMeterIdListMap = new HashMap<>();

    private HashMap<NodeConnectorRef, HashMap<EndpointPairAndRequirement, Long>> switchToPerFlowToMeterIdMap = new HashMap<>();

    public MeterManager(SalMeterService salMeterService) {
        this.salMeterService = salMeterService;
    }

    public long getPerFlowMeterId(NodeConnectorRef nodeConnectorRef, String src, String dst, long dropRate, long dropBurstSize) {
        return switchToPerFlowToMeterIdMap.get(nodeConnectorRef).get(new EndpointPairAndRequirement(src, dst, dropRate, dropBurstSize));
    }
    /*public HashMap<NodeConnectorRef, Table<Long, Long, Long>> getSwitchMeterMap() {
        return this.switchMeterMap;
    }*/

    //need to check the default meter ID is what in the Pica8 switch.
    //suppose it begins with 1. So I init it with 0, and add 1 to it when it is first used.
    /*private MeterId meterIdInc(NodeConnectorRef nodeConnectorRef) {
        LOG.info("in meterIdInc, nodeConnectorRef is" + nodeConnectorRef.toString());
        if (meterIdInSwitch.containsKey(nodeConnectorRef)) {
            meterIdInSwitch.put(nodeConnectorRef,
                    new AtomicLong(meterIdInSwitch.get(nodeConnectorRef).incrementAndGet()));
        } else {
            meterIdInSwitch.put(nodeConnectorRef, new AtomicLong(1L));
        }
        return new MeterId(meterIdInSwitch.get(nodeConnectorRef).longValue());
    }*/

    public void removeMeterFromSwitch (Endpoint endpoint, NodeConnectorRef nodeConnectorRef, NodeRef nodeRef, long dropRate, long burstSize) {
        EndpointPairAndRequirement epr = new EndpointPairAndRequirement(endpoint.getSrc().getValue(), endpoint.getDst().getValue(), dropRate, burstSize);
        int meterId = switchToPerFlowToMeterIdMap.get(nodeConnectorRef).get(epr).intValue();
        this.salMeterService.removeMeter(new RemoveMeterInputBuilder()
                .setMeterId(new MeterId((long)meterId))
                .setNode(nodeRef)
                .build());
        List<Boolean> perSwitchMeterList = switchToMeterIdListMap.get(nodeConnectorRef);
        perSwitchMeterList.set(meterId, false);
        switchToMeterIdListMap.put(nodeConnectorRef, perSwitchMeterList);
        HashMap<EndpointPairAndRequirement, Long> deleteFlowMeterMap = switchToPerFlowToMeterIdMap.get(nodeConnectorRef);
        deleteFlowMeterMap.remove(epr);
        switchToPerFlowToMeterIdMap.put(nodeConnectorRef, deleteFlowMeterMap);
    }

    public void addDropMeter(String src, String dst, long dropRate, long dropBurstSize, NodeConnectorRef nodeConnectorRef) {
        LOG.info("In MeterManager.addDropMeter");
        List<Boolean> perSwitchMeterList;
        HashMap<EndpointPairAndRequirement, Long> perSwitchPerFlowToMeterIdMap;
        if (!switchToMeterIdListMap.containsKey(nodeConnectorRef)) {
            perSwitchMeterList = new LinkedList<>();
            for (int i=0 ; i<=MAX_METER_ID_PICA8; ++i) {
                //false stands for meterId == i is free. We must use i from 1 not from 0.
                perSwitchMeterList.add(false);
            }
            switchToMeterIdListMap.put(nodeConnectorRef, perSwitchMeterList);
        }
        if (!switchToPerFlowToMeterIdMap.containsKey(nodeConnectorRef)) {
            perSwitchPerFlowToMeterIdMap = new HashMap<>();
            switchToPerFlowToMeterIdMap.put(nodeConnectorRef, perSwitchPerFlowToMeterIdMap);
        }

        perSwitchMeterList = switchToMeterIdListMap.get(nodeConnectorRef);
        perSwitchPerFlowToMeterIdMap = switchToPerFlowToMeterIdMap.get(nodeConnectorRef);

        int firstFreeMeterId = 1;
        while (perSwitchMeterList.get(firstFreeMeterId)) {
            ++firstFreeMeterId;
        }

        Meter meter = createDropMeter(dropRate, dropBurstSize, firstFreeMeterId);
        writeMeterToConfigData(buildMeterPath(firstFreeMeterId, nodeConnectorRef),meter);
        perSwitchMeterList.set(firstFreeMeterId, true);
        EndpointPairAndRequirement epr = new EndpointPairAndRequirement(src, dst, dropRate, dropBurstSize);
        perSwitchPerFlowToMeterIdMap.put(epr, (long)firstFreeMeterId);

        switchToMeterIdListMap.put(nodeConnectorRef, perSwitchMeterList);
        switchToPerFlowToMeterIdMap.put(nodeConnectorRef, perSwitchPerFlowToMeterIdMap);
    }

    public void addDropMeterByPath(String src, String dst, long dropRate, long dropBurstSize, List<NodeConnectorRef> path) {
        LOG.info("In MeterManager.addDropMeterByPath");
        if (dropRate <= 0 || dropBurstSize <= 0) {
            LOG.info("In add MeterByPath, dropRate and dropBurstSize must be a positive long integer.");
            return;
        }
        for (NodeConnectorRef nc : path) {
            addDropMeter(src, dst, dropRate, dropBurstSize, nc);
        }
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter> buildMeterPath(long meterIdLong, NodeConnectorRef nodeConnectorRef) {
        MeterId meterId = new MeterId(meterIdLong);
        MeterKey meterKey = new MeterKey(meterId);
        return InstanceIdentifierUtils.generateMeterInstanceIdentifier(nodeConnectorRef, meterKey);
    }

    private Meter createDropMeter(long dropRate, long dropBurstSize, long meterId) {
        //LOG.info("nodeConnectorRef is" + nodeConnectorRef.toString());
        DropBuilder dropBuilder = new DropBuilder();
        dropBuilder
                .setDropBurstSize(dropBurstSize)
                .setDropRate(dropRate);

        MeterBandHeaderBuilder mbhBuilder = new MeterBandHeaderBuilder()
                .setBandType(dropBuilder.build())
                .setBandId(new BandId(0L))
                .setMeterBandTypes(new MeterBandTypesBuilder()
                        .setFlags(new MeterBandType(true, false, false)).build())
                .setBandRate(dropRate)
                .setBandBurstSize(dropBurstSize);

        LOG.info("In createDropMeter, MeterBandHeaderBuilder is" + mbhBuilder.toString());

        List<MeterBandHeader> mbhList = new LinkedList<>();
        mbhList.add(mbhBuilder.build());

        MeterBandHeadersBuilder mbhsBuilder = new MeterBandHeadersBuilder()
                .setMeterBandHeader(mbhList);

        LOG.info("In createDropMeter, MeterBandHeader is " + mbhBuilder.build().toString());
        MeterBuilder meterBuilder = new MeterBuilder()
                .setFlags(new MeterFlags(true, true, false, false))
                .setMeterBandHeaders(mbhsBuilder.build())
                .setMeterId(new MeterId(meterId))
                .setMeterName(DEFAULT_METER_NAME)
                .setContainerName(DEFAULT_METER_CONTAINER);
        return meterBuilder.build();
    }

    private Future<RpcResult<AddMeterOutput>> writeMeterToConfigData(InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter> meterPath,
                                                                     Meter meter) {
        LOG.info("In writeMeterToConfigData");
        final InstanceIdentifier<Node> nodeInstanceId = meterPath.<Node>firstIdentifierOf(Node.class);
        final AddMeterInputBuilder builder = new AddMeterInputBuilder(meter);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setMeterRef(new MeterRef(meterPath));
        builder.setTransactionUri(new Uri(meter.getMeterId().getValue().toString()));
        return salMeterService.addMeter(builder.build());
    }
}
