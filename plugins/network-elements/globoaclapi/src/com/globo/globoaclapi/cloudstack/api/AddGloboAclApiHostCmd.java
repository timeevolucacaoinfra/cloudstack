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
package com.globo.globoaclapi.cloudstack.api;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.globoaclapi.cloudstack.manager.GloboACLService;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = "addGloboAclApiHost", responseObject = SuccessResponse.class, description = "Adds the Globo ACL API external host")
public class AddGloboAclApiHostCmd extends BaseAsyncCmd {

    private static final String s_name = "addgloboaclapihostresponse";
    @Inject
    GloboACLService _globoACLService;

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = BaseCmd.CommandType.UUID, entityType = PhysicalNetworkResponse.class, required = true, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.URL, type = BaseCmd.CommandType.STRING, required = true, description = "Globo ACL API url")
    private String url;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Username for Globo ACL API")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Password for Globo ACL API")
    private String password;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            Host host = _globoACLService.addGloboAclApiHost(physicalNetworkId, url, username, password);

            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setSuccess((host != null));
            this.setResponseObject(response);
        } catch (InvalidParameterValueException invalidParamExcp) {
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

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Add Globo ACL API provider";
    }
}
