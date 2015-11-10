/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.globo.globoaclapi.cloudstack.api;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.globoaclapi.cloudstack.manager.GloboACLService;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = "removeGloboACLRule", description = "Deletes a ACL rule", responseObject = SuccessResponse.class, entityType = {FirewallRule.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RemoveGloboACLRuleCmd extends BaseAsyncCmd{

    private static final String s_name = "removegloboaclruleresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, entityType = SuccessResponse.class, required = true, description = "the ID of the ACL rule")
    private String id;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, required = true, description = "the Network id of the ACL rule")
    private Long networkId;

    @Inject
    protected GloboACLService globoACLService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try{
            Network network = _networkService.getNetwork(networkId);
            globoACLService.removeACLRule(network, new Long(id));
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } catch(CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ACL_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting firewall rule id=" + id;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
