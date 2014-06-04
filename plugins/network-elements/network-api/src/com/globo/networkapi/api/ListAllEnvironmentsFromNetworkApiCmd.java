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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.globo.networkapi.manager.NetworkAPIService;
import com.globo.networkapi.response.NetworkAPIAllEnvironmentResponse.Environment;
import com.globo.networkapi.response.NetworkAPIEnvironmentExternalResponse;

@APICommand(name = "listAllEnvironmentsFromNetworkApi", responseObject=NetworkAPIEnvironmentExternalResponse.class, description="Lists all environments from NetworkAPI")
public class ListAllEnvironmentsFromNetworkApiCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(ListAllEnvironmentsFromNetworkApiCmd.class.getName());
    private static final String s_name = "listallenvironmentsfromnetworkapiresponse";
    
    @Inject
    NetworkAPIService _ntwkAPIService;
    
	// ///////////////////////////////////////////////////
	// ////////////// API parameters /////////////////////
	// ///////////////////////////////////////////////////

	@Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, required = true, description = "the Physical Network ID")
	private Long physicalNetworkId;
	
	// ///////////////////////////////////////////////////
	// ///////////////// Accessors ///////////////////////
	// ///////////////////////////////////////////////////

	public Long getPhysicalNetworkId() {
		return physicalNetworkId;
	}

    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
    	s_logger.debug("listAllEnvironmentsFromNetworkApiCmd command");
    	List<Environment> environmentList = _ntwkAPIService.listAllEnvironmentsFromNetworkApi(physicalNetworkId);
    	if (environmentList != null) {
    		List<NetworkAPIEnvironmentExternalResponse> responseList = new ArrayList<NetworkAPIEnvironmentExternalResponse>();
    		for (Environment environment : environmentList) {
    			NetworkAPIEnvironmentExternalResponse envResponse = new NetworkAPIEnvironmentExternalResponse();
    			envResponse.setEnvironmentId(environment.getId());
    			envResponse.setEnvironmentFullName(environment.getDcDivisionName() + " - " + environment.getLogicalEnvironmentName() + " - " + environment.getL3GroupName());
    			envResponse.setObjectName("networkapienvironment");
    			responseList.add(envResponse);
			}
    		
    		ListResponse<NetworkAPIEnvironmentExternalResponse> response = new ListResponse<NetworkAPIEnvironmentExternalResponse>();
    		response.setResponses(responseList);
    		response.setResponseName(getCommandName());
    		this.setResponseObject(response);
    	} else {
    		throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to retrieve all environments from NetworkAPI.");
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
