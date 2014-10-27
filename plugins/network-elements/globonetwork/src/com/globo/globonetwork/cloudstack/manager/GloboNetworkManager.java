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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Journal;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.NetUtils.supersetOrSubset;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.globo.globonetwork.cloudstack.GloboNetworkEnvironmentVO;
import com.globo.globonetwork.cloudstack.GloboNetworkLBNetworkVO;
import com.globo.globonetwork.cloudstack.GloboNetworkNetworkVO;
import com.globo.globonetwork.cloudstack.GloboNetworkVipAccVO;
import com.globo.globonetwork.cloudstack.api.AcquireNewIpForLbInGloboNetworkCmd;
import com.globo.globonetwork.cloudstack.api.AddGloboNetworkEnvironmentCmd;
import com.globo.globonetwork.cloudstack.api.AddGloboNetworkHostCmd;
import com.globo.globonetwork.cloudstack.api.AddGloboNetworkLBNetworkCmd;
import com.globo.globonetwork.cloudstack.api.AddGloboNetworkRealToVipCmd;
import com.globo.globonetwork.cloudstack.api.AddGloboNetworkVipToAccountCmd;
import com.globo.globonetwork.cloudstack.api.AddGloboNetworkVlanCmd;
import com.globo.globonetwork.cloudstack.api.AddNetworkViaGloboNetworkCmd;
import com.globo.globonetwork.cloudstack.api.DelGloboNetworkRealFromVipCmd;
import com.globo.globonetwork.cloudstack.api.GenerateUrlForEditingVipCmd;
import com.globo.globonetwork.cloudstack.api.ListAllEnvironmentsFromGloboNetworkCmd;
import com.globo.globonetwork.cloudstack.api.ListGloboNetworkEnvironmentsCmd;
import com.globo.globonetwork.cloudstack.api.ListGloboNetworkLBNetworksCmd;
import com.globo.globonetwork.cloudstack.api.ListGloboNetworkRealsCmd;
import com.globo.globonetwork.cloudstack.api.ListGloboNetworkVipsCmd;
import com.globo.globonetwork.cloudstack.api.RemoveGloboNetworkEnvironmentCmd;
import com.globo.globonetwork.cloudstack.api.RemoveGloboNetworkLBNetworkCmd;
import com.globo.globonetwork.cloudstack.api.RemoveGloboNetworkVipCmd;
import com.globo.globonetwork.cloudstack.commands.AcquireNewIpForLbCommand;
import com.globo.globonetwork.cloudstack.commands.ActivateNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.AddAndEnableRealInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.AddOrRemoveVipInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.CreateNewVlanInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.DeallocateVlanFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.DisableAndRemoveRealInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.GenerateUrlForEditingVipCommand;
import com.globo.globonetwork.cloudstack.commands.GetNetworkFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.GetVipInfoFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.GetVlanInfoFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.GloboNetworkErrorAnswer;
import com.globo.globonetwork.cloudstack.commands.ListAllEnvironmentsFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.RegisterEquipmentAndIpInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.ReleaseIpFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.RemoveNetworkInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.RemoveVipFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.UnregisterEquipmentAndIpInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.ValidateNicInVlanCommand;
import com.globo.globonetwork.cloudstack.dao.GloboNetworkEnvironmentDao;
import com.globo.globonetwork.cloudstack.dao.GloboNetworkLBNetworkDao;
import com.globo.globonetwork.cloudstack.dao.GloboNetworkNetworkDao;
import com.globo.globonetwork.cloudstack.dao.GloboNetworkVipAccDao;
import com.globo.globonetwork.cloudstack.exception.CloudstackGloboNetworkException;
import com.globo.globonetwork.cloudstack.resource.GloboNetworkResource;
import com.globo.globonetwork.cloudstack.response.GloboNetworkAllEnvironmentResponse;
import com.globo.globonetwork.cloudstack.response.GloboNetworkAllEnvironmentResponse.Environment;
import com.globo.globonetwork.cloudstack.response.GloboNetworkAndIPResponse;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse.Real;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVlanResponse;

@Component
@Local({GloboNetworkService.class, PluggableService.class})
public class GloboNetworkManager implements GloboNetworkService, PluggableService, Configurable {

	private static final Logger s_logger = Logger
			.getLogger(GloboNetworkManager.class);

	static final int NUMBER_OF_RESERVED_IPS_FROM_START = 5;
	static final int NUMBER_OF_RESERVED_IPS_BEFORE_END = 5;
    static final int NUMBER_OF_RESERVED_IPS_FOR_LB_FROM_START = 1;
    static final int NUMBER_OF_RESERVED_IPS_FOR_LB_BEFORE_END = 2;
	
	private static final ConfigKey<String> GloboNetworkVIPServerUrl = new ConfigKey<String>("Network", String.class, "globonetwork.vip.server.url", "", "Server URL to generate a new VIP request", true, ConfigKey.Scope.Global);
	private static final ConfigKey<String> GloboNetworkConnectionTimeout = new ConfigKey<String>("Network", String.class, "globonetwork.connectiontimeout", "120000", "GloboNetwork connection timeout (in milliseconds)", true, ConfigKey.Scope.Global);
	private static final ConfigKey<String> GloboNetworkReadTimeout = new ConfigKey<String>("Network", String.class, "globonetwork.readtimeout", "120000", "GloboNetwork read timeout (in milliseconds)", true, ConfigKey.Scope.Global);
	private static final ConfigKey<String> GloboNetworkNumberOfRetries = new ConfigKey<String>("Network", String.class, "globonetwork.numberofretries", "0", "GloboNetwork number of retries", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkVmEquipmentGroup = new ConfigKey<Long>("Network", Long.class, "globonetwork.vm.equipmentgroup", "", "Equipment group to be used when registering a VM NIC in GloboNetwork", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkModelVmUser = new ConfigKey<Long>("Network", Long.class, "globonetwork.model.vm.user", "83", "GloboNetwork model id to be used for User VMs", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkModelVmDomainRouter = new ConfigKey<Long>("Network", Long.class, "globonetwork.model.vm.domain.router", "84", "GloboNetwork model id to be used for Domain Router VMs", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkModelVmConsoleProxy = new ConfigKey<Long>("Network", Long.class, "globonetwork.model.vm.console.proxy", "85", "GloboNetwork model id to be used for Console Proxy VMs", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkModelVmSecondaryStorageVm = new ConfigKey<Long>("Network", Long.class, "globonetwork.model.vm.secondary.storage", "86", "GloboNetwork model id to be used for Secondary Storage VMs", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkModelVmElasticIpVm = new ConfigKey<Long>("Network", Long.class, "globonetwork.model.vm.elastic.ip", "87", "GloboNetwork model id to be used for Elastic IP VMs", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkModelVmElasticLoadBalancerVm = new ConfigKey<Long>("Network", Long.class, "globonetwork.model.vm.elastic.load.balancer", "88", "GloboNetwork model id to be used for Elastic Load Balancer VMs", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkModelVmInternalLoadBalancerVm = new ConfigKey<Long>("Network", Long.class, "globonetwork.model.vm.internal.load.balancer", "89", "GloboNetwork model id to be used for Internal Load Balancer VMs", true, ConfigKey.Scope.Global);
	private static final ConfigKey<Long> GloboNetworkModelVmUserBareMetal = new ConfigKey<Long>("Network", Long.class, "globonetwork.model.vm.user.bare.metal", "90", "GloboNetwork model id to be used for User Bare Metal", true, ConfigKey.Scope.Global);
	private static final ConfigKey<String> GloboNetworkDomainSuffix = new ConfigKey<String>("Network", String.class, "globonetwork.domain.suffix", "", "Domain suffix for all networks created with GloboNetwork", true, ConfigKey.Scope.Global);

	// DAOs
	@Inject
	DomainDao _domainDao;
	@Inject
	HostDao _hostDao;
	@Inject
	DataCenterDao _dcDao;
	@Inject
	HostPodDao _hostPodDao;
	@Inject
	VlanDao _vlanDao;
	@Inject
	PhysicalNetworkDao _physicalNetworkDao;
	@Inject
	NetworkOfferingDao _networkOfferingDao;
	@Inject
	ConfigurationDao _configDao;
	@Inject
	UserDao _userDao;
	@Inject
	NetworkDao _ntwkDao;
    @Inject
    NicDao _nicDao;
	@Inject
	NetworkServiceMapDao _ntwkSrvcDao;
	@Inject
	GloboNetworkNetworkDao _globoNetworkNetworkDao;
	@Inject
	GloboNetworkEnvironmentDao _globoNetworkEnvironmentDao;
	@Inject
	GloboNetworkVipAccDao _globoNetworkVipAccDao;
	@Inject
	GloboNetworkLBNetworkDao _globoNetworkLBNetworkDao;
    @Inject
    VMInstanceDao _vmDao;
	
	// Managers
	@Inject
	NetworkModel _networkManager;
	@Inject
	AgentManager _agentMgr;
	@Inject
	ConfigurationManager _configMgr;
	@Inject
	ResourceManager _resourceMgr;
	@Inject
	DomainManager _domainMgr;
	@Inject
	NetworkOrchestrationService _networkMgr;
	@Inject
	AccountManager _accountMgr;
    @Inject
    ProjectManager _projectMgr;
	@Inject
	NetworkService _ntwSvc;
	@Inject
	IpAddressManager _ipAddrMgr;
	@Inject
	LoadBalancingRulesManager _lbMgr;
	
	@Override
	public boolean canEnable(Long physicalNetworkId) {
		if (physicalNetworkId == null) {
			return false;
		}
		List<GloboNetworkEnvironmentVO> list = _globoNetworkEnvironmentDao.listByPhysicalNetworkId(physicalNetworkId);
		if (list.isEmpty()) {
			throw new CloudRuntimeException("Before enabling GloboNetwork you must add an environment to your physical interface");
		}
		return true;
	}

	@Override
    @DB
	public Network createNetwork(String name, String displayText, Long zoneId,
			Long networkOfferingId, Long napiEnvironmentId,
			String networkDomain, ACLType aclType, String accountName,
			Long projectId, Long domainId, Boolean subdomainAccess,
			Boolean displayNetwork, Long aclId)
			throws ResourceAllocationException, ResourceUnavailableException,
			ConcurrentOperationException, InsufficientCapacityException {

		Account caller = CallContext.current().getCallingAccount();

		if ((accountName != null && domainId != null) || projectId != null) {
			_accountMgr.finalizeOwner(caller, accountName, domainId,
					projectId);
		}

		DataCenter zone = _dcDao.findById(zoneId);
		if (zone == null) {
			throw new InvalidParameterValueException(
					"Specified zone id was not found");
		}
		
		Long physicalNetworkId = null;
		if (napiEnvironmentId != null) {
			GloboNetworkEnvironmentVO napiEnvironmentVO = null;
			for (PhysicalNetwork pNtwk : _physicalNetworkDao.listByZone(zoneId)) {
				napiEnvironmentVO = _globoNetworkEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(pNtwk.getId(), napiEnvironmentId);
				if (napiEnvironmentVO != null) {
					break;
				}
			}
			if (napiEnvironmentVO == null) {
				throw new InvalidParameterValueException("Unable to find a relationship between GloboNetwork environment and physical network");
			}
			physicalNetworkId = napiEnvironmentVO.getPhysicalNetworkId();
		} else {
			throw new InvalidParameterValueException("GloboNetwork enviromentId was not found");
		}
		
		Answer answer = createNewVlan(zoneId, name, displayText, napiEnvironmentId);

		GloboNetworkVlanResponse response = (GloboNetworkVlanResponse) answer;
		Long napiVlanId = response.getVlanId();

		Network network = null;
		
		try {
			network = this.createNetworkFromGloboNetworkVlan(napiVlanId, napiEnvironmentId, zoneId,
				networkOfferingId, physicalNetworkId, networkDomain, aclType,
				accountName, projectId, domainId, subdomainAccess,
				displayNetwork, aclId);
		} catch (Exception e) {
			// Exception when creating network in Cloudstack. Roll back transaction in GloboNetwork
			s_logger.error("Reverting network creation in GloboNetwork due to error creating network", e);
			this.deallocateVlanFromGloboNetwork(zoneId, napiVlanId);
			
			throw new ResourceAllocationException(e.getLocalizedMessage(), ResourceType.network);
		}

		// if the network offering has persistent set to true, implement the
		// network
		// FIXME While we have same issues with ACL API with net not in equipment, all
		// networks are considered persistent.
		// NetworkOfferingVO ntwkOff = _networkOfferingDao.findById(networkOfferingId);
		if (true /*ntwkOff.getIsPersistent()*/) {
			try {
				if (network.getState() == Network.State.Setup) {
					s_logger.debug("Network id=" + network.getId()
							+ " is already provisioned");
					return network;
				}
				DeployDestination dest = new DeployDestination(zone, null,
						null, null);
				UserVO callerUser = _userDao.findById(CallContext.current()
						.getCallingUserId());
				Journal journal = new Journal.LogJournal("Implementing "
						+ network, s_logger);
				ReservationContext context = new ReservationContextImpl(UUID
						.randomUUID().toString(), journal, callerUser, caller);
				s_logger.debug("Implementing network "
						+ network
						+ " as a part of network provision for persistent network");
				@SuppressWarnings("unchecked")
				Pair<NetworkGuru, NetworkVO> implementedNetwork = (Pair<NetworkGuru, NetworkVO>) _networkMgr
						.implementNetwork(network.getId(), dest, context);
				if (implementedNetwork.first() == null) {
					s_logger.warn("Failed to provision the network " + network);
				}
				network = implementedNetwork.second();
			} catch (ResourceUnavailableException ex) {
				s_logger.warn("Failed to implement persistent guest network "
						+ network + "due to ", ex);
				CloudRuntimeException e = new CloudRuntimeException(
						"Failed to implement persistent guest network");
				e.addProxyObject(network.getUuid(), "networkId");
				throw e;
			}
		}


		return network;
	}

	@Override
    @DB
	public Network createNetworkFromGloboNetworkVlan(final Long vlanId, final Long napiEnvironmentId, Long zoneId,
			final Long networkOfferingId, final Long physicalNetworkId,
			final String networkDomain, final ACLType aclType, String accountName, Long projectId,
			final Long domainId, final Boolean subdomainAccess, final Boolean displayNetwork,
			Long aclId) throws CloudException, ResourceUnavailableException,
			ResourceAllocationException, ConcurrentOperationException,
			InsufficientCapacityException {

		final Account caller = CallContext.current().getCallingAccount();
		
		final Account owner;
		if ((accountName != null && domainId != null) || projectId != null) {
			owner = _accountMgr.finalizeOwner(caller, accountName, domainId,
					projectId);
		} else {
			owner = caller;
		}

        // Only domain and account ACL types are supported in Action.
        if (aclType == null || !(aclType == ACLType.Domain || aclType == ACLType.Account)) {
            throw new InvalidParameterValueException("AclType should be " + ACLType.Domain + " or " +
            		ACLType.Account + " for network of type " + Network.GuestType.Shared);
        }
		
		final boolean isDomainSpecific = true;

		// Validate network offering
		final NetworkOfferingVO ntwkOff = _networkOfferingDao
				.findById(networkOfferingId);
		if (ntwkOff == null || ntwkOff.isSystemOnly()) {
			InvalidParameterValueException ex = new InvalidParameterValueException(
					"Unable to find network offering by specified id");
			if (ntwkOff != null) {
				ex.addProxyObject(ntwkOff.getUuid(), "networkOfferingId");
			}
			throw ex;
		} else if (GuestType.Shared != ntwkOff.getGuestType()) {
			InvalidParameterValueException ex = new InvalidParameterValueException(
					"GloboNetwork can handle only network offering with guest type shared");
			if (ntwkOff != null) {
				ex.addProxyObject(ntwkOff.getUuid(), "networkOfferingId");
			}
			throw ex;
		}
		// validate physical network and zone
		// Check if physical network exists
		final PhysicalNetwork pNtwk;
		if (physicalNetworkId != null) {
			pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
			if (pNtwk == null) {
				throw new InvalidParameterValueException(
						"Unable to find a physical network having the specified physical network id");
			}
		} else {
			throw new InvalidParameterValueException(
					"invalid physicalNetworkId " + physicalNetworkId);
		}

		if (zoneId == null) {
			zoneId = pNtwk.getDataCenterId();
		}

		final DataCenter zone = _dcDao.findById(zoneId);
		if (zone == null) {
			throw new InvalidParameterValueException(
					"Specified zone id was not found");
		}

		if (Grouping.AllocationState.Disabled == zone.getAllocationState()
				&& !_accountMgr.isAdmin(caller.getType())) {
			// See DataCenterVO.java
			PermissionDeniedException ex = new PermissionDeniedException(
					"Cannot perform this operation since specified Zone is currently disabled");
			ex.addProxyObject(zone.getUuid(), "zoneId");
			throw ex;
		}

		if (domainId != null) {
			DomainVO domain = _domainDao.findById(domainId);
			if (domain == null) {
				throw new InvalidParameterValueException(
						"Unable to find domain by specified id");
			}
			_accountMgr.checkAccess(caller, domain);
		}

		if (_configMgr.isOfferingForVpc(ntwkOff)) {
			throw new InvalidParameterValueException(
					"Network offering can't be used for VPC networks");
		}

		// CallContext.register(CallContext.current().getCallingUserId(), owner.getAccountId());

		/////// GloboNetwork specific code ///////

		// Get VlanInfo from GloboNetwork
		GetVlanInfoFromGloboNetworkCommand cmd = new GetVlanInfoFromGloboNetworkCommand();
		cmd.setVlanId(vlanId);

		final GloboNetworkVlanResponse response = (GloboNetworkVlanResponse) callCommand(cmd, zoneId);

		long networkAddresLong = response.getNetworkAddress().toLong();
		String networkAddress = NetUtils.long2Ip(networkAddresLong);
		final String netmask = response.getMask().ip4();
		final String cidr = NetUtils.ipAndNetMaskToCidr(networkAddress, netmask);

		String ranges[] = NetUtils.ipAndNetMaskToRange(networkAddress, netmask);
		final String gateway = ranges[0];
		final String startIP = NetUtils.long2Ip(NetUtils.ip2Long(ranges[0])
				+ NUMBER_OF_RESERVED_IPS_FROM_START);
		final String endIP = NetUtils.long2Ip(NetUtils.ip2Long(ranges[1])
				- NUMBER_OF_RESERVED_IPS_BEFORE_END);
		final Long vlanNum = response.getVlanNum();
		// NO IPv6 support yet
		final String startIPv6 = null;
		final String endIPv6 = null;
		final String ip6Gateway = null;
		final String ip6Cidr = null;

		s_logger.info("Creating network with name " + response.getVlanName()
				+ " (" + response.getVlanId() + "), network " + networkAddress
				+ " gateway " + gateway + " startIp " + startIP + " endIp "
				+ endIP + " cidr " + cidr);
		/////// End of GloboNetwork specific code ///////

		 Network network = Transaction.execute(new TransactionCallbackWithException<Network, CloudException>() {

			@Override
			public Network doInTransaction(TransactionStatus status) throws CloudException {
				Boolean newSubdomainAccess = subdomainAccess;
				Long sharedDomainId = null;
				if (isDomainSpecific) {
					if (domainId != null) {
						sharedDomainId = domainId;
					} else {
						sharedDomainId = owner.getDomainId();
						newSubdomainAccess = true;
					}
				}
				
				String newNetworkDomain = networkDomain;
				if (!StringUtils.isNotBlank(newNetworkDomain)) {
					/* Create new domain in DNS */
					String domainSuffix = GloboNetworkDomainSuffix.value();
					// domainName is of form 'zoneName-vlanNum.domainSuffix'
					if (domainSuffix == null) {
						domainSuffix = "";
					} else if (!domainSuffix.startsWith(".")) {
						domainSuffix = "." + domainSuffix;
					}
			    	newNetworkDomain = (zone.getName() + "-" + String.valueOf(response.getVlanNum()) + domainSuffix).toLowerCase();
				}

				Network network = _networkMgr.createGuestNetwork(
						networkOfferingId.longValue(), response.getVlanName(),
						response.getVlanDescription(), gateway, cidr,
						String.valueOf(response.getVlanNum()), newNetworkDomain, owner,
						sharedDomainId, pNtwk, zone.getId(), aclType, newSubdomainAccess, 
						null, // vpcId,
						ip6Gateway,
						ip6Cidr,
						displayNetwork,
						null // isolatedPvlan
						);
				
				// Save relashionship with napi and network
				GloboNetworkNetworkVO napiNetworkVO = new GloboNetworkNetworkVO(vlanId,
						network.getId(), napiEnvironmentId);
				napiNetworkVO = _globoNetworkNetworkDao.persist(napiNetworkVO);

				// if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
					// Create vlan ip range
					_configMgr.createVlanAndPublicIpRange(pNtwk.getDataCenterId(),
							network.getId(), physicalNetworkId, false, (Long) null,
							startIP, endIP, gateway, netmask, vlanNum.toString(), null,
							startIPv6, endIPv6, ip6Gateway, ip6Cidr);
				// }
				return network;
			}
		});
		return network;
	}

	protected GloboNetworkVlanResponse createNewVlan(Long zoneId, String name,
			String description, Long globoNetworkEnvironmentId) {

		CreateNewVlanInGloboNetworkCommand cmd = new CreateNewVlanInGloboNetworkCommand();
		cmd.setVlanName(name);
		cmd.setVlanDescription(description);
		cmd.setGloboNetworkEnvironmentId(globoNetworkEnvironmentId);

		return (GloboNetworkVlanResponse) callCommand(cmd, zoneId);
	}

    private Answer callCommand(Command cmd, Long zoneId) {
        return callCommand(cmd, zoneId, true);
    }

	private Answer callCommand(Command cmd, Long zoneId, boolean raisesExceptionWhenNoAnswer) {
		
		HostVO napiHost = getGloboNetworkHost(zoneId);
		if (napiHost == null) {
			throw new CloudstackGloboNetworkException("Could not find the GloboNetwork resource");
		}
		
		Answer answer = _agentMgr.easySend(napiHost.getId(), cmd);
		if (answer == null || !answer.getResult()) {
			
			if (answer instanceof GloboNetworkErrorAnswer) {
				GloboNetworkErrorAnswer napiAnswer = (GloboNetworkErrorAnswer) answer; 
				throw new CloudstackGloboNetworkException(napiAnswer.getNapiCode(), napiAnswer.getNapiDescription());
			} else {
			    if (raisesExceptionWhenNoAnswer) {
    				String msg = "Error executing command " + cmd + ". Maybe GloboNetwork Host is down";
    				msg = answer == null ? msg : answer.getDetails();
    				throw new CloudRuntimeException(msg);
			    }
			}
		}
		return answer;
	}
	
	private HostVO getGloboNetworkHost(Long zoneId) {
		return _hostDao.findByTypeNameAndZoneId(zoneId, Provider.GloboNetwork.getName(), Type.L2Networking);
	}

	@Override
	public Network validateNic(NicProfile nicProfile,
			VirtualMachineProfile vm, Network network)
			throws InsufficientVirtualNetworkCapcityException,
			InsufficientAddressCapacityException {

		ValidateNicInVlanCommand cmd = new ValidateNicInVlanCommand();
		cmd.setNicIp(nicProfile.getIp4Address());
		cmd.setVlanId(getGloboNetworkVlanId(network.getId()));
		cmd.setVlanNum(Long.valueOf(getVlanNum(nicProfile.getBroadCastUri())));

		String msg = "Unable to validate nic " + nicProfile + " from VM " + vm;
		Answer answer = this.callCommand(cmd, network.getDataCenterId());
		if (answer == null || !answer.getResult()) {
			msg = answer == null ? msg : answer.getDetails();
			throw new InsufficientVirtualNetworkCapcityException(msg, Nic.class, nicProfile.getId());
		}

		// everything is ok
		return network;
	}

	@Override
	public void implementNetwork(Network network) throws ConfigurationException {
		Long vlanId = getGloboNetworkVlanId(network.getId());
		if (vlanId == null) {
			throw new CloudRuntimeException("Inconsistency. Network "
					+ network.getName()
					+ " there is not relation with GloboNetwork");
		}

		GetVlanInfoFromGloboNetworkCommand cmd = new GetVlanInfoFromGloboNetworkCommand();
		cmd.setVlanId(vlanId);

		Answer answer = callCommand(cmd, network.getDataCenterId());

		GloboNetworkVlanResponse vlanResponse = (GloboNetworkVlanResponse) answer;
		vlanResponse.getNetworkAddress();
		Long networkId = vlanResponse.getNetworkId();
		if (!vlanResponse.isActive()) {
			// Create network in equipment
			ActivateNetworkCommand cmd_creation = new ActivateNetworkCommand(vlanId,
					networkId);
			Answer creation_answer = callCommand(cmd_creation, network.getDataCenterId());
			if (creation_answer == null || !creation_answer.getResult()) {
				throw new CloudRuntimeException(
						"Unable to create network in GloboNetwork: VlanId "
								+ vlanId + " networkId " + networkId);
			}
			s_logger.info("Network ready to use: VlanId " + vlanId
					+ " networkId " + networkId);
		} else {
			s_logger.warn("Network already created in GloboNetwork: VlanId "
					+ vlanId + " networkId " + networkId);
		}
	}

	/**
	 * Returns VlanId (in GloboNetwork) given an Network. If network is not
	 * associated with GloboNetwork, <code>null</code> will be returned.
	 * 
	 * @param networkId
	 * @return
	 */
	private Long getGloboNetworkVlanId(Long networkId) {
		if (networkId == null) {
			return null;
		}
		GloboNetworkNetworkVO vo = _globoNetworkNetworkDao.findByNetworkId(networkId);
		if (vo == null) {
			return null;
		}
		return vo.getGloboNetworkVlanId();
	}

	/**
	 * Get the number of vlan associate with {@code network}.
	 * 
	 * @param network
	 * @return
	 */
	private Integer getVlanNum(URI broadcastUri) {
		if (broadcastUri == null) {
			return null;
		}
		try {
			Integer vlanNum = Integer.valueOf(broadcastUri.getHost());
			return vlanNum;
		} catch (NumberFormatException nfe) {
			String msg = "Invalid Vlan number in broadcast URI " + broadcastUri;
			s_logger.error(msg);
			throw new CloudRuntimeException(msg, nfe);
		}
	}
	
	@Override
	@DB
	public GloboNetworkEnvironmentVO addGloboNetworkEnvironment(Long physicalNetworkId, String name, Long globoNetworkEnvironmentId) {
		
		if (name == null || name.trim().isEmpty()) {
			throw new InvalidParameterValueException("Invalid name: " + name);
		}
		
		// validate physical network and zone
		// Check if physical network exists
		PhysicalNetwork pNtwk = null;
		if (physicalNetworkId != null) {
			pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
			if (pNtwk == null) {
				throw new InvalidParameterValueException(
						"Unable to find a physical network having the specified physical network id");
			}
		} else {
			throw new InvalidParameterValueException("Invalid physicalNetworkId: " + physicalNetworkId);
		}

		Long zoneId = pNtwk.getDataCenterId();
		
		// now, check if environment exists in GloboNetwork
		if (globoNetworkEnvironmentId != null) {
			Environment environment = getEnvironment(physicalNetworkId, globoNetworkEnvironmentId);
			if (environment == null) {
				throw new InvalidParameterValueException(
						"Unable to find in GloboNetwork an enviroment having the specified environment id");
			}
		} else {
			throw new InvalidParameterValueException("Invalid GloboNetwork environmentId: " + globoNetworkEnvironmentId);
		}
		
		
		// Check if there is a environment with same id or name in this zone.
		List<GloboNetworkEnvironmentVO> globoNetworkEnvironments = listGloboNetworkEnvironmentsFromDB(null, zoneId);
		for (GloboNetworkEnvironmentVO globoNetworkEnvironment: globoNetworkEnvironments) {
			if (globoNetworkEnvironment.getName().equalsIgnoreCase(name)) {
				throw new InvalidParameterValueException("GloboNetwork environment with name " + name + " already exists in zone " + zoneId);
			}
			if (globoNetworkEnvironment.getGloboNetworkEnvironmentId() == globoNetworkEnvironmentId) {
				throw new InvalidParameterValueException("GloboNetwork environment with environmentId " + globoNetworkEnvironmentId + " already exists in zoneId " + zoneId);
			}
		}
				
	    GloboNetworkEnvironmentVO napiEnvironmentVO = new GloboNetworkEnvironmentVO(physicalNetworkId, name, globoNetworkEnvironmentId);
	    _globoNetworkEnvironmentDao.persist(napiEnvironmentVO);
	    return napiEnvironmentVO;
	}
	
    @Override
    @DB
    public GloboNetworkLBNetworkVO addGloboNetworkLBNetwork(final String name, Long physicalNetworkId, Long globoNetworkEnvironmentId, final Long globoNetworkLBNetworkId) throws ResourceAllocationException {
        
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidParameterValueException("Invalid name: " + name);
        }
        
        // validate physical network and zone
        // Check if physical network exists
        PhysicalNetwork pNtwk = null;
        if (physicalNetworkId != null) {
            pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNtwk == null) {
                throw new InvalidParameterValueException(
                        "Unable to find a physical network having the specified physical network id");
            }
        } else {
            throw new InvalidParameterValueException("Invalid physicalNetworkId: " + physicalNetworkId);
        }
        
        // Check if there is a environment with same id or name in this zone.
        final GloboNetworkEnvironmentVO globoNetworkEnvironment = _globoNetworkEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(pNtwk.getId(), globoNetworkEnvironmentId);
        if (globoNetworkEnvironment == null) {
            throw new InvalidParameterValueException("Could not find a relationship between GloboNetwork Environment " + globoNetworkEnvironmentId + " and physical network " + physicalNetworkId);
        }
        
        // Find out if LB Network exists in GloboNetwork
        GetNetworkFromGloboNetworkCommand cmd = new GetNetworkFromGloboNetworkCommand(globoNetworkLBNetworkId);
        Answer answer = callCommand(cmd, pNtwk.getDataCenterId(), false);
        // If code reaches this point, LB Network exists in GloboNetwork
        
        // Check if there is a LB network with same id or name in this zone.
        List<GloboNetworkLBNetworkVO> globoNetworkLBNetworks = listGloboNetworkLBNetworksFromDB(pNtwk.getId(), globoNetworkEnvironmentId);
        for (GloboNetworkLBNetworkVO globoNetworkLBNetwork: globoNetworkLBNetworks) {
            if (globoNetworkLBNetwork.getName().equalsIgnoreCase(name)) {
                throw new InvalidParameterValueException("LB network with name " + name + " already exists.");
            }
            if (globoNetworkLBNetwork.getGloboNetworkLBNetworkId() == globoNetworkLBNetworkId) {
                throw new InvalidParameterValueException("Relationship between Environment " + globoNetworkEnvironmentId + " and LB Network " + globoNetworkLBNetworkId + " already exists.");
            }
        }
        
        final Long zoneId = pNtwk.getDataCenterId();
        final GloboNetworkAndIPResponse response = (GloboNetworkAndIPResponse) answer;
        try {
            GloboNetworkLBNetworkVO globoNetworkLBNetworkVO = Transaction.execute(new TransactionCallbackWithException<GloboNetworkLBNetworkVO, CloudException>() {

                @Override
                public GloboNetworkLBNetworkVO doInTransaction(TransactionStatus status) throws CloudException {
                    Integer vlanNumber = response.getVlanNum();
                    VlanVO vlan = getPublicVlanFromZoneVlanNumberAndNetwork(zoneId, vlanNumber, response.getNetworkCidr(), response.getNetworkGateway());
                    if (vlan == null) {
                        vlan = createNewPublicVlan(zoneId, vlanNumber, response.getNetworkCidr(), response.getNetworkGateway());
                    }
                    
                    GloboNetworkLBNetworkVO globoNetworkLBNetworkVO = new GloboNetworkLBNetworkVO(name, globoNetworkEnvironment.getId(), globoNetworkLBNetworkId, vlan.getId());
                    _globoNetworkLBNetworkDao.persist(globoNetworkLBNetworkVO);
                    return globoNetworkLBNetworkVO;
                }
            });
            
            return globoNetworkLBNetworkVO;
            
        } catch (CloudException e) {
            // Exception when defining IP ranges in Cloudstack
            throw new ResourceAllocationException(e.getLocalizedMessage(), ResourceType.public_ip);
        }
    }

	@Override
	@DB
	public Host addGloboNetworkHost(Long physicalNetworkId, String username, String password, String url) {
		
		if (username == null || username.trim().isEmpty()) {
			throw new InvalidParameterValueException("Invalid username: " + username);
		}
		
		if (password == null || password.trim().isEmpty()) {
			throw new InvalidParameterValueException("Invalid password: " + password);
		}
		
		if (url == null || url.trim().isEmpty()) {
			throw new InvalidParameterValueException("Invalid url: " + url);
		}
		

		// validate physical network and zone
		// Check if physical network exists
		PhysicalNetwork pNtwk = null;
		if (physicalNetworkId != null) {
			pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
			if (pNtwk == null) {
				throw new InvalidParameterValueException(
						"Unable to find a physical network having the specified physical network id");
			}
		} else {
			throw new InvalidParameterValueException("Invalid physicalNetworkId: " + physicalNetworkId);
		}

		final Long zoneId = pNtwk.getDataCenterId();

		final Map<String, String> params = new HashMap<String, String>();
		params.put("guid", "globonetwork-" + String.valueOf(zoneId));
		params.put("zoneId", String.valueOf(zoneId));
		params.put("name", Provider.GloboNetwork.getName());
		
		String readTimeout = GloboNetworkReadTimeout.value();
		String connectTimeout = GloboNetworkConnectionTimeout.value();
		String numberOfRetries = GloboNetworkNumberOfRetries.value();

		params.put("url", url);
		params.put("username", username);
		params.put("password", password);
		params.put("readTimeout", readTimeout);
		params.put("connectTimeout", connectTimeout);
		params.put("numberOfRetries", numberOfRetries);

		final Map<String, Object> hostDetails = new HashMap<String, Object>();
		hostDetails.putAll(params);
		
		Host host = Transaction.execute(new TransactionCallback<Host>() {

			@Override
			public Host doInTransaction(TransactionStatus status) {
				GloboNetworkResource resource = new GloboNetworkResource();

				try {
					resource.configure(Provider.GloboNetwork.getName(), hostDetails);
					
					Host host = _resourceMgr.addHost(zoneId, resource, resource.getType(),
							params);
					return host;
				} catch (ConfigurationException e) {
		            throw new CloudRuntimeException(e);
				}
			}
		});
       return host;
	}

	@Override
	public List<Class<?>> getCommands() {
		List<Class<?>> cmdList = new ArrayList<Class<?>>();
		cmdList.add(AddGloboNetworkVlanCmd.class);
		cmdList.add(AddNetworkViaGloboNetworkCmd.class);
		cmdList.add(AddGloboNetworkEnvironmentCmd.class);
		cmdList.add(ListGloboNetworkEnvironmentsCmd.class);
		cmdList.add(ListAllEnvironmentsFromGloboNetworkCmd.class);
		cmdList.add(RemoveGloboNetworkEnvironmentCmd.class);
		cmdList.add(AddGloboNetworkHostCmd.class);
		cmdList.add(AddGloboNetworkVipToAccountCmd.class);
		cmdList.add(ListGloboNetworkVipsCmd.class);
		cmdList.add(AddGloboNetworkRealToVipCmd.class);
		cmdList.add(DelGloboNetworkRealFromVipCmd.class);
		cmdList.add(GenerateUrlForEditingVipCmd.class);
		cmdList.add(RemoveGloboNetworkVipCmd.class);
		cmdList.add(ListGloboNetworkRealsCmd.class);
		cmdList.add(AcquireNewIpForLbInGloboNetworkCmd.class);
		return cmdList;
	}
	
	@Override
	public List<Environment> listAllEnvironmentsFromGloboNetwork(Long physicalNetworkId) {
		
		// validate physical network and zone
		// Check if physical network exists
		PhysicalNetwork pNtwk = null;
		if (physicalNetworkId != null) {
			pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
			if (pNtwk == null) {
				throw new InvalidParameterValueException(
						"Unable to find a physical network having the specified physical network id");
			}
		} else {
			throw new InvalidParameterValueException("Invalid physicalNetworkId: " + physicalNetworkId);
		}
				
		Long zoneId = pNtwk.getDataCenterId();
		
		ListAllEnvironmentsFromGloboNetworkCommand cmd = new ListAllEnvironmentsFromGloboNetworkCommand();
		
		Answer answer = callCommand(cmd, zoneId);
		
		List<Environment> environments =  ((GloboNetworkAllEnvironmentResponse) answer).getEnvironmentList();
		return environments;
	}
	
	/**
	 * Get <code>Environment</code> object from environmentId.
	 * @param environmentId
	 * @return Return null if environment was not found.
	 */
	protected Environment getEnvironment(Long physicaNetworkId, Long environmentId) {
		if (environmentId == null) {
			return null;
		}
		
		Environment resultEnvironment = null;
		for (Environment environment : listAllEnvironmentsFromGloboNetwork(physicaNetworkId)) {
			if (environmentId.equals(environment.getId())) {
				resultEnvironment = environment;
				break;
			}
		}
		return resultEnvironment;
	}

	private void handleNetworkUnavaiableError(CloudstackGloboNetworkException e) {
		if (e.getNapiCode() == 116) {
			// If this is the return code, it means that the vlan/network no longer exists in GloboNetwork
			// and we should continue to remove it from CloudStack
			s_logger.warn("Inconsistency between CloudStack and GloboNetwork");
			return;
		} else {
			// Otherwise, there was a different error and we should abort the operation
			throw e;
		}
	}
	
	@Override
	public void removeNetworkFromGloboNetwork(Network network) {
		
		try {
            // TODO Put code to ensure vlan is valid in resource
			// Make sure the VLAN is valid
	        GetVlanInfoFromGloboNetworkCommand cmd = new GetVlanInfoFromGloboNetworkCommand();
	        Long vlanId = getGloboNetworkVlanId(network.getId());
	        cmd.setVlanId(vlanId);
	    
	        callCommand(cmd, network.getDataCenterId());
	        // if can get vlan information, it's ok to remove it.
		
			RemoveNetworkInGloboNetworkCommand cmd2 = new RemoveNetworkInGloboNetworkCommand();
			cmd2.setVlanId(vlanId);

			callCommand(cmd2, network.getDataCenterId());
		} catch (CloudstackGloboNetworkException e) {
			handleNetworkUnavaiableError(e);
		}
	}
	
	@Override
	@DB
	public void deallocateVlanFromGloboNetwork(Network network) {

		try {
			GloboNetworkNetworkVO napiNetworkVO = _globoNetworkNetworkDao.findByNetworkId(network.getId());
			if (napiNetworkVO != null) {
				this.deallocateVlanFromGloboNetwork(network.getDataCenterId(), napiNetworkVO.getGloboNetworkVlanId());
				_globoNetworkNetworkDao.remove(napiNetworkVO.getId());
			}
			
		} catch (CloudstackGloboNetworkException e) {
			handleNetworkUnavaiableError(e);
		}
	}
	
	public void deallocateVlanFromGloboNetwork(Long zoneId, Long vlanId) {
		
		DeallocateVlanFromGloboNetworkCommand cmd = new DeallocateVlanFromGloboNetworkCommand();
		cmd.setVlanId(vlanId);
		
		this.callCommand(cmd, zoneId);
	}
	
	@Override
	public List<GloboNetworkEnvironmentVO> listGloboNetworkEnvironmentsFromDB(Long physicalNetworkId, Long zoneId) {
		List<GloboNetworkEnvironmentVO> globoNetworkEnvironmentsVOList;

		if (physicalNetworkId != null) {
			// Check if physical network exists
			PhysicalNetwork pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
			if (pNtwk == null) {
				throw new InvalidParameterValueException(
						"Unable to find a physical network having the specified physical network id");
			}
			
			globoNetworkEnvironmentsVOList = _globoNetworkEnvironmentDao.listByPhysicalNetworkId(physicalNetworkId);

		} else if (zoneId != null) {
			// Check if zone exists
			DataCenter zone = _dcDao.findById(zoneId);
			if (zone == null) {
				throw new InvalidParameterValueException(
						"Specified zone id was not found");
			}

			globoNetworkEnvironmentsVOList = new ArrayList<GloboNetworkEnvironmentVO>();
			for (PhysicalNetworkVO physicalNetwork : _physicalNetworkDao.listByZone(zoneId)) {
				List<GloboNetworkEnvironmentVO> partialResult = _globoNetworkEnvironmentDao.listByPhysicalNetworkId(physicalNetwork.getId());
				if (partialResult != null) {
					globoNetworkEnvironmentsVOList.addAll(partialResult);
				}
			}
		} else {
			globoNetworkEnvironmentsVOList = _globoNetworkEnvironmentDao.listAll();
		}
		
		return globoNetworkEnvironmentsVOList;
	}
    
    @Override
    public List<GloboNetworkLBNetworkVO> listGloboNetworkLBNetworksFromDB(Long physicalNetworkId, Long globoNetworkEnvironmentId) {

        // Retrieve napiEnvironment from DB
        GloboNetworkEnvironmentVO globoNetworkEnvironment = _globoNetworkEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(physicalNetworkId, globoNetworkEnvironmentId);
        
        if (globoNetworkEnvironment == null) {
            // No physical network/environment pair registered in the database.
            throw new InvalidParameterValueException("Unable to find a relationship between physical network=" + physicalNetworkId + " and GloboNetwork environment=" + globoNetworkEnvironmentId);
        }

        List<GloboNetworkLBNetworkVO> globoNetworkLBNetworkVOList;

        if (globoNetworkEnvironmentId != null) {
            globoNetworkLBNetworkVOList = _globoNetworkLBNetworkDao.listByEnvironmentRefId(globoNetworkEnvironment.getId());

        } else {
            globoNetworkLBNetworkVOList = _globoNetworkLBNetworkDao.listAll();
        }
        
        return globoNetworkLBNetworkVOList;
    }	

	@Override
	@DB
	public boolean removeGloboNetworkEnvironment(Long physicalNetworkId, Long globoNetworkEnvironmentId) {

        // Check if there are any networks in this GloboNetwork environment
        List<GloboNetworkNetworkVO> associationList = _globoNetworkNetworkDao.listByEnvironmentId(globoNetworkEnvironmentId);
        
        if (!associationList.isEmpty()) {
        	throw new InvalidParameterValueException("There are active networks on environment " + globoNetworkEnvironmentId + ". Please delete them before removing this environment.");
        }
        
		// Retrieve napiEnvironment from DB
		GloboNetworkEnvironmentVO globoNetworkEnvironment = _globoNetworkEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(physicalNetworkId, globoNetworkEnvironmentId);
		
		if (globoNetworkEnvironment == null) {
			// No physical network/environment pair registered in the database.
			throw new InvalidParameterValueException("Unable to find a relationship between physical network=" + physicalNetworkId + " and GloboNetwork environment=" + globoNetworkEnvironmentId);
		}
		        
        boolean result = _globoNetworkEnvironmentDao.remove(globoNetworkEnvironment.getId());

		return result;
	}
	
    @Override
    @DB
    public boolean removeGloboNetworkLBNetwork(Long physicalNetworkId, Long globoNetworkEnvironmentId, Long globoNetworkLBNetworkId) {
        
        // Retrieve napiEnvironment from DB
        GloboNetworkEnvironmentVO globoNetworkEnvironment = _globoNetworkEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(physicalNetworkId, globoNetworkEnvironmentId);
        
        if (globoNetworkEnvironment == null) {
            // No physical network/environment pair registered in the database.
            throw new InvalidParameterValueException("Unable to find a relationship between physical network=" + physicalNetworkId + " and GloboNetwork environment=" + globoNetworkEnvironmentId);
        }

        // Retrieve LB Network from DB
        final GloboNetworkLBNetworkVO globoNetworkLBNetworkVO = _globoNetworkLBNetworkDao.findByEnvironmentRefAndLBNetwork(globoNetworkEnvironment.getId(), globoNetworkLBNetworkId);
        if (globoNetworkLBNetworkVO == null) {
            throw new InvalidParameterValueException("Unable to find a relationship between environment " + globoNetworkEnvironmentId + " and LB network " + globoNetworkLBNetworkId);
        }
        
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                _configMgr.deleteVlanAndPublicIpRange(CallContext.current().getCallingUserId(), globoNetworkLBNetworkVO.getVlanId(), CallContext.current()
                        .getCallingAccount());
                
                _globoNetworkLBNetworkDao.remove(globoNetworkLBNetworkVO.getId());
            }
        });
        return true;
    }	
	
	@Override
	public void registerNicInGloboNetwork(NicProfile nic, VirtualMachineProfile vm, Network network) {
		
		String msg = "Unable to register nic " + nic + " from VM " + vm + ".";
		if (vm == null || nic == null) {
			throw new CloudRuntimeException(msg + " Invalid nic, virtual machine or network.");
		}
		
		GloboNetworkNetworkVO globoNetworkNetworkVO = _globoNetworkNetworkDao.findByNetworkId(network.getId());
		if (globoNetworkNetworkVO == null) {
			throw new CloudRuntimeException(msg + " Could not obtain mapping for network in GloboNetwork.");
		}
		
		Long equipmentGroup = GloboNetworkVmEquipmentGroup.value();
		if (equipmentGroup == null || "".equals(equipmentGroup)) {
			throw new CloudRuntimeException(msg + " Invalid equipment group for VM. Check your GloboNetwork global options.");
		}

		Long equipmentModel = null;
		switch(vm.getType()) {
			case DomainRouter:
				equipmentModel = GloboNetworkModelVmDomainRouter.value();
				break;
			case ConsoleProxy:
				equipmentModel = GloboNetworkModelVmConsoleProxy.value();
				break;
			case SecondaryStorageVm:
				equipmentModel = GloboNetworkModelVmSecondaryStorageVm.value();
				break;
			case ElasticIpVm:
				equipmentModel = GloboNetworkModelVmElasticIpVm.value();
				break;
			case ElasticLoadBalancerVm:
				equipmentModel = GloboNetworkModelVmElasticLoadBalancerVm.value();
				break;
			case InternalLoadBalancerVm:
				equipmentModel = GloboNetworkModelVmInternalLoadBalancerVm.value();
				break;
			case UserBareMetal:
				equipmentModel = GloboNetworkModelVmUserBareMetal.value();
				break;
			default:
				equipmentModel = GloboNetworkModelVmUser.value();
				break;
		}
		if (equipmentModel == null) {
			throw new CloudRuntimeException(msg + " Invalid equipment model for VM of type " + vm.getType() + ". Check your GloboNetwork global options.");
		}
		
		RegisterEquipmentAndIpInGloboNetworkCommand cmd = new RegisterEquipmentAndIpInGloboNetworkCommand();
		cmd.setNicIp(nic.getIp4Address());
		cmd.setNicDescription("");
		cmd.setVmName(getEquipNameFromUuid(vm.getUuid()));
		cmd.setVlanId(globoNetworkNetworkVO.getGloboNetworkVlanId());
		cmd.setEnvironmentId(globoNetworkNetworkVO.getGloboNetworkEnvironmentId());
		cmd.setEquipmentGroupId(Long.valueOf(equipmentGroup));
		cmd.setEquipmentModelId(Long.valueOf(equipmentModel));
		
		Answer answer = this.callCommand(cmd, vm.getVirtualMachine().getDataCenterId());
		if (answer == null || !answer.getResult()) {
			msg = answer == null ? msg : answer.getDetails();
			throw new CloudRuntimeException(msg);
		}
	}
	
	private String getEquipNameFromUuid(String uuid) {
		String instanceNamePrefix = _configDao.getValue(Config.InstanceName.key());
		String equipName = "";
		if (instanceNamePrefix == null || instanceNamePrefix.equals("")) {
			equipName = uuid;
		} else {
			equipName = instanceNamePrefix + "-" + uuid;
		}
		return equipName;
	}

	@Override
	public void unregisterNicInGloboNetwork(NicProfile nic, VirtualMachineProfile vm) {
		
		String msg = "Unable to unregister nic " + nic + " from VM " + vm + ".";
		if (vm == null || nic == null) {
			throw new CloudRuntimeException(msg + " Invalid nic or virtual machine.");
		}
		
		Long equipmentGroup = GloboNetworkVmEquipmentGroup.value();
		if (equipmentGroup == null) {
			throw new CloudRuntimeException(msg + " Invalid equipment group for VM. Check your GloboNetwork global options.");
		}
		
		UnregisterEquipmentAndIpInGloboNetworkCommand cmd = new UnregisterEquipmentAndIpInGloboNetworkCommand();
		cmd.setNicIp(nic.getIp4Address());
		cmd.setVmName(getEquipNameFromUuid(vm.getUuid()));

		GloboNetworkNetworkVO globoNetworkNetworkVO = _globoNetworkNetworkDao.findByNetworkId(nic.getNetworkId());
		if (globoNetworkNetworkVO != null) {
			cmd.setEnvironmentId(globoNetworkNetworkVO.getGloboNetworkEnvironmentId());
		}
		
		Answer answer = this.callCommand(cmd, vm.getVirtualMachine().getDataCenterId());
		if (answer == null || !answer.getResult()) {
			msg = answer == null ? msg : answer.getDetails();
			throw new CloudRuntimeException(msg);
		}
	}

	@Override
	public GloboNetworkVipAccVO addGloboNetworkVipToAcc(Long globoNetworkVipId, Long networkId) {

		Account caller = CallContext.current().getCallingAccount();
		Network network = null;
		if (networkId != null) {
			network = _ntwkDao.findById(networkId);
			if (network == null) {
				throw new InvalidParameterValueException(
						"Unable to find a network having the specified network id");
			}
		} else {
			throw new InvalidParameterValueException("Invalid networkId: " + networkId);
		}
        // Perform account permission check on network
        _accountMgr.checkAccess(caller, AccessType.UseNetwork, false, network);
		
        GetVipInfoFromGloboNetworkCommand cmd = new GetVipInfoFromGloboNetworkCommand(globoNetworkVipId);
		Answer answer = this.callCommand(cmd, network.getDataCenterId());
		String msg = "Could not validate VIP id with GloboNetwork";
		if (answer == null || !answer.getResult()) {
			msg = answer == null ? msg : answer.getDetails();
			throw new CloudRuntimeException(msg);
		}
		
		// TODO Remove accountId
		Long accountId = network.getAccountId();
		GloboNetworkVipAccVO globoNetworkVipAcc = _globoNetworkVipAccDao.findGloboNetworkVipAcc(globoNetworkVipId, accountId, networkId);
		if (globoNetworkVipAcc != null) {
			// Already exists, continue
			s_logger.info("Association between VIP " + globoNetworkVipId + " and network " + networkId + " already exists");
		} else {
			globoNetworkVipAcc = new GloboNetworkVipAccVO(globoNetworkVipId, accountId, networkId);
			_globoNetworkVipAccDao.persist(globoNetworkVipAcc);
		}

	    return globoNetworkVipAcc;
	}

	@Override
	public void associateNicToVip(Long vipId, Nic nic) {
		VMInstanceVO vm = _vmDao.findById(nic.getInstanceId());
		if (vm == null) {
			throw new CloudRuntimeException("There is no VM that belongs to nic " + nic);
		}
		
		GloboNetworkVipAccVO globoNetworkVipVO = _globoNetworkVipAccDao.findGloboNetworkVip(vipId, nic.getNetworkId());
		if (globoNetworkVipVO == null) {
			throw new InvalidParameterValueException("Vip " + vipId + " is not associated with Cloudstack");
		}
		
		Network network = _ntwkDao.findById(nic.getNetworkId());
		if (network == null) {
			throw new InvalidParameterValueException("Network " + nic.getNetworkId() + " doesn't exist in Cloudstack");
		}
		
		AddAndEnableRealInGloboNetworkCommand cmd = new AddAndEnableRealInGloboNetworkCommand();
		cmd.setEquipName(getEquipNameFromUuid(vm.getUuid()));
		cmd.setIp(nic.getIp4Address());
		cmd.setVipId(vipId);
		Answer answer = callCommand(cmd, network.getDataCenterId());
		if (answer == null || !answer.getResult()) {
			throw new CloudRuntimeException("Error associating nic " + nic +
					" to vip " + vipId + ": " + (answer == null ? null : answer.getDetails()));
		}
	}

	@Override
	public void disassociateNicFromVip(Long vipId, Nic nic) {
		DisableAndRemoveRealInGloboNetworkCommand cmd = new DisableAndRemoveRealInGloboNetworkCommand();
		VMInstanceVO vm = _vmDao.findById(nic.getInstanceId());
		if (vm == null) {
			throw new CloudRuntimeException("There is no VM that belongs to nic " + nic);
		}
		cmd.setEquipName(getEquipNameFromUuid(vm.getUuid()));
		cmd.setIp(nic.getIp4Address());
		cmd.setVipId(vipId);
		Network network = _ntwkDao.findById(nic.getNetworkId());
		Answer answer = callCommand(cmd, network.getDataCenterId());
		if (answer == null || !answer.getResult()) {
			throw new CloudRuntimeException("Error removing nic " + nic +
					" from vip " + vipId + ": " + (answer == null ? null : answer.getDetails()));
		}
	}

	@Override
	public List<GloboNetworkVipResponse> listGloboNetworkVips(Long projectId) {

		Account caller = CallContext.current().getCallingAccount();
		List<Long> permittedAccounts = new ArrayList<Long>();
		
        permittedAccounts.add(caller.getId());
        
		if (projectId != null) {
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by specified id");
            }
            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                // getProject() returns type ProjectVO.
                InvalidParameterValueException ex = new InvalidParameterValueException("Account " + caller + " cannot access specified project id");
                ex.addProxyObject(project.getUuid(), "projectId");
                throw ex;
            }
            
            permittedAccounts.add(project.getProjectAccountId());
		}
		
		// FIXME Improve this search by creating a custom criteria
		List<DataCenterVO> zones = _dcDao.listAllZones();
		List<NetworkVO> allowedNetworks = new ArrayList<NetworkVO>();
		for (DataCenterVO zone : zones) {
			for (Long accountId : permittedAccounts) {
				allowedNetworks.addAll(_ntwkDao.listNetworksByAccount(accountId, zone.getId(), Network.GuestType.Shared, false));
			}
		}
		
		List<Long> networkIds = new ArrayList<Long>();
		for (NetworkVO networkVO : allowedNetworks) {
			networkIds.add(networkVO.getId());
		}
		
		if (networkIds.isEmpty()) {
			return new ArrayList<GloboNetworkVipResponse>();
		}
		
		// Get all vip Ids related to networks
		List<GloboNetworkVipAccVO> globoNetworkVipAccList = _globoNetworkVipAccDao.listByNetworks(networkIds);
		
		Map<Long, GloboNetworkVipResponse> vips = new HashMap<Long, GloboNetworkVipResponse>();
		for (GloboNetworkVipAccVO globoNetworkVipAcc : globoNetworkVipAccList) {

			Network network = _ntwkDao.findById(globoNetworkVipAcc.getNetworkId());
			
			GloboNetworkVipResponse vip;
			
			if (vips.get(globoNetworkVipAcc.getGloboNetworkVipId()) == null) {
				
				// Vip is not in the returning map yet, get all info from GloboNetwork
				GetVipInfoFromGloboNetworkCommand cmd = new GetVipInfoFromGloboNetworkCommand(globoNetworkVipAcc.getGloboNetworkVipId());
				Answer answer = this.callCommand(cmd, network.getDataCenterId());
				String msg = "Could not list VIPs from GloboNetwork";
				if (answer == null || !answer.getResult()) {
					msg = answer == null ? msg : answer.getDetails();
					throw new CloudRuntimeException(msg);
				}
				vip =  ((GloboNetworkVipResponse) answer);
				
				vips.put(globoNetworkVipAcc.getGloboNetworkVipId(), vip);
				
			} else {
				// Vip is already in the returning map
				vip = vips.get(globoNetworkVipAcc.getGloboNetworkVipId());
			}
			
			if (vip.getNetworkIds() == null) {
				vip.setNetworkIds(new ArrayList<String>());
			}
			vip.getNetworkIds().add(network.getUuid());
			
		}
		return new ArrayList<GloboNetworkVipResponse>(vips.values());
	}

	@Override
	public String generateUrlForEditingVip(Long vipId, Network network) {
		
		GenerateUrlForEditingVipCommand cmd = new GenerateUrlForEditingVipCommand(vipId, GloboNetworkVIPServerUrl.value());
		Answer answer = callCommand(cmd, network.getDataCenterId());
		String msg = "Could not list VIPs from GloboNetwork";
		if (answer == null || !answer.getResult()) {
			msg = answer == null ? msg : answer.getDetails();
			throw new CloudRuntimeException(msg);
		}
		return answer.getDetails();
	}
	
	@Override
	public void removeGloboNetworkVip(Long napiVipId) {

		Account caller = CallContext.current().getCallingAccount();
		
		List<GloboNetworkVipAccVO> globoNetworkVipList = _globoNetworkVipAccDao.findByVipId(napiVipId);
		
		if (globoNetworkVipList == null || globoNetworkVipList.isEmpty()) {
			throw new InvalidParameterValueException(
					"Unable to find an association for VIP " + napiVipId);
		}
		
		Network network = null;
		for (GloboNetworkVipAccVO globoNetworkVipAccVO : globoNetworkVipList) {
			network = _ntwkDao.findById(globoNetworkVipAccVO.getNetworkId());
			if (network == null) {
				throw new InvalidParameterValueException(
						"Unable to find a network having the specified network id");
			}
			// Perform account permission check on network
	        _accountMgr.checkAccess(caller, AccessType.UseNetwork, false, network);
	     
	        _globoNetworkVipAccDao.remove(globoNetworkVipAccVO.getId());
	    
		}
		
		RemoveVipFromGloboNetworkCommand cmd = new RemoveVipFromGloboNetworkCommand();
		cmd.setVipId(napiVipId);
		
		Answer answer = this.callCommand(cmd, network.getDataCenterId());
		
		String msg = "Could not remove VIP " + napiVipId + " from GloboNetwork";
		if (answer == null || !answer.getResult()) {
			msg = answer == null ? msg : answer.getDetails();
			throw new CloudRuntimeException(msg);
		}
	}
	
	@Override
	public List<GloboNetworkVipResponse.Real> listGloboNetworkReals(Long vipId) {
		if (vipId == null) {
			throw new InvalidParameterValueException("Invalid VIP id");
		}
		
		List<GloboNetworkVipAccVO> globoNetworkVips = _globoNetworkVipAccDao.findByVipId(vipId);

		if (globoNetworkVips == null) {
			throw new CloudRuntimeException("Could not find VIP " + vipId);
		}
		
		List<GloboNetworkVipResponse.Real> reals = new ArrayList<GloboNetworkVipResponse.Real>();
		
		if (globoNetworkVips.isEmpty()) {
			return reals;
		}
		
		// We need a network to call commands, any network associated to this VIP will do
		Network network = _ntwkDao.findById(globoNetworkVips.get(0).getNetworkId());

		if (network == null) {
			throw new CloudRuntimeException("Could not find network with networkId " + globoNetworkVips.get(0).getNetworkId());
		}
		
		GetVipInfoFromGloboNetworkCommand cmd = new GetVipInfoFromGloboNetworkCommand(vipId);
		Answer answer = this.callCommand(cmd, network.getDataCenterId());
		String msg = "Could not find VIP from GloboNetwork";
		if (answer == null || !answer.getResult()) {
			msg = answer == null ? msg : answer.getDetails();
			throw new CloudRuntimeException(msg);
		}
		GloboNetworkVipResponse vip =  ((GloboNetworkVipResponse) answer);
		
		for (Real real : vip.getReals()) {
			for (GloboNetworkVipAccVO globoNetworkVipVO : globoNetworkVips) {
				network = _ntwkDao.findById(globoNetworkVipVO.getNetworkId());
				
				if(!NetUtils.isIpWithtInCidrRange(real.getIp(), network.getCidr())) {
					// If real's IP is not within network range, skip it
					continue;
				}
				
				Nic nic = _nicDao.findByIp4AddressAndNetworkId(real.getIp(), network.getId());
				if (nic != null) {
					real.setNic(String.valueOf(nic.getId()));
					real.setNetwork(network.getName());
				
					// User VM name rather than UUID
					VMInstanceVO userVM = _vmDao.findByUuid(real.getVmName());
					if (userVM != null) {
						real.setVmName(userVM.getHostName());
					}
				}
			}
			
			reals.add(real);
		}
			
		return reals;
	}

	@Override
	public String getConfigComponentName() {
		return GloboNetworkManager.class.getSimpleName();
	}

	@Override
	public ConfigKey<?>[] getConfigKeys() {
		return new ConfigKey<?>[] {GloboNetworkVIPServerUrl, 
				GloboNetworkConnectionTimeout, 
				GloboNetworkReadTimeout, 
				GloboNetworkNumberOfRetries, 
				GloboNetworkVmEquipmentGroup,
				GloboNetworkModelVmUser,
				GloboNetworkModelVmDomainRouter,
				GloboNetworkModelVmConsoleProxy,
				GloboNetworkModelVmSecondaryStorageVm,
				GloboNetworkModelVmElasticIpVm,
				GloboNetworkModelVmElasticLoadBalancerVm,
				GloboNetworkModelVmInternalLoadBalancerVm,
				GloboNetworkModelVmUserBareMetal,
				GloboNetworkDomainSuffix};
	}
	
	protected Vlan getPublicVlanFromZoneVlanNumberAndNetwork(Long zoneId, Integer vlanNumber, String networkCidr, String networkGateway) throws ResourceAllocationException, ConcurrentOperationException, InvalidParameterValueException, InsufficientCapacityException {
        // check if vlan already exists
        List<VlanVO> vlans = _vlanDao.listByZoneAndType(zoneId, VlanType.VirtualNetwork);
        for (VlanVO existedVlan : vlans) {
            Integer existedVlanNumber;
            if (existedVlan.getVlanTag() == null || Vlan.UNTAGGED.equals(existedVlan.getVlanTag())) {
                existedVlanNumber = null;
            } else {
                URI vlanUri = URI.create(existedVlan.getVlanTag());
                existedVlanNumber = Integer.valueOf(BroadcastDomainType.Vlan.getValueFrom(vlanUri));
            }
            if (ObjectUtils.equals(vlanNumber, existedVlanNumber)) {
                // check if network is the same
                String existedNetworkCidr = NetUtils.getCidrFromGatewayAndNetmask(existedVlan.getVlanGateway(), existedVlan.getVlanNetmask());
                supersetOrSubset status = NetUtils.isNetowrkASubsetOrSupersetOfNetworkB(existedNetworkCidr, networkCidr);
                if (status == supersetOrSubset.sameSubnet) {
                    // Vlan and network already exist
                    return existedVlan;
                } else {
                    throw new ResourceAllocationException("Public vlan with number " + vlanNumber + " and network " + networkCidr + " is " + status 
                            + " by vlan uuid " + existedVlan.getUuid(), ResourceType.public_ip);
                }
            }
        }
        return null;
	}
	
	protected Vlan createNewPublicVlan(Long zoneId, Integer vlanNumber, String networkCidr, String networkGateway) throws ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException, InvalidParameterValueException, InsufficientCapacityException {
        // Configure new vlan
	    // FIXME Maybe we need to fix this code to exclude owned vlans (check _vlanDao.listZoneWideNonDedicatedVlans(dcId);
        List<NetworkVO> networks = _ntwkDao.listByZoneAndTrafficType(zoneId, TrafficType.Public);
        // Can have more than one public network in the same zone (differ by physical network)
        if (networks.isEmpty()) {
            throw new ResourceUnavailableException("There are no Public network is this zone", DataCenter.class, zoneId);
        }
        NetworkVO network = networks.get(0);
        
        String vlanId = BroadcastDomainType.Vlan.toUri(vlanNumber).toString();
        String networkMask = NetUtils.getCidrNetmask(networkCidr);
        String networkAddress = NetUtils.getCidr(networkCidr).first();
        long size = NetUtils.getCidrSize(networkMask);
        String startIP = NetUtils.getIpRangeStartIpFromCidr(networkAddress, size);
        String endIP = NetUtils.getIpRangeEndIpFromCidr(networkAddress, size);

        // shift start ips by NUMBER_OF_RESERVED_IPS_FROM_START and NUMBER_OF_RESERVED_IPS_BEFORE_END
        startIP = NetUtils.long2Ip(NetUtils.ip2Long(startIP) + NUMBER_OF_RESERVED_IPS_FOR_LB_FROM_START);
        endIP = NetUtils.long2Ip(NetUtils.ip2Long(endIP) - NUMBER_OF_RESERVED_IPS_FOR_LB_BEFORE_END);
        
        List<PhysicalNetworkVO> listPhysicalNetworks = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, TrafficType.Public);
        if (listPhysicalNetworks.isEmpty()) {
            throw new ResourceUnavailableException("There are no Public Physical network is this zone", DataCenter.class, zoneId);
        }
        Long physicalNetworkId = listPhysicalNetworks.get(0).getId();
        
        Vlan vlan = _configMgr.createVlanAndPublicIpRange(zoneId, network.getId(), physicalNetworkId, true, null,
                startIP, endIP, networkGateway, networkMask, vlanId, null, null, null, null, null);
        return vlan;
	}
	
	public IpAddress allocate(final Network network, Account owner) throws ConcurrentOperationException, ResourceAllocationException, InsufficientAddressCapacityException {

	    long globoLBNetworkId = getGloboLBNetworkIdByVlanId(network, 1L);

        AcquireNewIpForLbCommand cmd = new AcquireNewIpForLbCommand(globoLBNetworkId);
        final GloboNetworkAndIPResponse globoNetwork =  (GloboNetworkAndIPResponse) this.callCommand(cmd, network.getDataCenterId());
        
        try {
            PublicIp publicIp = Transaction.execute(new TransactionCallbackWithException<PublicIp, CloudException>() {

                @Override
                public PublicIp doInTransaction(TransactionStatus status) throws CloudException {
                    Long zoneId = network.getDataCenterId();
                    Integer vlanNumber = globoNetwork.getVlanNum();
                    VlanVO vlan = getPublicVlanFromZoneVlanNumberAndNetwork(zoneId, vlanNumber, globoNetwork.getNetworkCidr(), globoNetwork.getNetworkGateway());
                    if (vlan == null) {
                        vlan = createNewPublicVlan(zoneId, vlanNumber, globoNetwork.getNetworkCidr(), globoNetwork.getNetworkGateway());
                    }
                    
                    String myIp = globoNetwork.getIp().addr();
                    return PublicIp.createFromAddrAndVlan(_ipAddrDao.findByIpAndVlanId(myIp, vlan.getId()), vlan);
                }
            });
            return publicIp;
            
        } catch (CloudException e) {
            // Exception when allocating new IP in Cloudstack. Roll back transaction in GloboNetwork
            s_logger.error("Reverting IP allocation in GloboNetwork due to error allocating IP", e);
            releaseLbIpFromGloboNetwork(network, globoNetwork.getIp().addr());
            throw new ResourceAllocationException(e.getLocalizedMessage(), ResourceType.public_ip);
        }
	}

    @Override
    public PublicIp acquireLbIp(Long networkId, Long projectId) throws ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException, InvalidParameterValueException, InsufficientCapacityException {

        // First of all, check user permission
        final Account caller = CallContext.current().getCallingAccount();

        final Network network;
        if (networkId != null) {
            network = _ntwkDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException(
                        "Unable to find a network having the specified network id");
            }
        } else {
            throw new InvalidParameterValueException("Invalid networkId: " + networkId);
        }
        // Perform account permission check on network
        _accountMgr.checkAccess(caller, AccessType.UseNetwork, false, network);
        
        // If project was set, this IP belongs to that project
        // Otherwise, the caller is the owner
        final Account owner;
        if (projectId != null) {
            Project project = _projectMgr.getProject(projectId);
            if (project != null) {
                owner = _accountMgr. getAccount(project.getProjectAccountId());
            } else {
                throw new InvalidParameterValueException("Could not find project with id " + projectId);
            }
        } else {
            owner = caller;
        }

        long lbEnvironmentId = getLoadBalancerEnvironmentId(network, "127.0.0.1");
        
        AcquireNewIpForLbCommand cmd = new AcquireNewIpForLbCommand(lbEnvironmentId);
        
        final GloboNetworkAndIPResponse globoNetwork =  (GloboNetworkAndIPResponse) this.callCommand(cmd, network.getDataCenterId());
        
        try {
            PublicIp publicIp = Transaction.execute(new TransactionCallbackWithException<PublicIp, CloudException>() {

                @Override
                public PublicIp doInTransaction(TransactionStatus status) throws CloudException {
                    Long zoneId = network.getDataCenterId();
                    Integer vlanNumber = globoNetwork.getVlanNum();
                    Vlan vlan = getPublicVlanFromZoneVlanNumberAndNetwork(zoneId, vlanNumber, globoNetwork.getNetworkCidr(), globoNetwork.getNetworkGateway());
                    if (vlan == null) {
                        vlan = createNewPublicVlan(zoneId, vlanNumber, globoNetwork.getNetworkCidr(), globoNetwork.getNetworkGateway());
                    }
                    
                    String myIp = globoNetwork.getIp().addr();
                    return _ipAddrMgr.assignPublicIpAddressFromVlans(zoneId, null, owner, VlanType.VirtualNetwork, Arrays.asList(vlan.getId()),
                            null, myIp, false);
                }
            });
            return publicIp;
            
        } catch (CloudException e) {
            // Exception when allocating new IP in Cloudstack. Roll back transaction in GloboNetwork
            s_logger.error("Reverting IP allocation in GloboNetwork due to error allocating IP", e);
            releaseLbIpFromGloboNetwork(network, globoNetwork.getIp().addr());
            throw new ResourceAllocationException(e.getLocalizedMessage(), ResourceType.public_ip);
        }
    }
    
    @Override
    public boolean releaseLbIpFromGloboNetwork(Network network, String ip) {
        if (network == null) {
            throw new InvalidParameterValueException("Invalid network");
        }
        
        Long lbEnvironmentId = getLoadBalancerEnvironmentId(network, ip);
        if (lbEnvironmentId == null) {
            throw new InvalidParameterValueException("There is no lb environment associated to network " + network.getId());
        }

        ReleaseIpFromGloboNetworkCommand cmdRelease = new ReleaseIpFromGloboNetworkCommand(ip, lbEnvironmentId);
        this.callCommand(cmdRelease, network.getDataCenterId());
        return true;
    }

    protected Long getGloboLBNetworkIdByVlanId(Network network, Long vlanId) {
        GloboNetworkNetworkVO glbNetworkVO = _globoNetworkNetworkDao.findByNetworkId(network.getId());
        if (glbNetworkVO == null) {
            throw new InvalidParameterValueException("There is no environment associated to network " + network.getId());
        }
        
        GloboNetworkEnvironmentVO networkEnvironmentVO = _globoNetworkEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(network.getPhysicalNetworkId(), glbNetworkVO.getGloboNetworkEnvironmentId());
        if (networkEnvironmentVO == null) {
            throw new InvalidParameterValueException("There is no association between physical network " + network.getPhysicalNetworkId() + " and GloboNetwork environment" + glbNetworkVO.getGloboNetworkEnvironmentId());
        }
        
        Long globoNetworkEnvironmentRefId = networkEnvironmentVO.getId();
        
        GloboNetworkLBNetworkVO globoNetworkLBNetworkVO = _globoNetworkLBNetworkDao.findByEnvironmentRefAndVlanId(globoNetworkEnvironmentRefId, vlanId);
        
        if (globoNetworkLBNetworkVO == null) {
            throw new InvalidParameterValueException("Could not find any Load Balancing environment for network " + network.getId());
        }
        
        return globoNetworkLBNetworkVO.getGloboNetworkLBNetworkId();
    }

    protected Long getLoadBalancerEnvironmentId(Network network, String lbIp) {
        GloboNetworkNetworkVO glbNetworkVO = _globoNetworkNetworkDao.findByNetworkId(network.getId());
        if (glbNetworkVO == null) {
            throw new InvalidParameterValueException("There is no environment associated to network " + network.getId());
        }
        
        IPAddressVO ipAddress = _ipAddrDao.findByIpAndDcId(network.getDataCenterId(), lbIp);
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Could not find IP " + lbIp + " in Cloudstack");
        }
        
        // Retrieve napiEnvironment from DB
        GloboNetworkEnvironmentVO globoNetworkEnvironment = _globoNetworkEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(network.getPhysicalNetworkId(), glbNetworkVO.getGloboNetworkEnvironmentId());
        
        if (globoNetworkEnvironment == null) {
            // No physical network/environment pair registered in the database.
            throw new InvalidParameterValueException("Unable to find a relationship between physical network=" + network.getPhysicalNetworkId() + " and GloboNetwork environment=" + glbNetworkVO.getGloboNetworkEnvironmentId());
        }

        GloboNetworkLBNetworkVO lbNetworkVO = _globoNetworkLBNetworkDao.findByEnvironmentRefAndVlanId(globoNetworkEnvironment.getId(), ipAddress.getVlanId());
        if (lbNetworkVO == null) {
            throw new InvalidParameterValueException("Could not find mapping between environment " + glbNetworkVO.getGloboNetworkEnvironmentId() + " and vlan " + ipAddress.getVlanId());
        }
        
        GetNetworkFromGloboNetworkCommand cmd = new GetNetworkFromGloboNetworkCommand(lbNetworkVO.getGloboNetworkLBNetworkId());
        Answer answer = callCommand(cmd, network.getDataCenterId(), false);
        GloboNetworkAndIPResponse response = (GloboNetworkAndIPResponse) answer;

        return response.getVipEnvironmentId();
    }

    @Override
    public boolean applyLbRuleInGloboNetwork(Network network, LoadBalancingRule rule) {
        // Validate params
        if (network == null || rule == null) {
            return false;
        }
        
        Account account = _accountMgr.getAccount(network.getAccountId());

        GloboNetworkNetworkVO globoNetworkNetworkVO = _globoNetworkNetworkDao.findByNetworkId(network.getId());
        if (globoNetworkNetworkVO == null) {
            throw new InvalidParameterValueException("Could not obtain mapping for network in GloboNetwork.");
        }
        
        // Stickness/Persistence
        if (rule.getStickinessPolicies() == null || rule.getStickinessPolicies().size() > 1) {
            throw new InvalidParameterValueException("Invalid stickness policy, list should contain only one");
        }

        // Healthcheck
        if (rule.getHealthCheckPolicies() == null || rule.getHealthCheckPolicies().size() > 1) {
            throw new InvalidParameterValueException("Invalid healthcheck policy, list should contain only one");
        }
        
        // Port mapping
        String port = rule.getSourcePortStart() + ":" + rule.getDefaultPortStart();
        List<String> ports = new ArrayList<String>();
        ports.add(port);

        // Reals
        List<GloboNetworkVipResponse.Real> realList = new ArrayList<GloboNetworkVipResponse.Real>();
        for (LbDestination destVM : rule.getDestinations()) {
            VMInstanceVO vm = _vmDao.findById(destVM.getInstanceId());
            if (vm != null) {
                GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
                real.setIp(destVM.getIpAddress());
                real.setVmName(getEquipNameFromUuid(vm.getUuid()));
                real.setPorts(Arrays.asList(String.valueOf(destVM.getDestinationPortStart())));
                real.setRevoked(destVM.isRevoked());
                realList.add(real);
            } else {
                throw new InvalidParameterValueException("Could not find VM with id " + destVM.getInstanceId());
            }
        }
        
        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        // VIP infos
        cmd.setHost(rule.getName());
        cmd.setIpv4(rule.getSourceIp().addr());
        cmd.setVipEnvironmentId(getLoadBalancerEnvironmentId(network, rule.getSourceIp().addr()));
        cmd.setPorts(ports);
        cmd.setBusinessArea(account.getAccountName());
        cmd.setServiceName(rule.getName());
        
        // Options and parameters
        cmd.setMethodBal(rule.getAlgorithm());
        cmd.setPersistencePolicy(rule.getStickinessPolicies().isEmpty() ? null : rule.getStickinessPolicies().get(0));
        cmd.setHealthcheckPolicy(rule.getHealthCheckPolicies().isEmpty() ? null : rule.getHealthCheckPolicies().get(0));
        cmd.setRuleState(rule.getState());
        
        // Reals infos
        cmd.setRealsEnvironmentId(globoNetworkNetworkVO.getGloboNetworkEnvironmentId());
        cmd.setRealList(realList);
        
        this.callCommand(cmd, network.getDataCenterId());
        return true;
    }

    @Override
    public boolean validateLBRule(Network network, LoadBalancingRule rule) {
        // Validate params
        if (network == null || rule == null) {
            return false;
        }
        
        GloboNetworkNetworkVO globoNetworkNetworkVO = _globoNetworkNetworkDao.findByNetworkId(network.getId());
        if (globoNetworkNetworkVO == null) {
            throw new InvalidParameterValueException("GloboNetwork Load Balancer work only with networks managed by GloboNetwork. NetworkId=" + network.getId());
        }
        
        // check additional networks
        if (rule.getAdditionalNetworks() != null) {
            for (Long networkId : rule.getAdditionalNetworks()) {
                GloboNetworkNetworkVO additionalGNNetworkVO = _globoNetworkNetworkDao.findByNetworkId(networkId);
                if (additionalGNNetworkVO == null) {
                    throw new InvalidParameterValueException("GloboNetwork Load Balancer work only with networks managed by GloboNetwork. NetworkId=" + network.getId());
                }
            }
        }
        
        // Stickness/Persistence
        if (rule.getStickinessPolicies() != null && rule.getStickinessPolicies().size() > 1) {
            throw new InvalidParameterValueException("Invalid stickness policy, list should contain only one");
        }

        // Healthcheck
        if (rule.getHealthCheckPolicies() != null && rule.getHealthCheckPolicies().size() > 1) {
            throw new InvalidParameterValueException("Invalid healthcheck policy, list should contain only one");
        }
        
        // Get VIP info
        GetVipInfoFromGloboNetworkCommand cmd = new GetVipInfoFromGloboNetworkCommand(rule.getSourceIp().addr(), getLoadBalancerEnvironmentId(network, rule.getSourceIp().addr()));
        Answer answer = this.callCommand(cmd, network.getDataCenterId(), false);
        if (answer != null && answer.getResult()) {
            GloboNetworkVipResponse globoNetworkVip = (GloboNetworkVipResponse) answer;
            // TODO Store ref between lb id and globonetwork vip id to solve this situation.
            String port = String.format("%d:%d", rule.getSourcePortStart(), rule.getDefaultPortStart());
            if (!port.equals(globoNetworkVip.getPorts().get(0))) {
                throw new InvalidParameterValueException("You can create only 1 lb rule per IP.");
            }
        }
        return true;
    }
}
