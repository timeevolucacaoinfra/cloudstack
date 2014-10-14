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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.user.Account;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.globonetwork.cloudstack.GloboNetworkEnvironmentVO;
import com.globo.globonetwork.cloudstack.GloboNetworkLBEnvironmentVO;
import com.globo.globonetwork.cloudstack.GloboNetworkVipAccVO;
import com.globo.globonetwork.cloudstack.response.GloboNetworkAllEnvironmentResponse.Environment;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;

public interface GloboNetworkService {


	/**
	 * Add a GloboNetwork Environment to an specific zone.
	 * @param physicalNetworkId
	 * @param name Name of the relationship, for example, BACKEND, FRONTEND
	 * @param napiEnvironmentId
	 * @return
	 */
	public GloboNetworkEnvironmentVO addGloboNetworkEnvironment(Long physicalNetworkId, String name, Long napiEnvironmentId);

	/**
	 * Create a new network in sync with GloboNetwork.
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
	 * Create a new network based on existed vlanId from GloboNetwork.
	 * 
	 * @param vlanId Id (to the vlan number) of vlan.
	 * @param globoNetworkEnvironmentId Id of the environment in GloboNetwork
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
	public Network createNetworkFromGloboNetworkVlan(Long vlanId, Long globoNetworkEnvironmentId, Long zoneId,
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
	 * Ensure network is created and active in GloboNetwork. If network is not created, create it.
	 * @param network
	 * @throws ConfigurationException
	 */
	public void implementNetwork(Network network) throws ConfigurationException;
	
	/**
	 * Remove network from GloboNetwork and inactivate the vlan
	 * @param network
	 */
	public void removeNetworkFromGloboNetwork(Network network);
	
	/**
	 * Deallocate Vlan from GloboNetwork. 
	 * The vlan must have been previously inactivated with the 'removeNetworkFromGloboNetwork' method.
	 * @param network
	 */
	public void deallocateVlanFromGloboNetwork(Network network);
	
	/**
	 * List GloboNetwork environments from database with optional parameter physicalNetworkId or zoneId. If all parameters are null
	 * all GloboNetwork environments from database are returned.
	 * @param physicalNetworkId
	 * @param zoneId
	 */
	public List<GloboNetworkEnvironmentVO> listGloboNetworkEnvironmentsFromDB(Long physicalNetworkId, Long zoneId);

	   /**
     * List GloboNetwork VIP environments from database according to a specific GloboNetwork Environment ID.
     * If this parameter is not passed, all GloboNetwork VIP environments from database are returned.
     * @param globoNetworkEnvironmentId
     */
    public List<GloboNetworkLBEnvironmentVO> listGloboNetworkVipEnvironmentsFromDB(Long globoNetworkEnvironmentId);

	/**
	 * List all environments from GloboNetwork
	 * @param zoneId
	 * @return
	 */
	public List<Environment> listAllEnvironmentsFromGloboNetwork(Long zoneId);
	
	boolean canEnable(Long physicalNetworkId);

	/**
	 * Removes the relationship between physical network and GloboNetwork environment
	 * @param physicalNetworkId
	 * @param globoNetworkEnvironmentId
	 * @return
	 */
	public boolean removeGloboNetworkEnvironment(Long physicalNetworkId, Long globoNetworkEnvironmentId);

	/**
	 * Add GloboNetwork host details (provider) to CloudStack
	 * @param physicalNetworkId
	 * @param username
	 * @param password
	 * @param url
	 */
	public Host addGloboNetworkHost(Long physicalNetworkId, String username,
			String password, String url);
	
	/**
	 * Register VM NIC in GloboNetwork
	 * @param nic
	 * @param vm
	 * @param network
	 */
	public void registerNicInGloboNetwork(NicProfile nic, VirtualMachineProfile vm, Network network);

	/**
	 * Unregister NIC in GloboNetwork
	 * @param nic
	 * @param vm
	 */
	public void unregisterNicInGloboNetwork(NicProfile nic, VirtualMachineProfile vm);
	
	/**
	 * List all GloboNetwork VIPs associated to an account/project
	 * @param projectId
	 * @return
	 */
	public List<GloboNetworkVipResponse> listGloboNetworkVips(Long projectId);

	/**
	 * Associate GloboNetwork VIP to an account and network in Cloudstack
	 * @param networkId
	 * @param globoNetworkVipId
	 * @return
	 */
	public GloboNetworkVipAccVO addGloboNetworkVipToAcc(Long networkId, Long globoNetworkVipId);
	
	/**
	 * Associate nic (real) to GloboNetwork Vip.
	 * @param vipId
	 * @param nicId
	 */
	public void associateNicToVip(Long vipId, Nic nic);

	/**
	 * Deassociate nic (real) from GloboNetwork Vip.
	 * @param vipId
	 * @param nicId
	 */
	public void disassociateNicFromVip(Long vipId, Nic nic);
	
	public String generateUrlForEditingVip(Long vipId, Network network);

	/**
	 * Remove Load Balancer (VIP) from GloboNetwork
	 * @param globoNetworkVipId
	 */
	public void removeGloboNetworkVip(Long globoNetworkVipId);

	public List<GloboNetworkVipResponse.Real> listGloboNetworkReals(Long vipId);

	public boolean applyLbRuleInGloboNetwork(Network network, LoadBalancingRule rule);
	
	public IpAddress allocate(Network network, Account owner) throws ConcurrentOperationException, ResourceAllocationException, InsufficientAddressCapacityException;
	
    public PublicIp acquireLbIp(Long networkId, Long projectId) throws ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException, InvalidParameterValueException, InsufficientCapacityException;
    
    public boolean releaseLbIpFromGloboNetwork(Network network, String ip);

    public boolean validateLBRule(Network network, LoadBalancingRule rule);
}
