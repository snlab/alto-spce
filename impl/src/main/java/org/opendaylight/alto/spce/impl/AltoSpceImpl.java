/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl;

import com.google.common.base.Optional;
import org.opendaylight.alto.spce.impl.algorithm.PathComputation;
import org.opendaylight.alto.spce.impl.util.FlowManager;
import org.opendaylight.alto.spce.impl.util.InventoryReader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceRemoveInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceRemoveOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceRemoveOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceSetupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceSetupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.ErrorCodeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.alto.spce.setup.input.ConstraintMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.alto.spce.setup.input.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.tracker.rev151107.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AltoSpceImpl implements AltoSpceService {

    private SalFlowService salFlowService;
    private NetworkTrackerService networkTrackerService;
    private DataBroker dataBroker;
    private FlowManager flowManager;
    private InventoryReader inventoryReader;
    private PathComputation pathComputation;

    public AltoSpceImpl(SalFlowService salFlowService,
                        NetworkTrackerService networkTrackerService,
                        DataBroker dataBroker) {
        this.salFlowService = salFlowService;
        this.networkTrackerService = networkTrackerService;
        this.dataBroker = dataBroker;
        this.flowManager = new FlowManager(salFlowService);
        this.inventoryReader = new InventoryReader(dataBroker);
        this.pathComputation = new PathComputation(networkTrackerService);
    }

    public Future<RpcResult<AltoSpceRemoveOutput>> altoSpceRemove(AltoSpceRemoveInput input) {
        String path = input.getPath();
        ErrorCodeType errorCode = removePath(path);

        AltoSpceRemoveOutput output = new AltoSpceRemoveOutputBuilder()
                .setErrorCode(errorCode)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    public Future<RpcResult<AltoSpceSetupOutput>> altoSpceSetup(AltoSpceSetupInput input) {
        Endpoint endpoint = input.getEndpoint();
        List<AltoSpceMetric> altoSpceMetrics = input.getObjectiveMetrics();
        List<ConstraintMetric> constraintMetrics = input.getConstraintMetric();

        List<TpId> path = computePath(endpoint, altoSpceMetrics, constraintMetrics);
        ErrorCodeType errorCode = setupPath(endpoint, path);

        AltoSpceSetupOutput output = new AltoSpceSetupOutputBuilder()
                .setPath(pathToString(endpoint, path))
                .setErrorCode(errorCode)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    private Match parseMatch(String path) {
        String[] tpList = path.split("|");
        MacAddress srcEth = ipToMac(new Ipv4Address(tpList[0]));
        MacAddress dstEth = ipToMac(new Ipv4Address(tpList[tpList.length - 1]));
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

    private List<TpId> parseTpIds(String path) {
        List<TpId> tpIds = new LinkedList<>();
        String[] tpList = path.split("|");
        for (int i = 1; i < tpList.length -1; i++) {
            tpIds.add(new TpId(tpList[i]));
        }
        return tpIds;
    }

    private ErrorCodeType removePath(String path) {
        List<TpId> tpIds = parseTpIds(path);
        Match match = parseMatch(path);
        for (TpId tpId : tpIds) {
            this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                            .setMatch(match)
                            .setTransactionUri(tpId)
                            .build()
            );
        }
        return ErrorCodeType.OK;
    }

    private List<TpId> computePath(Endpoint endpoint,
                               List<AltoSpceMetric> altoSpceMetrics,
                               List<ConstraintMetric> constraintMetrics) {
        List<TpId> path = null;
        TpId srcTpId = getAttachTp(endpoint.getSrc());
        TpId dstTpId = getAttachTp(endpoint.getDst());
        Topology topology = getTopology();
        if (altoSpceMetrics.get(0) == AltoSpceMetric.Bandwidth) {
            path = pathComputation.maxBandwidthPath(srcTpId, dstTpId, topology, constraintMetrics);
        } else if (altoSpceMetrics.get(0) == AltoSpceMetric.Hopcount) {
            path = pathComputation.shortestPath(srcTpId, dstTpId, topology, constraintMetrics);
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
        Future<RpcResult<AltoSpceGetMacByIpOutput>> result = this.networkTrackerService.altoSpceGetMacByIp(input);
        try {
            AltoSpceGetMacByIpOutput output = result.get().getResult();
            mac = new MacAddress(output.getMacAddress());
        } catch (InterruptedException | ExecutionException e) {
        }
        return mac;
    }

    private ErrorCodeType setupPath(Endpoint endpoint, List<TpId> path) {
        MacAddress srcMac = ipToMac(endpoint.getSrc());
        MacAddress dstMac = ipToMac(endpoint.getDst());
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
        this.flowManager.addFlowByPath(srcMac, dstMac, nodeConnectorRefs);
        return ErrorCodeType.OK;
    }

    private String pathToString(Endpoint endpoint, List<TpId> path) {
        String pathString = endpoint.getSrc().getValue();
        for (TpId tpId : path) {
            pathString += "|" + tpId.getValue();
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

            Optional<Topology> dataFuture = readTx.read(LogicalDatastoreType.CONFIGURATION,
                    topologyInstanceIdentifier).get();

            return dataFuture.get();
        } catch (Exception e) {
        }
        return null;
    }
}
