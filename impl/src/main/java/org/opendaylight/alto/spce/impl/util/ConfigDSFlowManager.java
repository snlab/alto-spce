/*
 * Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.alto.spce.impl.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

public class ConfigDSFlowManager {

    private Logger LOG = LoggerFactory.getLogger(ConfigDSFlowManager.class);
    private DataBroker dataBroker;
    private final short DEFAULT_TABLE_ID = 0;
    private final Integer DEFAULT_PRIORITY = 20;
    private final Integer DEFAULT_HARD_TIMEOUT = 0;//3600;
    private final Integer DEFAULT_IDLE_TIMEOUT = 0;//1800;
    private final Long OFP_NO_BUFFER = Long.valueOf(4294967295L);
    private AtomicLong flowCookieInc = new AtomicLong(0x3a00000000000000L);
    private AtomicLong flowId = new AtomicLong(1);
    public ConfigDSFlowManager(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    private Flow createIpv4ToIpv4Flow(Short tableId, int priority,
                                      Ipv4Address sourceIp, Ipv4Address destIp, NodeConnectorRef destPort) {

        FlowBuilder ipToIpFlow = new FlowBuilder() //
                .setTableId(tableId) //
                .setFlowName("ip2ip")
                .setId(new FlowId(String.valueOf(flowId.getAndIncrement())));

        ipToIpFlow.setId(new FlowId(Long.toString(ipToIpFlow.hashCode())));

        Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder() //
                .setIpv4Destination(new Ipv4Prefix(destIp.getValue() + "/32"));
        if(sourceIp != null) {
            ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(sourceIp.getValue() + "/32"));
        }
        Layer3Match layer3Match = ipv4MatchBuilder.build();
        Match match = new MatchBuilder()
                .setLayer3Match(layer3Match)
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetType(new EthernetTypeBuilder()
                                .setType(new EtherType(0x0800L))
                                .build())
                        .build())
                .build();


        Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

        Action outputToControllerAction = new ActionBuilder() //
                .setOrder(0)
                .setAction(new OutputActionCaseBuilder() //
                        .setOutputAction(new OutputActionBuilder() //
                                .setMaxLength(0xffff) //
                                .setOutputNodeConnector(destPortUri) //
                                .build()) //
                        .build()) //
                .build();

        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
                .build();

        Instruction applyActionsInstruction = new InstructionBuilder() //
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()//
                        .setApplyActions(applyActions) //
                        .build()) //
                .build();

        ipToIpFlow
                .setMatch(match) //
                .setInstructions(new InstructionsBuilder() //
                        .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                        .build()) //
                .setPriority(DEFAULT_PRIORITY) //
                .setBufferId(OFP_NO_BUFFER) //
                .setHardTimeout(DEFAULT_HARD_TIMEOUT) //
                .setIdleTimeout(DEFAULT_IDLE_TIMEOUT) //
                .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        return ipToIpFlow.build();
    }


    int i = 0; //This is for test

    public void addFlow(NodeId nodeId, Flow flow) {
        ++i;
        // Build IID for a node
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, new NodeKey(nodeId)).build();
        LOG.info(nodeInstanceIdentifier.toString());

        //Build a flow. Flow id and flow cookie must be different. In this class flow id is increasing 1 every time this funciton is be called.
        String testIpv4Address = "10.10.10.";
        flow = createIpv4ToIpv4Flow(DEFAULT_TABLE_ID, 13,
                new Ipv4Address(testIpv4Address + String.valueOf(i)), //src IP
                new Ipv4Address(testIpv4Address + String.valueOf(i+1)),  //dst IP
                getNodeConnector(nodeInstanceIdentifier));  //output port

        LOG.info("flow is " + flow.toString());
        LOG.info("URL is " + getFlowInstanceIdentifier(new NodeRef(nodeInstanceIdentifier), flow).toString());

        //Begin the write transaction.
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, getFlowInstanceIdentifier(new NodeRef(nodeInstanceIdentifier), flow), flow, true);
        try {
            writeTransaction.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed: {}", e.toString());
        }
    }

    public void removeFlows(NodeRef nodeRef) {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, nodeRef.getValue());
        try {
            writeTransaction.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed: {}", e.toString());
        }
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> getFlowInstanceIdentifier(NodeRef nodeRef, Flow flow) {
        return ((InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>) nodeRef.getValue())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(DEFAULT_TABLE_ID))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow.class,
                        new FlowKey(new FlowId(flow.getId())));
    }

    //This function is only used for testing, I build a flow which output is the first port of a given node.
    private NodeConnectorRef getNodeConnector(InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeInsId) {
        if (nodeInsId == null) {
            return null;
        }
        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
        try {
            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> dataObjectOptional = null;
            dataObjectOptional = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeInsId).get();
            if (dataObjectOptional.isPresent()) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node = (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node) dataObjectOptional.get();
                //LOG.debug("Looking address in node : {}", , nodeInsId);
                for (NodeConnector nc : node.getNodeConnector()) {
                    return new NodeConnectorRef(nodeInsId.child(NodeConnector.class, nc.getKey()));

                }
            }
        } catch (InterruptedException e) {
            LOG.error("Failed to read nodes from Operation data store.");
            readOnlyTransaction.close();
            throw new RuntimeException("Failed to read nodes from Operation data store.", e);
        } catch (ExecutionException e) {
            LOG.error("Failed to read nodes from Operation data store.");
            readOnlyTransaction.close();
            throw new RuntimeException("Failed to read nodes from Operation data store.", e);
        }
        readOnlyTransaction.close();
        return null;
    }
}
