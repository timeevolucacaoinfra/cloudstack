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

package com.globo.globonetwork.cloudstack.api;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.globonetwork.cloudstack.manager.GloboNetworkService;

@APICommand(name = "generateUrlForEditingVip", responseObject=SuccessResponse.class, description="Generate an url used to edit or to create new vip")
public class GenerateUrlForEditingVipCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(GenerateUrlForEditingVipCmd.class.getName());
    private static final String s_name = "generateurlforeditingvipresponse";
    
    @Inject
    GloboNetworkService _globoNetworkService;
    
    @Parameter(name=ApiConstants.VIP_ID, type=CommandType.LONG, required = false, description="GloboNetwork VIP ID, when you want to edit. Otherwise, generated url will be to create a new vip")
    private Long globoNetworkVipId;
    
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.UUID, entityType=NetworkResponse.class, required=true, description="the network ID to be associated to VIP")
    private Long networkId;

    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("generateUrlForEditingVipCmd command with vipId=" + globoNetworkVipId + " networkId=" + networkId);
        	Network network = ApiDBUtils.findNetworkById(networkId);
        	if (network == null) {
        		throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid network");
        	}
        	
        	String url = _globoNetworkService.generateUrlForEditingVip(globoNetworkVipId, network);
        	
        	UrlResponse response = new UrlResponse();
        	response.setUrl(url);
        	response.setResponseName(getCommandName());
        	response.setObjectName("editingurl");
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
    	return CallContext.current().getCallingAccountId();
    }
}
