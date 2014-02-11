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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.networkapi.element.NetworkAPIService;
import com.globo.networkapi.response.AddNetworkApiVlanCmdResponse;

@APICommand(name = "addNetworkApiVlan", responseObject=AddNetworkApiVlanCmdResponse.class, description="Adds a vlan/network from Network API")
public class AddNetworkApiVlanCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(AddNetworkApiVlanCmd.class.getName());
    private static final String s_name = "addnetworkapivlan";
    
    @Inject
    NetworkAPIService _ntwkAPIService;

    /* Parameters */
    @Parameter(name=ApiConstants.VLAN_ID, type=CommandType.LONG, required = true, description="VLAN ID.")
    private Long vlanId;
    
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            required=true, description="the Zone ID for the network")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.NETWORK_OFFERING_ID, type=CommandType.UUID, entityType = NetworkOfferingResponse.class,
            required=true, description="the network offering id")
    private Long networkOfferingId;
    
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            description="the Physical Network ID the network belongs to")
    private Long physicalNetworkId;

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

    public Long getPhysicalNetworkId() {
    	return physicalNetworkId;
    }

    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("addNetworkAPIVlan command with vlanId=" + vlanId + " zoneId=" + zoneId + " networkOfferingId=" + networkOfferingId);
        	Network network = _ntwkAPIService.createNetworkFromNetworkAPIVlan(vlanId, zoneId, networkOfferingId, physicalNetworkId);
        	if (network != null) {
        		NetworkResponse response = _responseGenerator.createNetworkResponse(network);
        		response.setResponseName(getCommandName());
        		this.setResponseObject(response);
        	} else {
        		throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create network from NetworkAPI.");
        	}
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (ConfigurationException e) {
        	throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
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
