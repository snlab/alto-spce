/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.scheduler;

/**
 * Created by qiao on 11/16/15.
 */
public class ConfigurationOptions {

    public static final boolean DEBUG = false;

    /*File slicing mode*/
    public static final boolean FILE_SLICING_DISABLED = false;
    public static final boolean FILE_SLICING_ENABLED = true;

    /*Path selection mode*/
    public static final boolean PATH_SELECTION_DISABLED = false;
    public static final boolean PATH_SELECTION_ENABLED = true;

    /*Replica selection mode, only needed if file slicing is disabled*/
    public static final int MINHOP_REPLICA = 0;
    public static final int ENUM_REPLICA = 1;
    public static final int HERUISTIC_REPLICA = 2;

    /*Scheduling mode*/
    public static final int ONLINE_OMFRA = 0;
    public static final int OFFLINE_OMFRA = 1;

    public ConfigurationOptions() {}
}
