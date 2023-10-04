/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBase;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrent;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPm;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerReset;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndown;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OlmPowerServiceRpcImpl.
 */
@Component
public class OlmPowerServiceRpcImpl implements TransportpceOlmService {
    private static final Logger LOG = LoggerFactory.getLogger(OlmPowerServiceRpcImpl.class);
    private final OlmPowerService olmPowerService;
    private Registration reg;

    @Activate
    public OlmPowerServiceRpcImpl(@Reference OlmPowerService olmPowerService,
            @Reference RpcProviderService rpcProviderService) {
        this.olmPowerService = olmPowerService;
        this.reg = rpcProviderService.registerRpcImplementations(ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(GetPm.class, this::getPm)
            .put(ServicePowerSetup.class, this::servicePowerSetup)
            .put(ServicePowerTurndown.class, this::servicePowerTurndown)
            .put(CalculateSpanlossBase.class, this::calculateSpanlossBase)
            .put(CalculateSpanlossCurrent.class, this::calculateSpanlossCurrent)
            .put(ServicePowerReset.class, this::servicePowerReset)
            .build());
        LOG.info("OlmPowerServiceRpcImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("OlmPowerServiceRpcImpl Closed");
    }

    /**
     * This method is the implementation of the 'get-pm' RESTCONF service, which
     * is one of the external APIs into the olm application.
     *
     * <p>
     * 1. get-pm This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name
     *
     * <p>
     * The signature for this method was generated by yang tools from the
     * olm API model.
     *
     * @param input
     *            Input parameter from the olm yang model
     *
     * @return Result of the request
     */
    @Override
    public final ListenableFuture<RpcResult<GetPmOutput>> getPm(GetPmInput input) {
        if (input.getNodeId() == null) {
            LOG.error("getPm: NodeId can not be null");
            return RpcResultBuilder.<GetPmOutput>failed()
                    .withError(ErrorType.RPC, "Error with input parameters")
                    .buildFuture();
        }
        return RpcResultBuilder.success(this.olmPowerService.getPm(input)).buildFuture();
    }

    /**
     * This method is the implementation of the 'service-power-setup' RESTCONF service, which
     * is one of the external APIs into the olm application.
     *
     * <p>
     * 1. service-power-setup: This operation performs following steps:
     *    Step1: Calculate Spanloss on all links which are part of service.
     *    TODO Step2: Calculate power levels for each Tp-Id
     *    TODO Step3: Post power values on roadm connections
     *
     * <p>
     * The signature for this method was generated by yang tools from the
     * olm API model.
     *
     * @param input
     *            Input parameter from the olm yang model
     *            Input will contain nodeId and termination point
     *
     * @return Result of the request
     */
    @Override
    public final ListenableFuture<RpcResult<ServicePowerSetupOutput>> servicePowerSetup(
            ServicePowerSetupInput input) {
        return RpcResultBuilder.success(this.olmPowerService.servicePowerSetup(input)).buildFuture();
    }

    /**
     * This method is the implementation of the 'service-power-trundown' RESTCONF service, which
     * is one of the external APIs into the olm application.
     *
     * <p>
     * 1. service-power-turndown: This operation performs following steps:
     *    Step1: For each TP within Node sets interface outofservice .
     *    Step2: For each roam-connection sets power to -60dbm
     *    Step3: Turns power mode off
     *
     * <p>
     * The signature for this method was generated by yang tools from the
     * olm API model.
     *
     * @param input
     *            Input parameter from the olm yang model
     *            Input will contain nodeId and termination point
     *
     * @return Result of the request
     */
    @Override
    public final ListenableFuture<RpcResult<ServicePowerTurndownOutput>>
        servicePowerTurndown(ServicePowerTurndownInput input) {
        return RpcResultBuilder.success(this.olmPowerService.servicePowerTurndown(input)).buildFuture();
    }

    /**
     * This method calculates Spanloss for all Roadm to Roadm links,
     * part of active inventory in Network Model or for newly added links
     * based on input src-type.
     *
     * <p>
     * 1. Calculate-Spanloss-Base: This operation performs following steps:
     *    Step1: Read all Roadm-to-Roadm links from network model or get data for given linkID.
     *    Step2: Retrieve PMs for each end point for OTS interface
     *    Step3: Calculates Spanloss
     *    Step4: Posts calculated spanloss in Device and in network model
     *
     * <p>
     * The signature for this method was generated by yang tools from the
     * renderer API model.
     *
     * @param input
     *            Input parameter from the olm yang model
     *            Input will contain SourceType and linkId if srcType is Link
     *
     * @return Result of the request
     */
    @Override
    public final ListenableFuture<RpcResult<CalculateSpanlossBaseOutput>>
        calculateSpanlossBase(CalculateSpanlossBaseInput input) {
        return RpcResultBuilder.success(this.olmPowerService.calculateSpanlossBase(input)).buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<CalculateSpanlossCurrentOutput>> calculateSpanlossCurrent(
            CalculateSpanlossCurrentInput input) {
        return RpcResultBuilder.success(this.olmPowerService.calculateSpanlossCurrent(input)).buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServicePowerResetOutput>> servicePowerReset(ServicePowerResetInput input) {
        return RpcResultBuilder.success(this.olmPowerService.servicePowerReset(input)).buildFuture();
    }

    public Registration getRegisteredRpc() {
        return reg;
    }

}
