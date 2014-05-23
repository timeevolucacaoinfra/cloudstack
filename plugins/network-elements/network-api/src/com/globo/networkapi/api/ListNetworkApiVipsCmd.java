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
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.networkapi.manager.NetworkAPIService;
import com.globo.networkapi.response.NetworkAPIVipExternalResponse;
import com.globo.networkapi.response.NetworkAPIVipResponse;
import com.globo.networkapi.response.NetworkAPIVipResponse.Real;
import com.google.common.base.Joiner;

@APICommand(name = "listNetworkApiVips", responseObject=NetworkAPIVipExternalResponse.class, description="Lists NetworkAPI Vips")
public class ListNetworkApiVipsCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(ListNetworkApiVipsCmd.class);
    private static final String s_name = "listnetworkapivipsresponse";
    
    @Inject
    NetworkAPIService _ntwkAPIService;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class, description="the project id")
    private Long projectId;
    
    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("listNetworkApiVipsCmd command with projectId=" + projectId);
        	List<NetworkAPIVipResponse> napiVips = _ntwkAPIService.listNetworkAPIVips(this.projectId);
        	
        	List<NetworkAPIVipExternalResponse> responseList = new ArrayList<NetworkAPIVipExternalResponse>();
    		
    		for (NetworkAPIVipResponse networkAPIVip : napiVips) {
    			NetworkAPIVipExternalResponse vipResponse = new NetworkAPIVipExternalResponse();
    			vipResponse.setId(networkAPIVip.getId());
    			vipResponse.setName(networkAPIVip.getName());
    			vipResponse.setIp(networkAPIVip.getIp());
    			vipResponse.setNetwork(networkAPIVip.getNetwork());
    			vipResponse.setCache(networkAPIVip.getCache());
    			vipResponse.setMethod(networkAPIVip.getMethod());
    			vipResponse.setPersistence(networkAPIVip.getPersistence());
    			vipResponse.setHealthchecktype(networkAPIVip.getHealthcheckType());
    			vipResponse.setHealthcheck(networkAPIVip.getHealthcheck());
    			vipResponse.setMaxconn(networkAPIVip.getMaxConn());
    			vipResponse.setPorts(networkAPIVip.getPorts());
    			
    			List<NetworkAPIVipExternalResponse.Real> realList = new ArrayList<NetworkAPIVipExternalResponse.Real>();
    			for(Real real : networkAPIVip.getReals()) {
    				NetworkAPIVipExternalResponse.Real realResponse = new NetworkAPIVipExternalResponse.Real();
    				realResponse.setVmname(real.getVmName());
    				realResponse.setIp(real.getIp());
    				realResponse.setPorts(Joiner.on(", ").join(real.getPorts()));
    				realResponse.setState(real.getState());
    				realList.add(realResponse);
    			}
    			vipResponse.setReals(realList);
    			
    			vipResponse.setObjectName("networkapivip");
				responseList.add(vipResponse);
			}
    		 
    		ListResponse<NetworkAPIVipExternalResponse> response = new ListResponse<NetworkAPIVipExternalResponse>();
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
        return UserContext.current().getCaller().getId();
    }
}
