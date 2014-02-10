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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.networkapi.response.NetworkAPIResponse;

@APICommand(name = "addNetworkAPIVlan", responseObject=NetworkAPIResponse.class, description="Adds a vlan/network from Network API")
public class AddNetworkAPIVlanCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(AddNetworkAPIVlanCmd.class.getName());
    private static final String s_name = "addnetworkapivlan";

    /* Parameters */
    @Parameter(name=ApiConstants.VLAN_ID, type=CommandType.LONG, required = true, description="VLAN ID.")
    private Long vlanId;
    
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required = true, description="Zone ID.")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.NETWORK_OFFERING_ID, type=CommandType.LONG, required = true, description="Network offering ID.")
    private Long networkOfferingId;
    
    /* Accessors */
    public Long getVlanId() {
        return vlanId;
    }

    public Long getZoneId() {
    	return zoneId;
    }
    
    public Long getNetworkOfferingId() {
    	return networkOfferingId;
    }
    
    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("addNetworkAPIVlan command with vlanId=" + vlanId + " zoneId=" + zoneId + " networkOfferingId=" + networkOfferingId);
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Adding a VLAN network from NetworkAPI.";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_CREATE;
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
