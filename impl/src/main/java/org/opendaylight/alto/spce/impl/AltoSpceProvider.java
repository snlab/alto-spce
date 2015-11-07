/*
 * Copyright © 2015 Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.spce.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.spce.rev151106.AltoSpceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AltoSpceProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AltoSpceProvider.class);
    private RpcRegistration<AltoSpceService> altoSpceService;

    public void onSessionInitiated(ProviderContext session) {
        LOG.info("AltoSpceProvider Session Initiated!");
        altoSpceService = session.addRpcImplementation(AltoSpceService.class, new AltoSpceImpl());
    }

    @Override
    public void close() throws Exception {
        LOG.info("AltoSpceProvider Closed!");
        if (altoSpceService != null) {
            altoSpceService.close();
        }
    }
}
