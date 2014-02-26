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

package com.globo.networkapi.commands;

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
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.networkapi.NetworkAPIEnvironmentVO;
import com.globo.networkapi.element.NetworkAPIService;
import com.globo.networkapi.response.NetworkAPIEnvironmentResponse;

@APICommand(name = "listNetworkApiEnvironments", responseObject=NetworkAPIEnvironmentResponse.class, description="Lists NetworkAPI environments")
public class ListNetworkApiEnvironmentsCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(ListNetworkApiEnvironmentsCmd.class);
    private static final String s_name = "listnetworkapienvironmentsresponse";
    
    @Inject
    NetworkAPIService _ntwkAPIService;
    
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            required=false, description="the Physical Network ID the network belongs to")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            required=false, description="the ZoneID")
    private Long zoneId;

    public Long getPhysicalNetworkId() {
    	return physicalNetworkId;
    }

    public Long getZoneId() {
    	return zoneId;
    }

    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("listNetworkApiEnvironmentsCmd command with physicalNetowrkId=" + physicalNetworkId + " zoneId=" + zoneId);
        	List<NetworkAPIEnvironmentVO> napiEnvironments = _ntwkAPIService.listNetworkAPIEnvironmentsFromDB(this.physicalNetworkId, this.zoneId);
        	
        	List<NetworkAPIEnvironmentResponse> responseList = new ArrayList<NetworkAPIEnvironmentResponse>();
    		
    		for (NetworkAPIEnvironmentVO networkAPIEnvironmentVO : napiEnvironments) {
				NetworkAPIEnvironmentResponse envResponse = new NetworkAPIEnvironmentResponse();
				envResponse.setId(networkAPIEnvironmentVO.getId());
				envResponse.setName(networkAPIEnvironmentVO.getName());
				envResponse.setPhysicalNetworkId(ApiDBUtils.findPhysicalNetworkById(networkAPIEnvironmentVO.getPhysicalNetworkId()).getUuid());
				envResponse.setNapiEnvironmentId(networkAPIEnvironmentVO.getNapiEnvironmentId());
				envResponse.setObjectName("networkapienvironment");
				responseList.add(envResponse);
			}
    		 
    		ListResponse<NetworkAPIEnvironmentResponse> response = new ListResponse<NetworkAPIEnvironmentResponse>();
    		response.setResponses(responseList);
    		response.setResponseName(getCommandName());
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
