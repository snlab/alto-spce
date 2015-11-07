/*
 * Copyright © 2015 Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.List;
import java.util.concurrent.Future;

public class AltoSpceImpl implements AltoSpceService {

    private SalFlowService salFlowService;

    public AltoSpceImpl(SalFlowService salFlowService) {
        this.salFlowService  = salFlowService;
    }

    public Future<RpcResult<AltoSpceRemoveOutput>> altoSpceRemove(AltoSpceRemoveInput input) {
        String path = input.getPath();

        ErrorCodeType errorCode = removePath(path);

        AltoSpceRemoveOutput output = new AltoSpceRemoveOutputBuilder()
                .setErrorCode(errorCode)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
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

    public Future<RpcResult<AltoSpceSetupOutput>> altoSpceSetup(AltoSpceSetupInput input) {
        Endpoint endpoint = input.getEndpoint();
        List<AltoSpceMetric> altoSpceMetrics = input.getObjectiveMetrics();
        List<ConstraintMetric> constraintMetrics = input.getConstraintMetric();

        String path = computePath(endpoint, altoSpceMetrics, constraintMetrics);
        ErrorCodeType errorCode = setupPath(path);

        AltoSpceSetupOutput output = new AltoSpceSetupOutputBuilder()
                .setPath(path)
                .setErrorCode(errorCode)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    private String computePath(Endpoint endpoint, List<AltoSpceMetric> altoSpceMetrics, List<ConstraintMetric> constraintMetrics) {
        return null;
    }

    private ErrorCodeType setupPath(String path) {
        return null;
    }
}
