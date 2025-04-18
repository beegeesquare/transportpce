/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.pce.networkanalyzer.port.NoPreference;
import org.opendaylight.transportpce.pce.networkanalyzer.port.Preference;
import org.opendaylight.transportpce.pce.node.mccapabilities.NodeMcCapability;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;

@ExtendWith(MockitoExtension.class)
public class PceOpticalNodeTest extends AbstractTest {

    private PceOpticalNode pceOpticalNode;
    private Node node;
    private BitSet usedBitSet = new BitSet(8);
    private BitSet availableBitSet = new BitSet(8);
    private String deviceNodeId = "device node";
    private String serviceType = "100GE";
    @Mock
    private PortMapping portMapping;
    private final Preference portPreference = new NoPreference();

    @BeforeEach
    void setUp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.SRGTXRXPP);
        node = node1Builder.setNodeId(new NodeId("test")).build();
        availableBitSet.set(0,8);
    }

    @Test
    void isValidTest() {
        OpenroadmNodeType nodeType = OpenroadmNodeType.ROADM;
        pceOpticalNode = new PceOpticalNode(deviceNodeId, serviceType, portMapping, node, nodeType,
            StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        assertTrue(pceOpticalNode.isValid());
    }

    @Test
    void testInitSrgTps() {

        pceOpticalNode = new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.ROADM, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initSrgTps(portPreference);
        pceOpticalNode.initXndrTps(ServiceFormat.OMS);
        pceOpticalNode.initFrequenciesBitSet();
        assertFalse(pceOpticalNode.isValid());
        assertNull(pceOpticalNode.getBitSetData());
        assertTrue(pceOpticalNode.checkTP("testTP"));
        assertNull(pceOpticalNode.getAvailableTribPorts());
        assertNull(pceOpticalNode.getAvailableTribPorts());
        assertNull(pceOpticalNode.getXpdrClient("test"));
    }

    @Test
    void testInitXndrTpSrgTypes() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERNETWORK);
        Node specificNode = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(deviceNodeId, serviceType, portMapping, specificNode,
                OpenroadmNodeType.SRG, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initFrequenciesBitSet();
        assertTrue(pceOpticalNode.isValid());
        assertEquals(availableBitSet, pceOpticalNode.getBitSetData().get(88,96));
        assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    void testInitXndrTpDegTypes() {
        pceOpticalNode = new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initFrequenciesBitSet();
        assertTrue(pceOpticalNode.isValid());
        assertEquals(usedBitSet,pceOpticalNode.getBitSetData().get(88,96));
        assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    void testInitXndrTpXpondrTypes() {
        pceOpticalNode = new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.XPONDER, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initFrequenciesBitSet();
        assertTrue(pceOpticalNode.isValid());
        assertEquals(availableBitSet, pceOpticalNode.getBitSetData().get(88,96));
        assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    void testinitFrequenciesBitSet() {
        pceOpticalNode = new PceOpticalNode(null, null, null, node,
                OpenroadmNodeType.ROADM, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initXndrTps(ServiceFormat.OMS);
        pceOpticalNode.initFrequenciesBitSet();
        assertFalse(pceOpticalNode.isValid());
        assertNull(pceOpticalNode.getBitSetData());
        assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    void testGetRdmSrgClient() {
        pceOpticalNode = new PceOpticalNode(null, null, null, node,
                OpenroadmNodeType.ROADM, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initSrgTps(portPreference);
        assertNull(pceOpticalNode.getRdmSrgClient("7", StringConstants.SERVICE_DIRECTION_AZ));
        assertFalse(pceOpticalNode.isValid());
        assertNull(pceOpticalNode.getBitSetData());
        assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    void testGetRdmSrgClientEmpty() {
        NodeBuilder node1Builder = getNodeBuilderEmpty(geSupportingNodes(), OpenroadmTpType.SRGTXRXPP);
        Node specificNode = node1Builder.setNodeId(new NodeId("test")).build();
        pceOpticalNode = new PceOpticalNode(null, null, null, specificNode,
                OpenroadmNodeType.ROADM, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initSrgTps(portPreference);
        pceOpticalNode.initFrequenciesBitSet();
        pceOpticalNode.initXndrTps(ServiceFormat.OMS);
        assertNull(pceOpticalNode.getRdmSrgClient("7" ,StringConstants.SERVICE_DIRECTION_AZ));
        assertFalse(pceOpticalNode.isValid());
        assertNull(pceOpticalNode.getBitSetData());
        assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    void testGetRdmSrgClientDeg() {
        pceOpticalNode = new PceOpticalNode(null, null, null, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initSrgTps(portPreference);
        assertNull(pceOpticalNode.getRdmSrgClient("7" ,StringConstants.SERVICE_DIRECTION_AZ));
        assertFalse(pceOpticalNode.isValid());
        assertNull(pceOpticalNode.getBitSetData());
        assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    void testGetRdmSrgClientsrgtxcp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.SRGTXCP);
        Node specificNode = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(null, null, null, specificNode,
                OpenroadmNodeType.ROADM, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initSrgTps(portPreference);
        assertFalse(pceOpticalNode.isValid());
        assertNull(pceOpticalNode.getBitSetData());
        assertTrue(pceOpticalNode.checkTP("testTP"));
        assertNull(pceOpticalNode.getRdmSrgClient("5", StringConstants.SERVICE_DIRECTION_AZ));
    }

    @Test
    void testGetRdmSrgClientDegreerxtpp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.DEGREERXTTP);
        node = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(null, null, null, node,
                OpenroadmNodeType.ROADM, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, new NodeMcCapability());
        pceOpticalNode.initSrgTps(portPreference);
        assertNull(pceOpticalNode.getRdmSrgClient("2" ,StringConstants.SERVICE_DIRECTION_AZ));
        assertFalse(pceOpticalNode.isValid());
        assertNull(pceOpticalNode.getBitSetData());
        assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    private Map<SupportingNodeKey,SupportingNode> geSupportingNodes() {
        Map<SupportingNodeKey,SupportingNode> supportingNodes1 = new HashMap<>();
        SupportingNode supportingNode1 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 1"))
                .setNetworkRef(new NetworkId(StringConstants.CLLI_NETWORK))
                .build();
        supportingNodes1
                .put(supportingNode1.key(),supportingNode1);

        SupportingNode supportingNode2 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 2"))
                .setNetworkRef(new NetworkId(StringConstants.OPENROADM_NETWORK))
                .build();
        supportingNodes1
                .put(supportingNode2.key(),supportingNode2);
        return supportingNodes1;
    }

    private NodeBuilder getNodeBuilder(Map<SupportingNodeKey,SupportingNode> supportingNodes1,
            OpenroadmTpType openroadmTpType) {

        TerminationPoint1Builder tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder();
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        xpdrTpBldr.addAugmentation(createAnotherTerminationPoint().build());
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1 node1 = getNode1();
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();
        Node1 node11 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                .setAdministrativeState(AdminStates.InService).setOperationalState(State.InService).build();
        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1Rev180226)
                .addAugmentation(node1)
                .addAugmentation(node11)
                .setSupportingNode(supportingNodes1);
    }

    private NodeBuilder getNodeBuilderEmpty(Map<SupportingNodeKey,SupportingNode>  supportingNodes1,
            OpenroadmTpType openroadmTpType) {

        TerminationPoint1Builder tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder();
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        xpdrTpBldr.addAugmentation(createAnotherTerminationPoint().build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1 node1 = getNode1Empty();
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();
        Node1 node11 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                .setAdministrativeState(AdminStates.InService).setOperationalState(State.InService).build();
        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1Rev180226)
                .addAugmentation(node1)
                .addAugmentation(node11)
                .setSupportingNode(supportingNodes1);
    }

    private org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1 getNode1() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder()
                .setSrgAttributes(getSrgAttributes())
                .setDegreeAttributes(getDegAttributes())
                .build();
    }

    private org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1 getNode1Empty() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder()
                .setSrgAttributes(getEmptySrgAttributes())
                .setDegreeAttributes(getEmptyDegAttributes())
                .build();
    }

    private DegreeAttributes getDegAttributes() {
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.USED_SLOT_VALUE);
        byteArray[7] = (byte) GridConstant.AVAILABLE_SLOT_VALUE;
        Map<AvailFreqMapsKey, AvailFreqMaps> waveMap = new HashMap<>();
        AvailFreqMaps availFreqMaps = new AvailFreqMapsBuilder().setMapName(GridConstant.C_BAND)
                .setFreqMapGranularity(new FrequencyGHz(Decimal64.valueOf(String.valueOf(GridConstant.GRANULARITY))))
                .setStartEdgeFreq(
                    new FrequencyTHz(Decimal64.valueOf(String.valueOf(GridConstant.START_EDGE_FREQUENCY))))
                .setEffectiveBits(Uint16.valueOf(GridConstant.EFFECTIVE_BITS))
                .setFreqMap(byteArray)
                .build();
        waveMap.put(availFreqMaps.key(), availFreqMaps);
        return new DegreeAttributesBuilder()
                .setAvailFreqMaps(waveMap)
                .build();
    }

    private SrgAttributes getSrgAttributes() {
        return new SrgAttributesBuilder().setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available()).build();
    }

    private DegreeAttributes getEmptyDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailFreqMaps(Map.of())
                .build();
    }

    private SrgAttributes getEmptySrgAttributes() {
        return new SrgAttributesBuilder().setAvailFreqMaps(Map.of()).build();
    }

    private TerminationPointBuilder getTerminationPointBuilder() {
        return new TerminationPointBuilder().setTpId(new TpId("2"));
    }

    private TerminationPoint1Builder getTerminationPoint1Builder(OpenroadmTpType openroadmTpType) {
        return new TerminationPoint1Builder().setTpType(openroadmTpType).setOperationalState(State.InService)
                .setAdministrativeState(AdminStates.InService);

    }

    private org.opendaylight.yang.gen
            .v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder createAnotherTerminationPoint() {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder()
                .setCtpAttributes((new CtpAttributesBuilder()).build())
                .setCpAttributes((new CpAttributesBuilder()).build())
                .setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(Map.of()).build())
                .setXpdrClientAttributes((new XpdrClientAttributesBuilder()).build())
                .setXpdrPortAttributes((new XpdrPortAttributesBuilder()).build())
                .setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder()
                        .setTailEquipmentId("destNode" + "--" + "destTp").build());
    }
}
