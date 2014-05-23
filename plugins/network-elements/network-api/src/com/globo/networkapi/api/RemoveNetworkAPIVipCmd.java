// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.globo.networkapi.api;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.networkapi.manager.NetworkAPIService;

@APICommand(name = "removeNetworkApiVip", responseObject=SuccessResponse.class, description="Remove a VIP from Network API and network in Cloudstack")
public class RemoveNetworkAPIVipCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(RemoveNetworkAPIVipCmd.class.getName());
    private static final String s_name = "removenetworkapivipresponse";
    
    @Inject
    NetworkAPIService _ntwkAPIService;
    
    @Parameter(name=ApiConstants.VIP_ID, type=CommandType.LONG, required=true, description="NetworkAPI VIP ID.")
    private Long napiVipId;
    
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.UUID, entityType=NetworkResponse.class, required=true, description="the network ID associated to VIP")
    private Long networkId;
    
    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("removeNetworkApiVip command with napiVipId=" + napiVipId + " networkId=" + networkId);
        	_ntwkAPIService.removeNapiVip(napiVipId);
        	
        	SuccessResponse response = new SuccessResponse(getCommandName());
        	response.setSuccess(true);
        	this.setResponseObject(response);
        	
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return UserContext.current().getCaller().getId();
    }
}
