/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.opendaylight.alto.spce.impl.algorithm.PathComputation;
import org.opendaylight.alto.spce.impl.util.*;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.alto.spce.setup.input.ConstraintMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.alto.spce.setup.input.ConstraintMetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.endpoint.group.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.endpoint.group.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetMacByIpInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetMacByIpInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.AltoSpceGetMacByIpOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.NetworkTrackerService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AltoSpceImpl implements AltoSpceService {

    private static final Logger LOG = LoggerFactory.getLogger(FlowManager.class);
    private SalFlowService salFlowService;
    private SalMeterService salMeterService;
    private NetworkTrackerService networkTrackerService;
    private DataBroker dataBroker;
    private FlowManager flowManager;
    private MeterManager meterManager;
    private InventoryReader inventoryReader;
    private PathComputation pathComputation;
    private HashMap<Endpoint, List<TpId>> pathHashMap = new HashMap<>();
    private Table<String, String, String> srcDstRequirementTable = HashBasedTable.create();
    private long BandwidthMap[][];
    private ConfigDSFlowManager cdsFlowManager;

    public AltoSpceImpl(SalFlowService salFlowService,
                        SalMeterService salMeterService,
                        NetworkTrackerService networkTrackerService,
                        DataBroker dataBroker) {
        this.salFlowService = salFlowService;
        this.salMeterService = salMeterService;
        this.networkTrackerService = networkTrackerService;
        this.dataBroker = dataBroker;
        this.meterManager = new MeterManager(salMeterService);
        this.flowManager = new FlowManager(salFlowService, this.meterManager);
        this.inventoryReader = new InventoryReader(dataBroker);
        this.pathComputation = new PathComputation(networkTrackerService);
        this.cdsFlowManager = new ConfigDSFlowManager(dataBroker);
    }

    @Override
    public Future<RpcResult<RateLimitingSetupOutput>> rateLimitingSetup(RateLimitingSetupInput input) {
        Endpoint endpoint = input.getEndpoint();
        String src = endpoint.getSrc().getValue();
        String dst = endpoint.getDst().getValue();
        int limitedRate = input.getLimitedRate();
        int burstSize = input.getBurstSize();
        LOG.info(String.format(
                "In rateLimitingSetup, src is %s, dst is %s, the limited rate is %d, and the burst size is %d"
                , src, dst, limitedRate, burstSize));
        ErrorCodeType errorCode = //setupPathWithMeter(endpoint, limitedRate, burstSize, pathHashMap.get(endpoint));
                limitPathRate(endpoint, limitedRate, burstSize);
        RateLimitingSetupOutput output = new RateLimitingSetupOutputBuilder()
                .setErrorCode(errorCode)
                .setPath(pathToString(endpoint, pathHashMap.get(endpoint))).build();
        if (errorCode == ErrorCodeType.OK) {
            srcDstRequirementTable.put(src, dst, Integer.toString(limitedRate) + ':' + Integer.toString(burstSize));
        }
        return RpcResultBuilder.success(output).buildFuture();
    }

    private ErrorCodeType limitPathRate(Endpoint endpoint, int limitedRate, int burstSize) {
        LOG.info("In limit path rate");
        //List<TpId> path = new LinkedList<>(pathHashMap.get(endpoint));
        //LOG.info("Path without meter is" + path.toString());
        ErrorCodeType errorCode = removePathForMeterSetup(endpoint);
        //We add the new flows with meter so path must be added back
        //pathHashMap.put(endpoint, path);
        List<TpId> path = pathHashMap.get(endpoint);
        LOG.info("Path without meter is" + path.toString());
        if (errorCode== ErrorCodeType.ERROR) {
            return ErrorCodeType.ERROR;
        } else {
            List<NodeConnectorRef> nodeConnectorRefs = new LinkedList<>();
            List<TpId> tpIds = path;
                    // parseTpIds(path);
            for (TpId tpid : tpIds) {
                String nc_value = tpid.getValue();
                InstanceIdentifier<NodeConnector> ncid = InstanceIdentifier.builder(Nodes.class)
                        .child(
                                Node.class,
                                new NodeKey(new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')))))
                        .child(
                                NodeConnector.class,
                                new NodeConnectorKey(new NodeConnectorId(nc_value)))
                        .build();
                nodeConnectorRefs.add(new NodeConnectorRef(ncid));
            }
            LOG.info("Setup a path with rate limiting");
            meterManager.addDropMeterByPath(endpoint.getSrc().getValue(), endpoint.getDst().getValue(),
                    limitedRate, burstSize, nodeConnectorRefs);
            String src = endpoint.getSrc().getValue();
            String dst = endpoint.getDst().getValue();
            LOG.info(String.format("In limitPathRate(), have added meter from %s to %s", src, dst));
            //EndpointBuilder endpointBuilder = new EndpointBuilder()
            //        .setSrc(new Ipv4Address(src))
            //        .setDst(new Ipv4Address(dst));

            //Endpoint endpoint = endpointBuilder.build();
            ErrorCodeType setPathWithMeterCode = setupPathWithMeter(endpoint, limitedRate, burstSize, path);
            if (setPathWithMeterCode==ErrorCodeType.ERROR)
                return ErrorCodeType.ERROR;
            srcDstRequirementTable.put(src, dst, Integer.toString(limitedRate) + ':' + Integer.toString(burstSize));
        }
        return errorCode;
    }

    @Override
    public Future<RpcResult<RateLimitingRemoveOutput>> rateLimitingRemove(RateLimitingRemoveInput input) {
        String path = input.getPath();
        //long dropRate = input.getLimitedRate();
        //long burstSize = input.getBurstSize();
        String src = path.substring(0, path.indexOf('|'));
        String dst = path.substring(path.lastIndexOf('|')+1);
        Endpoint endpoint = new EndpointBuilder()
                .setSrc(new Ipv4Address(src))
                .setDst(new Ipv4Address(dst))
                .build();
        String oldDropRateBurstSizeString = srcDstRequirementTable.get(src, dst);
        String oldDropRateString = oldDropRateBurstSizeString.substring(0, oldDropRateBurstSizeString.indexOf(':'));
        String oldBurstSizeString = oldDropRateBurstSizeString.substring(oldDropRateBurstSizeString.indexOf(':')+1);
        int oldDropRate = Integer.parseInt(oldDropRateString);
        int oldBurstSize = Integer.parseInt(oldBurstSizeString);
        ErrorCodeType errorCode = removePathWithMeter(endpoint, oldDropRate, oldBurstSize);
        if (errorCode == ErrorCodeType.OK) {
            srcDstRequirementTable.remove(src, dst);
            srcDstRequirementTable.put(src, dst, "-1:-1");
        }
        RateLimitingRemoveOutputBuilder rlrob = new RateLimitingRemoveOutputBuilder()
                .setErrorCode(errorCode);
        return RpcResultBuilder.success(rlrob.build()).buildFuture();
    }

    public Future<RpcResult<AltoSpceRemoveOutput>> altoSpceRemove(AltoSpceRemoveInput input) {
        String path = input.getPath();
        ErrorCodeType errorCode = removePathNew(path);

        AltoSpceRemoveOutput output = new AltoSpceRemoveOutputBuilder()
                .setErrorCode(errorCode)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<GetPathOutput>> getPath(GetPathInput input) {
        Endpoint endpoint = input.getEndpoint();
        List<TpId> path = pathHashMap.get(endpoint);
        GetPathOutput output;
        if (null == path) {
            output = new GetPathOutputBuilder()
                    .setPath("NoPathAvailable")
                    .setBandwidth(-1)
                    .setBurstSize(-1)
                    .setErrorCode(ErrorCodeType.ERROR)
                    .build();
        } else {
            String src = endpoint.getSrc().getValue();
            String dst = endpoint.getDst().getValue();
            String oldDropRateBurstSizeString = srcDstRequirementTable.get(src, dst);
            String oldDropRateString = oldDropRateBurstSizeString.substring(0, oldDropRateBurstSizeString.indexOf(':'));
            String oldBurstSizeString = oldDropRateBurstSizeString.substring(oldDropRateBurstSizeString.indexOf(':')+1);
            output = new GetPathOutputBuilder()
                    .setPath(pathToString(endpoint, path))
                    .setBandwidth(Integer.valueOf(Integer.parseInt(oldDropRateString)))
                    .setBurstSize(Integer.valueOf(Integer.parseInt(oldBurstSizeString)))
                    .setErrorCode(ErrorCodeType.OK)
                    .build();
        }
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<GetTpBandwidthMapOutput>> getTpBandwidthMap() {
        Topology topology = null;
        String result = "GetTpBandwidthMapError";
        try {
            ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction();

            InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifier
                    .builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId("flow:1")))
                    .build();

            Optional<Topology> dataFuture = readTx.read(LogicalDatastoreType.OPERATIONAL,
                    topologyInstanceIdentifier).get();
            topology = dataFuture.get();
        } catch (Exception e) {
            LOG.info("Exception occurs when get topology: " + e.getMessage());
        }

        GetTpBandwidthMapOutputBuilder outputBuilder = new GetTpBandwidthMapOutputBuilder();

        if (null == topology) {
            outputBuilder
                    .setErrorCode(ErrorCodeType.ERROR)
                    .setTpBandwidthMap(result);
        } else {
            TpBandwidthMap tpBandwidthMap = new TpBandwidthMap(topology, networkTrackerService);
            String tpBandwidthMapString = tpBandwidthMap.getTpBandwidthMap();
            if (null != tpBandwidthMapString) {
                outputBuilder
                        .setErrorCode(ErrorCodeType.OK)
                        .setTpBandwidthMap(tpBandwidthMapString);
            } else {
                outputBuilder.setErrorCode(ErrorCodeType.ERROR);
            }
        }
        return RpcResultBuilder.success(outputBuilder.build()).buildFuture();

    }

    @Override
    public Future<RpcResult<GetBandwidthTopologyOutput>> getBandwidthTopology() {

        Topology topology = null;
        String tpIdMap = null;
        String bandwidthTopologyMap = null;
        String result = "GetBandwidthTopologyError";
        try {
            ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction();

            InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifier
                    .builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId("flow:1")))
                    .build();

            Optional<Topology> dataFuture = readTx.read(LogicalDatastoreType.OPERATIONAL,
                    topologyInstanceIdentifier).get();
            topology = dataFuture.get();
        } catch (Exception e) {
            LOG.info("Exception occurs when get topology: " + e.getMessage());
        }

        GetBandwidthTopologyOutputBuilder outputBuilder = new GetBandwidthTopologyOutputBuilder();

        if (null == topology) {
            outputBuilder
                    .setErrorCode(ErrorCodeType.ERROR)
                    .setBandwidthTopology("GetBandwidthTopologyError");
        } else {
            BandwidthTopology bt = new BandwidthTopology(topology, networkTrackerService);;
            tpIdMap = bt.getTpIdMap();
            bandwidthTopologyMap = bt.getBandwidthMap();
            if (null == tpIdMap || null == bandwidthTopologyMap) {
                outputBuilder.setErrorCode(ErrorCodeType.ERROR)
                        .setBandwidthTopology("GetBandwidthTopologyError")
                        .setTpidMap("GetTpIdMapError");
            } else {
                outputBuilder.setBandwidthTopology(bandwidthTopologyMap).setTpidMap(tpIdMap).setErrorCode(ErrorCodeType.OK);
            }
        }
        return RpcResultBuilder.success(outputBuilder.build()).buildFuture();
    }

    public Future<RpcResult<AltoSpceSetupOutput>> altoSpceSetup(AltoSpceSetupInput input) {
        Endpoint endpoint = input.getEndpoint();
        List<AltoSpceMetric> altoSpceMetrics = input.getObjectiveMetrics();
        List<ConstraintMetric> constraintMetrics = compressConstraint(input.getConstraintMetric());
        List<TpId> path = null;
        ErrorCodeType errorCode = ErrorCodeType.ERROR;

        if (constraintMetrics != null) {
            path = computePath(endpoint, altoSpceMetrics, constraintMetrics);
            if (null == path) {
                AltoSpceSetupOutput output = new AltoSpceSetupOutputBuilder()
                        .setPath("No available path")
                        .setErrorCode(ErrorCodeType.ERROR)
                        .build();
                return RpcResultBuilder.success(output).buildFuture();
            }
            errorCode = setupPath(endpoint, path);
        }

        AltoSpceSetupOutput output = new AltoSpceSetupOutputBuilder()
                .setPath(pathToString(endpoint, path))
                .setErrorCode(errorCode)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RateLimitingUpdateOutput>> rateLimitingUpdate(RateLimitingUpdateInput input) {
        Endpoint endpoint = input.getEndpoint();
        ErrorCodeType errorCode = ErrorCodeType.OK;
        String src = endpoint.getSrc().getValue();
        String dst = endpoint.getDst().getValue();
        String oldDropRateBurstSizeString = srcDstRequirementTable.get(src, dst);
        if (oldDropRateBurstSizeString!=null && !oldDropRateBurstSizeString.equals("-1:-1")) {
            String oldDropRateString = oldDropRateBurstSizeString.substring(0, oldDropRateBurstSizeString.indexOf(':'));
            String oldBurstSizeString = oldDropRateBurstSizeString.substring(oldDropRateBurstSizeString.indexOf(':') + 1);
            int oldDropRate = Integer.parseInt(oldDropRateString);
            int oldBurstSize = Integer.parseInt(oldBurstSizeString);
            ErrorCodeType removeErrorCode = removePathWithMeter(endpoint, oldDropRate, oldBurstSize);
            if (removeErrorCode == ErrorCodeType.OK) {
                srcDstRequirementTable.remove(src, dst);
            } else {
                errorCode = ErrorCodeType.ERROR;
            }
        }
        int newDropRate = input.getLimitedRate();
        int newBurstSize = input.getBurstSize();
        ErrorCodeType setupErrorCode = limitPathRate(endpoint, newDropRate, newBurstSize);
        if (/*removeErrorCode == ErrorCodeType.ERROR || */setupErrorCode == ErrorCodeType.ERROR ) {
            errorCode = ErrorCodeType.ERROR;
        }
        RateLimitingUpdateOutput output = new RateLimitingUpdateOutputBuilder()
                .setErrorCode(errorCode)
                .setPath(pathToString(endpoint, pathHashMap.get(endpoint)))
                .build();
        if (errorCode == ErrorCodeType.OK) {
            srcDstRequirementTable.put(src, dst, Integer.toString(newDropRate) + ':' + Integer.toString(newBurstSize));
        }
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<AddFlowToSwitchOutput>> addFlowToSwitch(AddFlowToSwitchInput input) {
        String switchId = input.getSwitchId();
        NodeId nodeId = new NodeId(switchId);
        this.cdsFlowManager.addFlow(nodeId,null);
        AddFlowToSwitchOutput output = new AddFlowToSwitchOutputBuilder().setErrorCode(ErrorCodeType.OK).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RemoveAllFlowsToSwitchOutput>> removeAllFlowsToSwitch(RemoveAllFlowsToSwitchInput input) {
        String switchId = input.getSwitchId();
        NodeId nodeId = new NodeId(switchId);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, new NodeKey(nodeId)).build();
        this.cdsFlowManager.removeFlows(new NodeRef(nodeInstanceIdentifier));
        RemoveAllFlowsToSwitchOutput output = new RemoveAllFlowsToSwitchOutputBuilder().setErrorCode(ErrorCodeType.OK).build();
        return RpcResultBuilder.success(output).buildFuture();
 }

    private List<ConstraintMetric> compressConstraint(List<ConstraintMetric> constraintMetrics) {
        if (constraintMetrics == null)
            return null;
        List<ConstraintMetric> compressedConstraintMetrics = new LinkedList<>();
        BigInteger minHopcount = BigInteger.ZERO;
        BigInteger maxHopcount = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger minBandwidth = BigInteger.ZERO;
        BigInteger maxBandwidth = BigInteger.valueOf(Long.MAX_VALUE);
        for (ConstraintMetric constraintMetric : constraintMetrics) {
            if (constraintMetric.getMetric() == AltoSpceMetric.Hopcount) {
                minHopcount = minHopcount.max(constraintMetric.getMin());
                maxHopcount = maxHopcount.min(constraintMetric.getMax());
                if (minHopcount.compareTo(maxHopcount) == 1) {
                    return null;
                }
            } else if (constraintMetric.getMetric() == AltoSpceMetric.Bandwidth) {
                minBandwidth = minBandwidth.max(constraintMetric.getMin());
                maxBandwidth = maxBandwidth.min(constraintMetric.getMax());
                if (minBandwidth.compareTo(maxBandwidth) == 1) {
                    return null;
                }
            }
        }
        compressedConstraintMetrics.add(new ConstraintMetricBuilder()
                .setMetric(AltoSpceMetric.Hopcount)
                .setMin(minHopcount)
                .setMax(maxHopcount)
                .build());
        compressedConstraintMetrics.add(new ConstraintMetricBuilder()
                .setMetric(AltoSpceMetric.Bandwidth)
                .setMin(minBandwidth)
                .setMax(maxBandwidth)
                .build());
        return compressedConstraintMetrics;
    }

    private Match parseMacMatch(Endpoint endpoint) {
        //String[] tpList = path.split("\\|");
        MacAddress srcEth = ipToMac(new Ipv4Address(endpoint.getSrc().getValue()));
        MacAddress dstEth = ipToMac(new Ipv4Address(endpoint.getDst().getValue()));
        if (srcEth == null | dstEth == null) {
            return null;
        }
        return new MatchBuilder()
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetSource(new EthernetSourceBuilder()
                                .setAddress(srcEth)
                                .build())
                        .setEthernetDestination(new EthernetDestinationBuilder()
                                .setAddress(dstEth)
                                .build())
                        .build())
                .build();
    }

    private Match parseIpMatch(Endpoint endpoint) {
        //String[] tpList = path.split("\\|");
        Ipv4Prefix srcIp = new Ipv4Prefix(endpoint.getSrc().getValue() + "/32");
        Ipv4Prefix dstIp = new Ipv4Prefix(endpoint.getDst().getValue() + "/32");
        if (srcIp == null | dstIp == null) {
            return null;
        }
        return new MatchBuilder()
                .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Source(srcIp)
                        .setIpv4Destination(dstIp)
                        .build())
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetType(new EthernetTypeBuilder()
                                .setType(new EtherType(0x0800L))
                                .build())
                        .build())
                .build();
    }

    private Match parseMacMatch(String path) {
        String[] tpList = path.split("\\|");
        MacAddress srcEth = ipToMac(new Ipv4Address(tpList[0]));
        MacAddress dstEth = ipToMac(new Ipv4Address(tpList[tpList.length - 1]));
        if (srcEth == null | dstEth == null) {
            return null;
        }
        return new MatchBuilder()
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetSource(new EthernetSourceBuilder()
                                .setAddress(srcEth)
                                .build())
                        .setEthernetDestination(new EthernetDestinationBuilder()
                                .setAddress(dstEth)
                                .build())
                        .build())
                .build();
    }

    private Match parseIpMatch(String path) {
        String[] tpList = path.split("\\|");
        Ipv4Prefix srcIp = new Ipv4Prefix(tpList[0] + "/32");
        Ipv4Prefix dstIp = new Ipv4Prefix(tpList[tpList.length - 1] + "/32");
        if (srcIp == null | dstIp == null) {
            return null;
        }
        return new MatchBuilder()
                .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Source(srcIp)
                        .setIpv4Destination(dstIp)
                        .build())
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetType(new EthernetTypeBuilder()
                                .setType(new EtherType(0x0800L))
                                .build())
                        .build())
                .build();
    }

    private List<TpId> parseTpIds(String path) {
        List<TpId> tpIds = new LinkedList<>();
        String[] tpList = path.split("\\|");
        for (int i = 1; i < tpList.length -1; i++) {
            tpIds.add(new TpId(tpList[i]));
        }
        return tpIds;
    }

    private ErrorCodeType removePath(Endpoint endpoint) {
        List<TpId> tpIds = pathHashMap.get(endpoint);
        Match macMatch = parseMacMatch(endpoint);
        Match ipMatch = parseIpMatch(endpoint);
        if (macMatch == null | ipMatch == null) {
            return ErrorCodeType.ERROR;
        }
        try {
            for (TpId tpId : tpIds) {
                NodeRef nodeRef =
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(
                                        new NodeId(tpId.getValue().substring(0, tpId.getValue().lastIndexOf(":")))))
                                .build());
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(macMatch)
                        .setNode(nodeRef)
                        .setTransactionUri(tpId)
                        .build()
                );
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(ipMatch)
                        .setNode(nodeRef)
                        .build()
                );
            }
        } catch (Exception e) {
            LOG.info("Exception occurs when remove a path: " + e.getMessage());
            return ErrorCodeType.ERROR;
        }
        pathHashMap.remove(endpoint);
        return ErrorCodeType.OK;
    }

    private ErrorCodeType removePathForMeterSetup(Endpoint endpoint) {
        List<TpId> tpIds = pathHashMap.get(endpoint);
        Match macMatch = parseMacMatch(endpoint);
        Match ipMatch = parseIpMatch(endpoint);
        if (macMatch == null | ipMatch == null) {
            return ErrorCodeType.ERROR;
        }
        try {
            for (TpId tpId : tpIds) {
                NodeRef nodeRef =
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(
                                        new NodeId(tpId.getValue().substring(0, tpId.getValue().lastIndexOf(":")))))
                                .build());
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(macMatch)
                        .setNode(nodeRef)
                        .setTransactionUri(tpId)
                        .build()
                );
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(ipMatch)
                        .setNode(nodeRef)
                        .build()
                );
            }
        } catch (Exception e) {
            LOG.info("Exception occurs when remove a path: " + e.getMessage());
            return ErrorCodeType.ERROR;
        }
        //pathHashMap.remove(endpoint);
        return ErrorCodeType.OK;
    }

    private ErrorCodeType removePathWithMeter(Endpoint endpoint, long dropRate, long burstSize) {

        List<TpId> tpIds = pathHashMap.get(endpoint);
        Match macMatch = parseMacMatch(endpoint);
        Match ipMatch = parseIpMatch(endpoint);
        if (macMatch == null | ipMatch == null) {
            return ErrorCodeType.ERROR;
        }
        try {
            for (TpId tpId : tpIds) {
                NodeRef nodeRef =
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(
                                        new NodeId(tpId.getValue().substring(0, tpId.getValue().lastIndexOf(":")))))
                                .build());
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(macMatch)
                        .setNode(nodeRef)
                        .setTransactionUri(tpId)
                        .build()
                );
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(ipMatch)
                        .setNode(nodeRef)
                        .build()
                );
                String nc_value = tpId.getValue();
                InstanceIdentifier<NodeConnector> ncid = InstanceIdentifier.builder(Nodes.class)
                        .child(
                                Node.class,
                                new NodeKey(new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')))))
                        .child(
                                NodeConnector.class,
                                new NodeConnectorKey(new NodeConnectorId(nc_value)))
                        .build();
                meterManager.removeMeterFromSwitch(endpoint, new NodeConnectorRef(ncid), nodeRef, dropRate, burstSize);
                /*this.salMeterService.removeMeter(new RemoveMeterInputBuilder()
                    .setMeterId(new MeterId(this.meterManager.getPerFlowMeterId(
                        new NodeConnectorRef(ncid),
                        endpoint.getSrc().getValue(),
                        endpoint.getDst().getValue(),
                        dropRate,
                        burstSize)))
                    .setNode(nodeRef)
                    .build());*/
            }
            setupPath(endpoint, pathHashMap.get(endpoint));
        } catch (Exception e) {
            LOG.info("Exception occurs when remove a path: " + e.getMessage());
            return ErrorCodeType.ERROR;
        }
        //pathHashMap.remove(endpoint);
        return ErrorCodeType.OK;
    }

    private ErrorCodeType removePath(String path) {
        String src = path.substring(0, path.indexOf('|'));
        String dst = path.substring(path.lastIndexOf('|')+1);
        LOG.info(String.format("In removePath() from %s to %s", src, dst));
        EndpointBuilder endpointBuilder = new EndpointBuilder()
                .setSrc(new Ipv4Address(src))
                .setDst(new Ipv4Address(dst));
        List<TpId> tpIds = parseTpIds(path);
        Match macMatch = parseMacMatch(path);
        Match ipMatch = parseIpMatch(path);
        if (macMatch == null | ipMatch == null) {
            return ErrorCodeType.ERROR;
        }
        try {
            for (TpId tpId : tpIds) {
                NodeRef nodeRef =
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(
                                        new NodeId(tpId.getValue().substring(0, tpId.getValue().lastIndexOf(":")))))
                                .build());
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(macMatch)
                        .setNode(nodeRef)
                        .setTransactionUri(tpId)
                        .build()
                );
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(ipMatch)
                        .setNode(nodeRef)
                        .build()
                );
            }
        } catch (Exception e) {
            LOG.info("Exception occurs when remove a path: " + e.getMessage());
            return ErrorCodeType.ERROR;
        }
        pathHashMap.remove(endpointBuilder.build());
        return ErrorCodeType.OK;
    }

    private ErrorCodeType removePathNew(String path) {
        String src = path.substring(0, path.indexOf('|'));
        String dst = path.substring(path.lastIndexOf('|')+1);
        LOG.info(String.format("In removePath() from %s to %s", src, dst));
        EndpointBuilder endpointBuilder = new EndpointBuilder()
                .setSrc(new Ipv4Address(src))
                .setDst(new Ipv4Address(dst));
        List<TpId> tpIds = parseTpIds(path);
        Match macMatch = parseMacMatch(path);
        Match ipMatch = parseIpMatch(path);
        if (macMatch == null | ipMatch == null) {
            return ErrorCodeType.ERROR;
        }
        try {
            for (TpId tpId : tpIds) {
                NodeRef nodeRef =
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(
                                        new NodeId(tpId.getValue().substring(0, tpId.getValue().lastIndexOf(":")))))
                                .build());
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(macMatch)
                        .setNode(nodeRef)
                        .setTransactionUri(tpId)
                        .build()
                );
                this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                        .setMatch(ipMatch)
                        .setNode(nodeRef)
                        .build()
                );
            }
        } catch (Exception e) {
            LOG.info("Exception occurs when remove a path: " + e.getMessage());
            return ErrorCodeType.ERROR;
        }
        if ((srcDstRequirementTable.get(src, dst)!=null)&&(!srcDstRequirementTable.get(src, dst).equals("-1:-1"))) {
            Endpoint endpoint = endpointBuilder.build();
            String oldDropRateBurstSizeString = srcDstRequirementTable.get(src, dst);
            String oldDropRateString = oldDropRateBurstSizeString.substring(0, oldDropRateBurstSizeString.indexOf(':'));
            String oldBurstSizeString = oldDropRateBurstSizeString.substring(oldDropRateBurstSizeString.indexOf(':')+1);
            int oldDropRate = Integer.parseInt(oldDropRateString);
            int oldBurstSize = Integer.parseInt(oldBurstSizeString);
            srcDstRequirementTable.remove(src, dst);
            try {
                for (TpId tpId : tpIds) {
                    NodeRef nodeRef =
                            new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                    .child(Node.class, new NodeKey(
                                            new NodeId(tpId.getValue().substring(0, tpId.getValue().lastIndexOf(":")))))
                                    .build());
                    String nc_value = tpId.getValue();
                    InstanceIdentifier<NodeConnector> ncid = InstanceIdentifier.builder(Nodes.class)
                            .child(
                                    Node.class,
                                    new NodeKey(new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')))))
                            .child(
                                    NodeConnector.class,
                                    new NodeConnectorKey(new NodeConnectorId(nc_value)))
                            .build();
                    meterManager.removeMeterFromSwitch(endpoint, new NodeConnectorRef(ncid), nodeRef, oldDropRate, oldBurstSize);
                }
            } catch (Exception e) {
                LOG.info("Exception occurs when remove a path: " + e.getMessage());
                return ErrorCodeType.ERROR;
            }
        }
        if ((srcDstRequirementTable.get(src, dst)!=null)) {
            srcDstRequirementTable.remove(src,dst);
        }
        pathHashMap.remove(endpointBuilder.build());
        return ErrorCodeType.OK;
    }

    private List<TpId> computePath(Endpoint endpoint,
                               List<AltoSpceMetric> altoSpceMetrics,
                               List<ConstraintMetric> constraintMetrics) {
        List<TpId> path = null;
        TpId srcTpId = getAttachTp(endpoint.getSrc());
        TpId dstTpId = getAttachTp(endpoint.getDst());
        Topology topology = getTopology();

        try {
            if (altoSpceMetrics.get(0) == AltoSpceMetric.Bandwidth) {
                path = pathComputation.maxBandwidthPath(srcTpId, dstTpId, topology, constraintMetrics);
            } else if (altoSpceMetrics.get(0) == AltoSpceMetric.Hopcount) {
                path = pathComputation.shortestPath(srcTpId, dstTpId, topology, constraintMetrics);
            }
        } catch (Exception e) {
            LOG.info("Exception occurs when compute path: " + e.getMessage());
        }

        return path;
    }

    private TpId getAttachTp(Ipv4Address src) {
        return this.inventoryReader.getNodeConnectorByMac(ipToMac(src));
    }

    private MacAddress ipToMac(Ipv4Address src) {
        MacAddress mac = null;
        AltoSpceGetMacByIpInput input = new AltoSpceGetMacByIpInputBuilder()
                .setIpAddress(src.getValue())
                .build();
        LOG.info("In ipToMac, IP is " + src.getValue());
        Future<RpcResult<AltoSpceGetMacByIpOutput>> result = this.networkTrackerService.altoSpceGetMacByIp(input);
        try {
            AltoSpceGetMacByIpOutput output = result.get().getResult();
            mac = new MacAddress(output.getMacAddress());
        } catch (InterruptedException | ExecutionException e) {
            LOG.info("Exception occurs when convert ip to mac: " + e.getMessage());
        }
        return mac;
    }

    private ErrorCodeType setupPath(Endpoint endpoint, List<TpId> path) {
        LOG.info("In default setupPath, the path is " + path.toString());
        if (path == null) {
            LOG.info("Setup Error: path is null.");
            return ErrorCodeType.ERROR;
        }

        try {
            Ipv4Address srcIp = endpoint.getSrc();
            Ipv4Address dstIp = endpoint.getDst();
            MacAddress srcMac = ipToMac(srcIp);
            MacAddress dstMac = ipToMac(dstIp);
            List<NodeConnectorRef> nodeConnectorRefs = new LinkedList<>();
            for (TpId tpid : path) {
                String nc_value = tpid.getValue();
                InstanceIdentifier<NodeConnector> ncid = InstanceIdentifier.builder(Nodes.class)
                        .child(
                                Node.class,
                                new NodeKey(new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')))))
                        .child(
                                NodeConnector.class,
                                new NodeConnectorKey(new NodeConnectorId(nc_value)))
                        .build();
                nodeConnectorRefs.add(new NodeConnectorRef(ncid));
            }
            LOG.info("Setup a path: srcIp=" + srcIp.getValue() + ", dstIp=" + dstIp.getValue());
            LOG.info("Setup a path: srcMac=" + srcMac.getValue() + ", dstMac=" + dstMac.getValue());
            LOG.info("Setup a path, and nodeConnectorRefs is" + nodeConnectorRefs.toString());
            this.flowManager.addFlowByPath(srcIp, dstIp, nodeConnectorRefs);
            //this.flowManager.addFlowByPath(srcMac, dstMac, nodeConnectorRefs);
        } catch (Exception e) {
            LOG.info("Exception occurs when setup a path: " + e.getMessage());
            return ErrorCodeType.ERROR;
        }
        pathHashMap.put(endpoint, path);
        srcDstRequirementTable.put(endpoint.getSrc().getValue(), endpoint.getDst().getValue(), "-1:-1");
        LOG.info("Update pathHashMap" + path.toString());
        return ErrorCodeType.OK;
    }

    private ErrorCodeType setupPathWithMeter(Endpoint endpoint, long dropRate, long dropBurstSize, List<TpId> path) {
        LOG.info("In setupPathWithMeter, the path is " + path.toString());
        if (path == null) {
            LOG.info("Setup Error: path is null.");
            return ErrorCodeType.ERROR;
        }

        try {
            Ipv4Address srcIp = endpoint.getSrc();
            Ipv4Address dstIp = endpoint.getDst();
            MacAddress srcMac = ipToMac(srcIp);
            MacAddress dstMac = ipToMac(dstIp);
            List<NodeConnectorRef> nodeConnectorRefs = new LinkedList<>();
            for (TpId tpid : path) {
                String nc_value = tpid.getValue();
                InstanceIdentifier<NodeConnector> ncid = InstanceIdentifier.builder(Nodes.class)
                        .child(
                                Node.class,
                                new NodeKey(new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')))))
                        .child(
                                NodeConnector.class,
                                new NodeConnectorKey(new NodeConnectorId(nc_value)))
                        .build();
                nodeConnectorRefs.add(new NodeConnectorRef(ncid));
            }

            LOG.info("Setup a path with rate limiting" + dropRate + ": srcIp=" + srcIp.getValue() + ", dstIp=" + dstIp.getValue());
            LOG.info("Setup a path with rate limiting" + dropRate + ": srcMac=" + srcMac.getValue() + ", dstMac=" + dstMac.getValue());
            LOG.info("Setup a path with rate limiting, and" +
                    " nodeConnectorRefs is" + nodeConnectorRefs.toString());
            this.flowManager.addFlowByPathWithMeter(srcIp, dstIp, dropRate, dropBurstSize, nodeConnectorRefs);
            LOG.info("add IP By path with meter finished");
            //this.flowManager.addFlowByPathWithMeter(srcMac, dstMac, dropRate, dropBurstSize, nodeConnectorRefs);
            LOG.info("add MAC By path with meter finished");
        } catch (Exception e) {
            LOG.info("Exception occurs when setup a path: " + e.getMessage());
            throw e;
            //return ErrorCodeType.ERROR;
        }
        pathHashMap.put(endpoint, path);
        LOG.info("Update pathHashMap" + path.toString());
        return ErrorCodeType.OK;
    }

    private String pathToString(Endpoint endpoint, List<TpId> path) {
        String pathString = endpoint.getSrc().getValue();
        if (path != null) {
            for (TpId tpId : path) {
                pathString += "|" + tpId.getValue();
            }
        }
        pathString += "|" + endpoint.getDst().getValue();
        return pathString;
    }

    private Topology getTopology() {
        try {
            ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction();

            InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifier
                    .builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId("flow:1")))
                    .build();

            Optional<Topology> dataFuture = readTx.read(LogicalDatastoreType.OPERATIONAL,
                    topologyInstanceIdentifier).get();

            return dataFuture.get();
        } catch (Exception e) {
            LOG.info("Exception occurs when get topology: " + e.getMessage());
        }
        return null;
    }
}
