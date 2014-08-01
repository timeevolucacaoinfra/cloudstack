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
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.globonetwork.cloudstack.manager.GloboNetworkService;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipExternalResponse;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;

@APICommand(name = "listNetworkApiVips", responseObject=GloboNetworkVipExternalResponse.class, description="Lists NetworkAPI Vips")
public class ListGloboNetworkVipsCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(ListGloboNetworkVipsCmd.class);
    private static final String s_name = "listnetworkapivipsresponse";
    
    @Inject
    GloboNetworkService _ntwkAPIService;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class, description="the project id")
    private Long projectId;
    
    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("listNetworkApiVipsCmd command with projectId=" + projectId);
        	List<GloboNetworkVipResponse> napiVips = _ntwkAPIService.listNetworkAPIVips(this.projectId);
        	
        	List<GloboNetworkVipExternalResponse> responseList = new ArrayList<GloboNetworkVipExternalResponse>();
    		
    		for (GloboNetworkVipResponse networkAPIVip : napiVips) {
    			GloboNetworkVipExternalResponse vipResponse = new GloboNetworkVipExternalResponse();
    			vipResponse.setId(networkAPIVip.getId());
    			vipResponse.setName(networkAPIVip.getName());
    			vipResponse.setIp(networkAPIVip.getIp());
    			vipResponse.setCache(networkAPIVip.getCache());
    			vipResponse.setMethod(networkAPIVip.getMethod());
    			vipResponse.setPersistence(networkAPIVip.getPersistence());
    			vipResponse.setHealthchecktype(networkAPIVip.getHealthcheckType());
    			vipResponse.setHealthcheck(networkAPIVip.getHealthcheck());
    			vipResponse.setMaxconn(networkAPIVip.getMaxConn());
    			vipResponse.setPorts(networkAPIVip.getPorts());
    			vipResponse.setNetworkids(networkAPIVip.getNetworkIds());
    			
    			vipResponse.setObjectName("networkapivip");
				responseList.add(vipResponse);
			}
    		 
    		ListResponse<GloboNetworkVipExternalResponse> response = new ListResponse<GloboNetworkVipExternalResponse>();
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
