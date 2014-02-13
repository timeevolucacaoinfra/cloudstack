package com.globo.networkapi.element;

import javax.naming.ConfigurationException;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface NetworkAPIService {

	/**
	 * Create a new network based on vlanId from NetworkAPI.
	 * @param vlanId
	 * @param zoneId
	 * @param networkOfferingId
	 * @param physicalNetworkId
	 * @return
	 * @throws ResourceUnavailableException
	 * @throws ConfigurationException
	 * @throws ResourceAllocationException
	 * @throws ConcurrentOperationException
	 * @throws InsufficientCapacityException
	 */
    public Network createNetworkFromNetworkAPIVlan(Long vlanId, Long zoneId, Long networkOfferingId, Long physicalNetworkId) throws ResourceUnavailableException, ConfigurationException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException;
	
    /**
     * Validate if nicProfile in compatible with Network and destination dest.
     * @param NicProfile
     * @param network
     * @param dest
     * @return
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     */
    public Network validateNic(NicProfile nicProfile, VirtualMachineProfile<? extends VirtualMachine> vm, Network network, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException; 

}
