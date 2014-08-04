/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.globo.globonetwork.cloudstack.manager;

import java.util.List;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;

import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.globonetwork.client.model.Vlan;
import com.globo.globonetwork.cloudstack.GloboNetworkEnvironmentVO;
import com.globo.globonetwork.cloudstack.GloboNetworkVipAccVO;
import com.globo.globonetwork.cloudstack.response.GloboNetworkAllEnvironmentResponse.Environment;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;

public interface GloboNetworkService {


	/**
	 * Add a NetworkAPI Environment to an specific zone.
	 * @param physicalNetworkId
	 * @param name Name of the relationship, for example, BACKEND, FRONTEND
	 * @param napiEnvironmentId
	 * @return
	 */
	public GloboNetworkEnvironmentVO addNetworkAPIEnvironment(Long physicalNetworkId, String name, Long napiEnvironmentId);

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
			ConcurrentOperationException, CloudException;

	/**
	 * Validate if nicProfile in compatible with Network and destination dest.
	 * 
	 * @param NicProfile
	 * @param network
	 * @return
	 * @throws InsufficientVirtualNetworkCapcityException
	 * @throws InsufficientAddressCapacityException
	 */
	public Network validateNic(NicProfile nicProfile,
			VirtualMachineProfile vm,
			Network network)
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
	public List<GloboNetworkEnvironmentVO> listNetworkAPIEnvironmentsFromDB(Long physicalNetworkId, Long zoneId);

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
	 */
	public void registerNicInNetworkAPI(NicProfile nic, VirtualMachineProfile vm, Network network);

	/**
	 * Unregister NIC in Network API
	 * @param nic
	 * @param vm
	 */
	public void unregisterNicInNetworkAPI(NicProfile nic, VirtualMachineProfile vm);
	
	/**
	 * List all Network API VIPs associated to an account/project
	 * @param projectId
	 * @return
	 */
	public List<GloboNetworkVipResponse> listNetworkAPIVips(Long projectId);

	/**
	 * Associate Network API VIP to an account and network in Cloudstack
	 * @param networkId
	 * @param napiVipId
	 * @return
	 */
	public GloboNetworkVipAccVO addNapiVipToAcc(Long networkId, Long napiVipId);
	
	/**
	 * Associate nic (real) to NetworkAPI Vip.
	 * @param vipId
	 * @param nicId
	 */
	public void associateNicToVip(Long vipId, Nic nic);

	/**
	 * Deassociate nic (real) from NetworkAPI Vip.
	 * @param vipId
	 * @param nicId
	 */
	public void disassociateNicFromVip(Long vipId, Nic nic);
	
	public String generateUrlForEditingVip(Long vipId, Network network);

	/**
	 * Remove Load Balancer (VIP) from NetworkAPI
	 * @param napiVipId
	 */
	public void removeNapiVip(Long napiVipId);

	/**
	 * Find a single Netowrk API VIP
	 * @param vipId
	 * @param networkId 
	 * @return
	 */
	public List<GloboNetworkVipResponse.Real> listNetworkAPIReals(Long vipId);
}
