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
import com.cloud.network.rules.StickinessPolicy;
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
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBStickinessPolicyCmd;
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


    @Parameter(name = "stickinessmethodname", type = CommandType.STRING,
               description = "name of the LB Stickiness policy method, possible values can be obtained from ListNetworks API ")
    private String stickinessMethodName;


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
            createStickiness();
        }catch (Exception e) {
            s_logger.warn("[load_balancer " + name + "] removing loadbalancer after error", e);
            _lbService.deleteLoadBalancerRule(getEntityId(), false);
            handleErrorAfterIpCreated(name, ipCreated, ipAddressId);
            throw e;
        }
    }

    protected void createStickiness() throws CloudRuntimeException {
        final String name = getName();

        if (!isToCreateStickiness()) {
            s_logger.debug("[load_balancer " + name + "] it is not necessary to create stickiness. stickinessmethodname: " + stickinessMethodName);
            return;
        }

        try {
            s_logger.debug("[load_balancer " + name + "] creating stickiness policy ");
            stickinessCmd = new CreateLBStickinessPolicyCmd();
            stickinessCmd.setStickinessMethodName(stickinessMethodName);
            stickinessCmd.setLbStickinessPolicyName(stickinessMethodName);
            stickinessCmd.setLbRuleId(getEntityId());

            StickinessPolicy result = _lbService.createLBStickinessPolicy(stickinessCmd);
            stickinessCmd.setEntityId(result.getId());
            stickinessCmd.setEntityUuid(result.getUuid());

            boolean success = _lbService.applyLBStickinessPolicy(stickinessCmd); // with error it already rollback the stickiness
            if (!success) {
                throw new CloudRuntimeException("Error trying apply load balancer stickiness policy.");
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Error trying create Stickiness policy.", e);
        }

    }


    protected void createHealthcheck() throws ResourceUnavailableException {
        final String name = getName();

        if (!isToCreateHealthcheck()) {
            s_logger.debug("[load_balancer " + name + "] it is not necessary to create healthcheck. pingpath: " + pingPath);
            return;
        }


        s_logger.debug("[load_balancer " + name + "] creating healthcheck ");
        healthCmd = new CreateLBHealthCheckPolicyCmd();
        healthCmd.setFullUrlParams(new HashMap<String, String>());
        healthCmd.setPingPath(pingPath);
        healthCmd.setLbRuleId(this.getEntityId());

        try {
            HealthCheckPolicy healthcheck = _lbService.createLBHealthCheckPolicy(healthCmd);

            healthCmd.setEntityId(healthcheck.getId());
            healthCmd.setEntityUuid(healthcheck.getUuid());

            s_logger.debug("[load_balancer " + name + "] apply healthcheck: " + healthcheck.getUuid());
            boolean healthcheckApplied = _lbService.applyLBHealthCheckPolicy(healthCmd); // with error it already rollback the healthcheck
            if (!healthcheckApplied) {
                throw new CloudRuntimeException("Error trying to apply load balancer healthcheck");
            }

        } catch (Exception e) {
            if ( e instanceof  CloudRuntimeException) {
                throw e;
            }
            throw new CloudRuntimeException("Error trying to create load balancer healthcheck.", e);
        }
    }


    @Override
    public void create() {
        final String name = getName();



        try {
            PublicIp ip = globoNetworkSvc.acquireLbIp(getNetworkId(), getProjectId(), getGloboNetworkLBEnvironmentId());
            IpAddress ipAddress = _networkService.associateIPToNetwork(ip.getId(), getNetworkId());
            ipAddressId = ipAddress.getId();
            ipCreated = true;

            setPublicIpId(ip.getId());
            setSourceIpAddressId(ipAddress.getId());

            s_logger.debug("[load_balancer " + name + "] portable ip created with id " + ip.getId() + ", ipaddress " + ipAddress.getId());

            super.create();
        } catch (Exception e) {
            s_logger.error("[load_balancer " + name + "] error creating load balancer ", e);
            handleErrorAfterIpCreated(name, ipCreated, ipAddressId);

            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, e.getMessage());
        }
    }

    private boolean ipCreated = false;
    private CreateLBHealthCheckPolicyCmd healthCmd;
    private CreateLBStickinessPolicyCmd stickinessCmd;
    private Long ipAddressId = null;

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

    public boolean isToCreateStickiness() {
        return stickinessMethodName != null && !stickinessMethodName.isEmpty();
    }

    public boolean isToCreateHealthcheck() {
        return pingPath != null && !pingPath.isEmpty();
    }


    public void setPingPath(String pingPath) {
        this.pingPath = pingPath;
    }

    public void setStickinessMethodName(String stickinessMethodName) {
        this.stickinessMethodName = stickinessMethodName;
    }
}
