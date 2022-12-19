/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;

public class PowerMgmtTest {
    private DataBroker dataBroker;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private CrossConnect crossConnect;
    private DeviceTransactionManager deviceTransactionManager;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;

    @Before
    public void setUp() {
        this.dataBroker = Mockito.mock(DataBroker.class);
        this.openRoadmInterfaces = Mockito.mock(OpenRoadmInterfaces.class);
        this.crossConnect = Mockito.mock((CrossConnectImpl.class));
        this.deviceTransactionManager = Mockito.mock(DeviceTransactionManager.class);
        this.portMapping = Mockito.mock(PortMapping.class);
        this.powerMgmt = new PowerMgmtImpl(this.dataBroker, this.openRoadmInterfaces, this.crossConnect,
                this.deviceTransactionManager, this.portMapping, "1000", "1000");
    }

    @Test
    public void testSetPowerWhenMappingReturnNull() {
        Mockito.when(this.portMapping.getNode(Mockito.anyString())).thenReturn(null);
        boolean output = this.powerMgmt.setPower(OlmPowerServiceRpcImplUtil.getServicePowerSetupInput());
        Assert.assertEquals(false, output);
    }

    @Test
    public void testSetPowerForTransponderAEnd() throws OpenRoadmInterfaceException {
        Mockito.when(this.portMapping.getNode("xpdr-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeTpdr("xpdr-A", OpenroadmNodeVersion._121,
                    List.of("network-A")));
        Mockito.when(this.portMapping.getNode("roadm-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                    List.of("srg1-A", "deg2-A")));
        Interface interfOch = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(new Interface1Builder()
                        .setOch(new OchBuilder().build())
                        .build())
                .build();
        Mockito.when(this.openRoadmInterfaces.getInterface(Mockito.matches("xpdr-A"), Mockito.anyString()))
            .thenReturn(Optional.of(interfOch));
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                                .Interface1Builder()
                                .setOts(new OtsBuilder()
                                        .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                                        .build())
                                .build())
                .build();
        Mockito.when(this.openRoadmInterfaces
                        .getInterface(Mockito.matches("roadm-A"), Mockito.anyString()))
                .thenReturn(Optional.of(interfOts));
        MockedStatic<PowerMgmtVersion121> pmv121 = Mockito.mockStatic(PowerMgmtVersion121.class);
        pmv121.when(() -> PowerMgmtVersion121.setTransponderPower(Mockito.anyString(), Mockito.anyString(),
                    Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(true);
        Map<String, Double> powerRangeMap = new HashMap<>();
        powerRangeMap.put("MaxTx", 0.1);
        powerRangeMap.put("MinTx", -5.1);
        pmv121.when(() -> PowerMgmtVersion121.getXponderPowerRange(Mockito.anyString(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.any()))
                .thenReturn(powerRangeMap);
        Mockito.when(this.crossConnect.setPowerLevel(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                        Mockito.anyString()))
                .thenReturn(true);

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInputForTransponder();
        boolean result = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, result);
    }

    @Test
    public void testSetPowerForTransponderZEnd() throws OpenRoadmInterfaceException {
        Mockito.when(this.portMapping.getNode("xpdr-C"))
                .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeTpdr("xpdr-C", OpenroadmNodeVersion._121,
                        List.of("client-C")));

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("xpdr-C", "network-C", "client-C");
        boolean result = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, result);
    }

    @Test
    public void testSetPowerForRoadmAEnd() throws OpenRoadmInterfaceException {
        Mockito.when(this.portMapping.getNode("roadm-A"))
                .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                        List.of("srg1-A", "deg2-A")));
        Mockito.when(this.deviceTransactionManager.getDataFromDevice(Mockito.anyString(), Mockito.any(),
                        Mockito.any(), Mockito.anyLong(), Mockito.any()))
                .thenReturn(Optional.empty());
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                            .Interface1Builder()
                    .setOts(new OtsBuilder()
                            .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                            .build())
                    .build())
                .build();
        Mockito.when(this.openRoadmInterfaces.getInterface(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(interfOts));
        Mockito.when(this.crossConnect.setPowerLevel(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                        Mockito.anyString()))
                .thenReturn(true);

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("roadm-A", "srg1-A", "deg2-A");
        boolean result = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, result);
        verify(this.crossConnect, times(1)).setPowerLevel(Mockito.matches("roadm-A"),
                Mockito.matches(OpticalControlMode.Power.getName()), eq(Decimal64.valueOf("-3.00")),
                Mockito.matches("srg1-A-deg2-A-761:768"));
        verify(this.crossConnect, times(1)).setPowerLevel(Mockito.matches("roadm-A"),
                Mockito.matches(OpticalControlMode.GainLoss.getName()), eq(Decimal64.valueOf("-3.00")),
                Mockito.matches("srg1-A-deg2-A-761:768"));
    }

    @Test
    public void testSetPowerForRoadmZEnd() throws OpenRoadmInterfaceException {
        Mockito.when(this.portMapping.getNode("roadm-C"))
                .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-C", OpenroadmNodeVersion._121,
                        List.of("deg1-C", "srg1-C")));
        Mockito.when(this.deviceTransactionManager.getDataFromDevice(Mockito.anyString(), Mockito.any(),
                        Mockito.any(), Mockito.anyLong(), Mockito.any()))
                .thenReturn(Optional.empty());
        Mockito.when(this.crossConnect.setPowerLevel(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                        Mockito.anyString()))
                .thenReturn(true);

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("roadm-C", "deg1-C", "srg1-C");
        boolean result = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, result);
        verify(this.crossConnect, times(1)).setPowerLevel(Mockito.matches("roadm-C"),
                Mockito.matches(OpticalControlMode.Power.getName()), Mockito.isNull(),
                Mockito.matches("deg1-C-srg1-C-761:768"));
    }

    @Test
    public void testSetPowerForTransponderWhenNoTransponderPort() throws OpenRoadmInterfaceException {
        Mockito.when(this.portMapping.getNode("xpdr-A"))
                .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeTpdr("xpdr-A", OpenroadmNodeVersion._121,
                        List.of("network-A")));
        Mockito.when(this.portMapping.getNode("roadm-A"))
                .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                        List.of("srg1-A", "deg2-A")));
        Interface interfOch = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(new Interface1Builder()
                        .setOch(new OchBuilder().build())
                        .build())
                .build();
        Mockito.when(this.openRoadmInterfaces.getInterface(Mockito.matches("xpdr-A"), Mockito.anyString()))
                .thenReturn(Optional.of(interfOch));
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                                .Interface1Builder()
                                .setOts(new OtsBuilder()
                                        .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                                        .build())
                                .build())
                .build();
        Mockito.when(this.openRoadmInterfaces
                        .getInterface(Mockito.matches("roadm-A"), Mockito.anyString()))
                .thenReturn(Optional.of(interfOts));
        try (MockedStatic<PowerMgmtVersion121> pmv121 = Mockito.mockStatic(PowerMgmtVersion121.class)) {
            pmv121.when(() -> PowerMgmtVersion121.setTransponderPower(Mockito.anyString(), Mockito.anyString(),
                            Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenReturn(true);
            pmv121.when(() -> PowerMgmtVersion121.getXponderPowerRange(Mockito.anyString(), Mockito.anyString(),
                            Mockito.anyString(), Mockito.any()))
                    .thenReturn(new HashMap<>());

            Mockito.when(this.crossConnect.setPowerLevel(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                            Mockito.anyString()))
                    .thenReturn(true);


            ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInputForTransponder();
            boolean result = this.powerMgmt.setPower(input);
            Assert.assertEquals(true, result);
            pmv121.verify(() -> PowerMgmtVersion121.setTransponderPower(Mockito.matches("xpdr-A"),
                    Mockito.anyString(), eq(new BigDecimal("-5")), Mockito.any(), Mockito.any()));
            verify(this.crossConnect, times(1)).setPowerLevel(Mockito.matches("roadm-A"),
                    Mockito.matches(OpticalControlMode.GainLoss.getName()), eq(Decimal64.valueOf("-3.00")),
                    Mockito.matches("srg1-A-deg2-A-761:768"));
        }
    }

    @Test
    public void testSetPowerForTransponderAEndWithRoadmPort() throws OpenRoadmInterfaceException {
        Mockito.when(this.portMapping.getNode("xpdr-A"))
                .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeTpdr("xpdr-A", OpenroadmNodeVersion._121,
                        List.of("network-A")));
        Mockito.when(this.portMapping.getNode("roadm-A"))
                .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                        List.of("srg1-A", "deg2-A")));
        Interface interfOch = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(new Interface1Builder()
                        .setOch(new OchBuilder().build())
                        .build())
                .build();
        Mockito.when(this.openRoadmInterfaces.getInterface(Mockito.matches("xpdr-A"), Mockito.anyString()))
                .thenReturn(Optional.of(interfOch));
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                                .Interface1Builder()
                                .setOts(new OtsBuilder()
                                        .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                                        .build())
                                .build())
                .build();
        Mockito.when(this.openRoadmInterfaces
                        .getInterface(Mockito.matches("roadm-A"), Mockito.anyString()))
                .thenReturn(Optional.of(interfOts));
        try (MockedStatic<PowerMgmtVersion121> pmv121 = Mockito.mockStatic(PowerMgmtVersion121.class)) {

            pmv121.when(() -> PowerMgmtVersion121.setTransponderPower(Mockito.anyString(), Mockito.anyString(),
                            Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenReturn(true);
            Map<String, Double> powerRangeMapTpdrTx = new HashMap<>();
            powerRangeMapTpdrTx.put("MaxTx", 0.1);
            powerRangeMapTpdrTx.put("MinTx", -5.1);
            pmv121.when(() -> PowerMgmtVersion121.getXponderPowerRange(Mockito.anyString(), Mockito.anyString(),
                            Mockito.anyString(), Mockito.any()))
                    .thenReturn(powerRangeMapTpdrTx);
            Map<String, Double> powerRangeMapSrgRx = new HashMap<>();
            powerRangeMapSrgRx.put("MaxRx", -4.2);
            powerRangeMapSrgRx.put("MinRx", -22.2);
            pmv121.when(() -> PowerMgmtVersion121.getSRGRxPowerRange(Mockito.anyString(), Mockito.anyString(),
                            Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                    .thenReturn(powerRangeMapSrgRx);
            Mockito.when(this.crossConnect.setPowerLevel(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                            Mockito.anyString()))
                    .thenReturn(true);

            ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInputForTransponder();
            boolean result = this.powerMgmt.setPower(input);
            Assert.assertEquals(true, result);
            pmv121.verify(() -> PowerMgmtVersion121.setTransponderPower(Mockito.matches("xpdr-A"),
                    Mockito.anyString(), eq(new BigDecimal("-4.20000000000000017763568394002504646778106689453125")),
                    Mockito.any(), Mockito.any()));
        }
    }

    @Test
    public void testSetPowerWithoutNode() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInputWthoutNode();
        boolean result = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, result);
        verifyNoInteractions(this.crossConnect);
    }

    @Test
    public void testSetPowerForBadNodeType() throws OpenRoadmInterfaceException {
        Mockito.when(this.portMapping.getNode("ila node"))
                .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeIla());

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("ila node", "rx-port", "tx-port");
        boolean result = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, result);
        verifyNoInteractions(this.crossConnect);
        verifyNoInteractions(this.openRoadmInterfaces);
    }


    @Test
    public void testPowerTurnDownWhenSuccess() {
        Mockito.when(this.crossConnect.setPowerLevel(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                        Mockito.anyString()))
                .thenReturn(true);
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        boolean result = this.powerMgmt.powerTurnDown(input);
        Assert.assertEquals(true, result);
        verify(this.crossConnect, times(1)).setPowerLevel(Mockito.matches("roadm-C"),
                Mockito.matches(OpticalControlMode.Off.getName()), Mockito.isNull(), Mockito.anyString());
        verify(this.crossConnect, times(1)).setPowerLevel(Mockito.matches("roadm-A"),
                Mockito.matches(OpticalControlMode.Power.getName()), eq(Decimal64.valueOf("-60")),
                Mockito.anyString());
        verify(this.crossConnect, times(1)).setPowerLevel(Mockito.matches("roadm-A"),
                Mockito.matches(OpticalControlMode.Off.getName()), Mockito.isNull(), Mockito.anyString());
    }

    @Test
    public void testPowerTurnDownWhenFailure() {
        Mockito.when(this.crossConnect.setPowerLevel(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                        Mockito.anyString()))
                .thenReturn(false);
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        boolean result = this.powerMgmt.powerTurnDown(input);
        Assert.assertEquals(false, result);
        verify(this.crossConnect, times(2))
                .setPowerLevel(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString());
    }
}
