package com.globo.networkapi.element;

import javax.naming.ConfigurationException;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;

public interface NetworkAPIService {
	
    public Network createNetworkFromNetworkAPIVlan(Long vlanId, Long zoneId, Long networkOfferingId, Long physicalNetworkId) throws ResourceUnavailableException, ConfigurationException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException;
	

}
