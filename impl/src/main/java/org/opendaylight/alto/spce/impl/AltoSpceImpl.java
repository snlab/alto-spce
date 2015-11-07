/*
 * Copyright © 2015 Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.List;
import java.util.concurrent.Future;

public class AltoSpceImpl implements AltoSpceService {

    private SalFlowService salFlowService;
    private DataBroker dataBroker;

    public AltoSpceImpl(SalFlowService salFlowService, DataBroker dataBroker) {
        this.salFlowService = salFlowService;
        this.dataBroker = dataBroker;
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
                .setPath(pathToString(path))
                .setErrorCode(errorCode)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    private String pathToString(List<TpId> path) {
        String pathString = "|";
        for (TpId tpId : path) {
            pathString += tpId.getValue() + "|";
        }
        return pathString;
    }

    private ErrorCodeType removePath(String path) {
        List<NodeRef> nodeRefs = parseNodeRefs(path);
        Match match = parseMatch(path);
        for (NodeRef node : nodeRefs) {
            this.salFlowService.removeFlow(new RemoveFlowInputBuilder()
                            .setMatch(match)
                            .setNode(node)
                            .build()
            );
        }
        return ErrorCodeType.OK;
    }

    private Match parseMatch(String path) {
        return null;
    }

    private List<NodeRef> parseNodeRefs(String path) {
        return null;
    }

    private List<TpId> computePath(Endpoint endpoint,
                               List<AltoSpceMetric> altoSpceMetrics,
                               List<ConstraintMetric> constraintMetrics) {
        List<TpId> path = null;
        TpId srcTpId = getAttachTp(endpoint.getSrc());
        TpId dstTpId = getAttachTp(endpoint.getDst());
        Topology topology = getTopology();
        if (altoSpceMetrics.get(0) == AltoSpceMetric.Bandwidth) {
            path = maxBandwidthPath(srcTpId, dstTpId, topology, constraintMetrics);
        } else if (altoSpceMetrics.get(0) == AltoSpceMetric.Hopcount) {
            path = shortestPath(srcTpId, dstTpId, topology, constraintMetrics);
        }
        return path;
    }

    private List<TpId> shortestPath(TpId srcTpId, TpId dstTpId, Topology topology, List<ConstraintMetric> constraintMetrics) {
        return null;
    }

    private List<TpId> maxBandwidthPath(TpId srcTpId, TpId dstTpId, Topology topology, List<ConstraintMetric> constraintMetrics) {
        return null;
    }

    private TpId getAttachTp(Ipv4Address src) {
        return null;
    }

    private ErrorCodeType setupPath(Endpoint endpint, List<TpId> path) {
        Match match = null;
        for (TpId tpId : path) {
            this.salFlowService.addFlow(new AddFlowInputBuilder()
                    .setMatch(match)
                    .setNode(null)
                    .build()
            );
        }
        return null;
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
