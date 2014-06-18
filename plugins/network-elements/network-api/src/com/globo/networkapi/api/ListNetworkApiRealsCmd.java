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
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.networkapi.manager.NetworkAPIService;
import com.globo.networkapi.response.NetworkAPIVipExternalResponse;
import com.globo.networkapi.response.NetworkAPIVipResponse;
import com.google.common.base.Joiner;

@APICommand(name = "listNetworkApiReals", responseObject=NetworkAPIVipExternalResponse.class, description="List NetworkAPI Reals")
public class ListNetworkApiRealsCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(ListNetworkApiRealsCmd.class);
    private static final String s_name = "listnetworkapirealsresponse";
    
    @Inject
    NetworkAPIService _ntwkAPIService;
    
    @Parameter(name=ApiConstants.VIP_ID, required = true, type=CommandType.LONG, entityType = NetworkAPIVipResponse.class, description="the vip id")
    private Long vipId;
    
    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("listNetworkApiRealsCmd command with vipId=" + vipId);
        	List<NetworkAPIVipResponse.Real> networkAPIReals = _ntwkAPIService.listNetworkAPIReals(this.vipId);
    		
        	List<NetworkAPIVipExternalResponse.Real> responseList = new ArrayList<NetworkAPIVipExternalResponse.Real>();
        	
        	for(NetworkAPIVipResponse.Real napiReal : networkAPIReals) {
        		NetworkAPIVipExternalResponse.Real realResponse = new NetworkAPIVipExternalResponse.Real();
    			realResponse.setVmname(napiReal.getVmName());
    			realResponse.setIp(napiReal.getIp());
        		realResponse.setNetwork(napiReal.getNetwork());
    			realResponse.setPorts(Joiner.on(", ").join(napiReal.getPorts()));
    			realResponse.setState(napiReal.getState());
    			realResponse.setNic(napiReal.getNic());
    			
    			realResponse.setObjectName("networkapireal");
    			responseList.add(realResponse);
        	}
        	
        	ListResponse<NetworkAPIVipExternalResponse.Real> response = new ListResponse<NetworkAPIVipExternalResponse.Real>();
        	response.setResponses(responseList);
    		response.setResponseName(getCommandName());
    		this.setResponseObject(response);
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage(), runtimeExcp);
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
