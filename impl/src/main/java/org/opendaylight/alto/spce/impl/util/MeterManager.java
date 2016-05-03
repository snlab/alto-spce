/*
 * Copyright © 2015 Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.BandId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterBandType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterRef;
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
    private final long DEAULT_METER_ID_PICA8 = 256;
    private final String DEFAULT_METER_NAME = "alto-spce rate limiting";
    private final String DEFAULT_METER_CONTAINER = "alto-spce rate limiting container";
    private HashMap<NodeConnectorRef, AtomicLong> meterIdInSwitch = new HashMap<>();

    public MeterManager(SalMeterService salMeterService) {
        this.salMeterService = salMeterService;
    }

    //need to check the default meter ID is what in the Pica8 switch.
    //suppose it begins with 1. So I init it with 0, and add 1 to it when it is first used.
    private MeterId meterIdInc(NodeConnectorRef nodeConnectorRef) {
        LOG.info("in meterIdInc, nodeConnectorRef is" + nodeConnectorRef.toString());
        if (meterIdInSwitch.containsKey(nodeConnectorRef)) {
            meterIdInSwitch.put(nodeConnectorRef,
                    new AtomicLong(meterIdInSwitch.get(nodeConnectorRef).incrementAndGet()));
        } else {
            meterIdInSwitch.put(nodeConnectorRef, new AtomicLong(0L));
        }
        return new MeterId(meterIdInSwitch.get(nodeConnectorRef).longValue());
    }

    public void addDropMeter(long dropRate, long dropBurstSize, NodeConnectorRef nodeConnectorRef) {
        LOG.info("In MeterManager.addDropMeter");
        Meter meter = createDropMeter(dropRate, dropBurstSize, nodeConnectorRef);
        writeMeterToConfigData(buildMeterPath(nodeConnectorRef),meter);
    }

    public void addDropMeterByPath(long dropRate, long dropBurstSize, List<NodeConnectorRef> path) {
        LOG.info("In MeterManager.addDropMeterByPath");
        if (dropRate <= 0 || dropBurstSize <= 0) {
            LOG.info("In add MeterByPath, dropRate and dropBurstSize must be a positive long integer.");
            return;
        }

        for (NodeConnectorRef nc : path) {
            addDropMeter(dropRate, dropBurstSize, nc);
        }
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter> buildMeterPath(NodeConnectorRef nodeConnectorRef) {
        MeterId meterId = new MeterId(meterIdInSwitch.get(nodeConnectorRef).longValue());
        MeterKey meterKey = new MeterKey(meterId);
        return InstanceIdentifierUtils.generateMeterInstanceIdentifier(nodeConnectorRef, meterKey);
    }

    private Meter createDropMeter(long dropRate, long dropBurstSize, NodeConnectorRef nodeConnectorRef) {
        LOG.info("nodeConnectorRef is" + nodeConnectorRef.toString());
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
                .setMeterBandHeaders(mbhsBuilder.build())
                .setMeterId(new MeterId(meterIdInc(nodeConnectorRef)))
                .setMeterName(DEFAULT_METER_NAME)
                .setContainerName(DEFAULT_METER_CONTAINER);
        return meterBuilder.build();
    }

    private Future<RpcResult<AddMeterOutput>> writeMeterToConfigData(InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter> meterPath,
                                                                     Meter meter) {
        final InstanceIdentifier<Node> nodeInstanceId = meterPath.<Node>firstIdentifierOf(Node.class);
        final AddMeterInputBuilder builder = new AddMeterInputBuilder(meter);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setMeterRef(new MeterRef(meterPath));
        builder.setTransactionUri(new Uri(meter.getMeterId().getValue().toString()));
        return salMeterService.addMeter(builder.build());
    }
}
