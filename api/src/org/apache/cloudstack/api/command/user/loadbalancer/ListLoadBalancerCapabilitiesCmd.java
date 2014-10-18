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
package org.apache.cloudstack.api.command.user.loadbalancer;

import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.LoadBalancerCapabilitiesResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.log4j.Logger;

import com.cloud.network.Network.Capability;

@APICommand(name = "listLoadBalancerCapabilities", description = "Lists Load Balancer Capabilities", responseObject = LoadBalancerCapabilitiesResponse.class, since="4.6.0")
public class ListLoadBalancerCapabilitiesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListLoadBalancerCapabilitiesCmd.class.getName());

    private static final String s_name = "listloadbalancerssresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.UUID, entityType = NetworkResponse.class, 
            description="the network offering id of the Load Balancer")
    private Long networkId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    
    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        Map<Capability, String> capabilities = _lbService.listCapabilities(this);
        LoadBalancerCapabilitiesResponse response = _responseGenerator.createLoadBalancerCapabilitiesReponse(capabilities);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        // No owner is need for this command
        return 0;
    }

}
