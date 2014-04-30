package com.globo.networkapi.manager;

import java.util.List;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.networkapi.NetworkAPIEnvironmentVO;
import com.globo.networkapi.model.Environment;
import com.globo.networkapi.model.Vlan;

public interface NetworkAPIService {


	/**
	 * Add a NetworkAPI Environment to an specific zone.
	 * @param physicalNetworkId
	 * @param name Name of the relationship, for example, BACKEND, FRONTEND
	 * @param napiEnvironmentId
	 * @return
	 */
	public NetworkAPIEnvironmentVO addNetworkAPIEnvironment(Long physicalNetworkId, String name, Long napiEnvironmentId);

	/**
	 * Create a new network in sync with NetworkAPI.
	 * 
	 * @param zoneId
	 * @param networkOfferingId
	 * @param physicalNetworkId
	 * @param networkDomain
	 * @param aclType
	 * @param accountName
	 * @param projectId
	 * @param domainId
	 * @param subdomainAccess
	 * @param displayNetwork
	 * @param aclId
	 * @return
	 */
	public Network createNetwork(String name, String displayText, Long zoneId,
			Long networkOfferingId, Long napiEnvironmentId,
			String networkDomain, ACLType aclType, String accountName,
			Long projectId, Long domainId, Boolean subdomainAccess,
			Boolean displayNetwork, Long aclId)
			throws ResourceAllocationException, ResourceUnavailableException,
			ConcurrentOperationException, InsufficientCapacityException;

	/**
	 * Create a new network based on existed vlanId from NetworkAPI.
	 * 
	 * @param vlanId Id (to the vlan number) of vlan.
	 * @param napiEnvironmentId Id of the environment in NetworkAPI
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
	public Network createNetworkFromNetworkAPIVlan(Long vlanId, Long napiEnvironmentId, Long zoneId,
			Long networkOfferingId, Long physicalNetworkId,
			String networkDomain, ACLType aclType, String accountName,
			Long projectId, Long domainId, Boolean subdomainAccess,
			Boolean displayNetwork, Long aclId)
			throws ResourceUnavailableException, 
			ResourceAllocationException, ConcurrentOperationException,
			InsufficientCapacityException;
	/**
	 * Validate if nicProfile in compatible with Network and destination dest.
	 * 
	 * @param NicProfile
	 * @param network
	 * @param dest
	 * @return
	 * @throws InsufficientVirtualNetworkCapcityException
	 * @throws InsufficientAddressCapacityException
	 */
	public Network validateNic(NicProfile nicProfile,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			Network network, DeployDestination dest)
			throws InsufficientVirtualNetworkCapcityException,
			InsufficientAddressCapacityException;

	/**
	 * Ensure network is created and active in NetworkAPI. If network is not created, create it.
	 * @param network
	 * @throws ConfigurationException
	 */
	public void implementNetwork(Network network) throws ConfigurationException;
	
	/**
	 * Remove network from NetworkAPI and inactive the vlan
	 * @param network
	 */
	public void removeNetworkFromNetworkAPI(Network network);
	
	/**
	 * Deallocate Vlan from NetworkAPI. 
	 * The vlan must have been previously inactivated with the 'removeNetworkFromNetworkAPI' method.
	 * @param network
	 */
	public void deallocateVlanFromNetworkAPI(Network network);
	
	/**
	 * List NetworkAPI environments from database with optional parameter physicalNetworkId or zoneId. If all parameters are null
	 * all NetworkAPIEnvironments from database are returned.
	 * @param physicalNetworkId
	 * @param zoneId
	 */
	public List<NetworkAPIEnvironmentVO> listNetworkAPIEnvironmentsFromDB(Long physicalNetworkId, Long zoneId);

	/**
	 * List all environments from NetworkAPI
	 * @param zoneId
	 * @return
	 */
	public List<Environment> listAllEnvironmentsFromNetworkApi(Long zoneId);
	
	boolean canEnable(Long physicalNetworkId);

	/**
	 * Removes the relationship between physical network and NetworkAPI environment
	 * @param physicalNetworkId
	 * @param napiEnvironmentId
	 * @return
	 */
	public boolean removeNetworkAPIEnvironment(Long physicalNetworkId, Long napiEnvironmentId);

	/**
	 * Add Network API host details (provider) to CloudStack
	 * @param physicalNetworkId
	 * @param username
	 * @param password
	 * @param url
	 */
	public Host addNetworkAPIHost(Long physicalNetworkId, String username,
			String password, String url);
	
	/**
	 * Retrieve VLAN info from Network API
	 * @param network
	 * @return
	 */
	public Vlan getVlanInfoFromNetworkAPI(Network network);

	/**
	 * Register VM NIC in Network API
	 * @param nic
	 * @param vm
	 * @param network
	 * @return
	 */
	public void registerNicInNetworkAPI(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, Network network);
}
