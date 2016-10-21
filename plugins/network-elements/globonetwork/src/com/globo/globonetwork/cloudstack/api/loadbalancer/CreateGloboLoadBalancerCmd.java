// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.globo.globonetwork.cloudstack.api.loadbalancer;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.addr.PublicIp;

import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.globonetwork.cloudstack.manager.GloboNetworkService;
import java.util.HashMap;
import javax.inject.Inject;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBHealthCheckPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.response.LoadBalancerResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.log4j.Logger;

@APICommand(name = "createGloboLoadBalancer", description = "Creates a globo load balancer", responseObject = LoadBalancerResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateGloboLoadBalancerCmd extends CreateLoadBalancerRuleCmd /*implements LoadBalancer */{
    public static final Logger s_logger = Logger.getLogger(CreateGloboLoadBalancerCmd.class.getName());

    private static final String s_name = "creategloboloadbalancerresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Inject
    private GloboNetworkService globoNetworkSvc;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Project the IP address will be associated with")
    private Long projectId;

    @Parameter(name = "lbenvironmentid", type = CommandType.LONG, description = "Globo Network LB Environment Id", required = true)
    private Long globoNetworkLBEnvironmentId;

    @Parameter(name = ApiConstants.HEALTHCHECK_PINGPATH, type = CommandType.STRING, required = false, description = "HTTP Ping Path")
    private String pingPath;


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {
        final String name = getName();
        try {
            s_logger.debug("[load_balancer " + name + "] processing execute createGloboLoadBalance");

            super.execute();

            createHealthcheck();
        }catch (Exception e) {
            handleErrorAfterHealthcheckCreated(name, healthcheckCreated );
            handleErrorAfterIpCreated(name, ipCreated, getSourceIpAddressId());
            throw e;
        }
    }

    private void handleErrorAfterHealthcheckCreated(String name, boolean healthcheckCreated) {
        if ( healthcheckCreated ) {
            s_logger.warn("[load_balancer " + name + "] removing healthcheck after error");
            _lbService.deleteLoadBalancerRule(getEntityId(), false);
        }
    }

    private void createHealthcheck() throws ResourceUnavailableException {
        final String name = getName();
        s_logger.debug("[load_balancer " + name + "] creating healthcheck ");

        healthCmd.setFullUrlParams(new HashMap<String, String>());
        healthCmd.setPingPath(pingPath);
        healthCmd.setLbRuleId(this.getEntityId());

        healthcheckCreated = true;

        s_logger.debug("[load_balancer " + name + "] apply healthcheck ");
        HealthCheckPolicy healthcheck = _lbService.createLBHealthCheckPolicy(healthCmd);
        healthCmd.setEntityId(healthcheck.getId());
        healthCmd.setEntityUuid(healthcheck.getUuid());

        boolean healthcheckApplied = _lbService.applyLBHealthCheckPolicy(healthCmd);
        if (!healthcheckApplied) {
            throw new CloudRuntimeException("Error trying to create load balancer healthcheck");
        }
    }


    @Override
    public void create() {
        final String name = getName();

        Long ipAddressId = null;

        try {
            PublicIp ip = globoNetworkSvc.acquireLbIp(getNetworkId(), getProjectId(), getGloboNetworkLBEnvironmentId());
            IpAddress ipAddress = _networkService.associateIPToNetwork(ip.getId(), getNetworkId());
            ipAddressId = ipAddress.getId();
            ipCreated = true;

            setPublicIpId(ip.getId());
            setSourceIpAddressId(ipAddress.getId());

            s_logger.debug("[load_balancer " + name + "] portable ip created " + ip.getId() + ", ipaddress " + ipAddress.getId());

            super.create();
        } catch (Exception e) {
            handleErrorAfterIpCreated(name, ipCreated, ipAddressId);

            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, e.getMessage());
        }
    }

    private boolean ipCreated = false;
    private boolean healthcheckCreated = false;
    private CreateLBHealthCheckPolicyCmd healthCmd = new CreateLBHealthCheckPolicyCmd();

    @Override
    public long getEntityOwnerId() {
        return getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating load balancer: " + getName() + " account: " + getAccountName();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.FirewallRule;
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getNetworkId();
    }


    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getGloboNetworkLBEnvironmentId() {
        return globoNetworkLBEnvironmentId;
    }

    public void setGloboNetworkLBEnvironmentId(Long globoNetworkLBEnvironmentId) {
        this.globoNetworkLBEnvironmentId = globoNetworkLBEnvironmentId;
    }

    public long getAccountId() {
        long networkId = getNetworkId();

        Network network = _networkService.getNetwork(networkId);
        return network.getAccountId();
    }


    private void handleErrorAfterIpCreated(String lbName, boolean ipCreated, Long ipAddressId) {
        if (ipCreated) {
            boolean result = globoNetworkSvc.disassociateIpAddrFromGloboNetwork(ipAddressId);
            if (result) {
                s_logger.warn("[load_balancer " + lbName + "] error after try to disassociateIp " + ipAddressId);
            }
        }
    }
}
