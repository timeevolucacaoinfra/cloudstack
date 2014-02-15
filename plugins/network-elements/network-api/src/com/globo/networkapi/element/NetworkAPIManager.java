package com.globo.networkapi.element;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Journal;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.networkapi.commands.AddNetworkApiVlanCmd;
import com.globo.networkapi.commands.AddNetworkViaNetworkapiCmd;
import com.globo.networkapi.commands.CreateNewVlanInNetworkAPICommand;
import com.globo.networkapi.commands.GetVlanInfoFromNetworkAPICommand;
import com.globo.networkapi.commands.ValidateNicInVlanCommand;
import com.globo.networkapi.resource.NetworkAPIResource;
import com.globo.networkapi.response.NetworkAPIVlanResponse;


public class NetworkAPIManager implements NetworkAPIService, PluggableService {
	
	private static final Logger s_logger = Logger
			.getLogger(NetworkAPIManager.class);
	
	static final int NUMBER_OF_RESERVED_IPS_FROM_START = 5;
	static final int NUMBER_OF_RESERVED_IPS_BEFORE_END = 5;

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
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    UserDao _userDao = null;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;

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
    NetworkManager _networkMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationServer _configServer;
    
	@Override
	public Network createNetwork(String name, String displayText, Long zoneId,
			Long networkOfferingId, Long physicalNetworkId,
			String networkDomain, ACLType aclType, String accountName,
			Long projectId, Long domainId, Boolean subdomainAccess,
			Boolean displayNetwork, Long aclId)
			throws ResourceAllocationException, ResourceUnavailableException,
			ConcurrentOperationException, InsufficientCapacityException {
		
		
		// FIXME Very important: Include permission checks before create network in networkapi

		Long napiEnvironmentId = getEnvironmentIdFromPod(null);
		Answer answer = createNewVlan(name, displayText,
				napiEnvironmentId);
		if (answer == null || !answer.getResult()) {
			String errorDescription = answer == null ? "no description" : answer.getDetails(); 
			throw new CloudRuntimeException("Error creating Vlan in NetworkAPI: " + errorDescription);
		}
		NetworkAPIVlanResponse response = (NetworkAPIVlanResponse) answer;
		Long napiVlanId = response.getVlanId();

		return createNetworkFromNetworkAPIVlan(napiVlanId, zoneId,
				networkOfferingId, physicalNetworkId, networkDomain,
				accountName, projectId, domainId, subdomainAccess,
				displayNetwork, aclId);
	}


	@Override
	public Network createNetworkFromNetworkAPIVlan(Long vlanId, Long zoneId,
			Long networkOfferingId, Long physicalNetworkId,
			String networkDomain, String accountName,
			Long projectId, Long domainId, Boolean subdomainAccess,
			Boolean displayNetwork, Long aclId)
			throws ResourceUnavailableException, 
			ResourceAllocationException, ConcurrentOperationException,
			InsufficientCapacityException {
    	
        Account caller = UserContext.current().getCaller();
        // NetworkAPI manage only shared network, so aclType is always domain
        ACLType aclType = ACLType.Domain;
        boolean isDomainSpecific = true;

        // Validate network offering
        NetworkOfferingVO ntwkOff = _networkOfferingDao.findById(networkOfferingId);
        if (ntwkOff == null || ntwkOff.isSystemOnly()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find network offering by specified id");
            if (ntwkOff != null) {
                ex.addProxyObject(ntwkOff.getUuid(), "networkOfferingId");
            }
            throw ex;
        } else if (GuestType.Shared != ntwkOff.getGuestType()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("NetworkAPI can handle only network offering with guest type shared");
            if (ntwkOff != null) {
                ex.addProxyObject(ntwkOff.getUuid(), "networkOfferingId");
            }
            throw ex;
        }
        // validate physical network and zone
        // Check if physical network exists
        PhysicalNetwork pNtwk = null;
        if (physicalNetworkId != null) {
            pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNtwk == null) {
                throw new InvalidParameterValueException("Unable to find a physical network having the specified physical network id");
            }
        }

        if (zoneId == null) {
            zoneId = pNtwk.getDataCenterId();
        }
        
        // Only Admin can create Shared networks (implies aclType=Domain)
        if (!_accountMgr.isAdmin(caller.getType())) {
            throw new PermissionDeniedException("Only admin can create networkapi shared networks");
        }

        if(displayNetwork != null){
            if(!_accountMgr.isRootAdmin(caller.getType())){
                throw new PermissionDeniedException("Only admin allowed to update displaynetwork parameter");
            }
        }else{
            displayNetwork = true;
        }

        DataCenter zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Specified zone id was not found");
        }

        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            // See DataCenterVO.java
            PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation since specified Zone is currently disabled");
            ex.addProxyObject(zone.getUuid(), "zoneId");
            throw ex;
        }

        if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by specified id");
            }
            _accountMgr.checkAccess(caller, domain);
        }

        Account owner = null;
        if ((accountName != null && domainId != null) || projectId != null) {
            owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);
        } else {
            owner = caller;
        }

        UserContext.current().setAccountId(owner.getAccountId());
        
        ///////// NetworkAPI specific code ///////

        // Get VlanInfo from NetworkAPI
		GetVlanInfoFromNetworkAPICommand cmd = new GetVlanInfoFromNetworkAPICommand();
		cmd.setVlanId(vlanId);
		
		ConcurrentMap<String, String> cfg = new ConcurrentHashMap<String, String>();
		
		NetworkAPIVlanResponse response;
		try {
			response = (NetworkAPIVlanResponse) callCommand(cmd, cfg);
		} catch (ConfigurationException e) {
			// FIXME
			throw new CloudRuntimeException(e);
		}
		
		if (response == null || !response.getResult()) {
			String msg = "Unable to execute command " + cmd.getClass().getSimpleName();
			s_logger.error(msg);
			// FIXME Understand this exception, and put more specific object
            throw new ResourceUnavailableException(msg, DataCenter.class, zoneId);
		}
		
		long networkAddresLong = response.getNetworkAddress().toLong();
		String networkAddress = NetUtils.long2Ip(networkAddresLong);
		String netmask = response.getMask().ip4();
		String cidr = NetUtils.ipAndNetMaskToCidr(networkAddress, netmask);
		
		String ranges[] = NetUtils.ipAndNetMaskToRange(networkAddress, netmask);
		String gateway = ranges[0];
		String startIP = NetUtils.long2Ip(NetUtils.ip2Long(ranges[0])+NUMBER_OF_RESERVED_IPS_FROM_START);
		String endIP = NetUtils.long2Ip(NetUtils.ip2Long(ranges[1])-NUMBER_OF_RESERVED_IPS_BEFORE_END);
		Long vlanNum = response.getVlanNum();
	     // NO IPv6 support yet
        String startIPv6 = null, endIPv6 = null, ip6Gateway = null, ip6Cidr = null;

		s_logger.info("Creating network with name " + response.getVlanName() +
				" (" + response.getVlanId() +
				"), network " + networkAddress +
				" gateway " + gateway + " startIp " + startIP + " endIp " + endIP + " cidr " + cidr
				);
        ///////// End of NetworkAPI specific code ///////

		Transaction txn = Transaction.currentTxn();
        txn.start();

        Long sharedDomainId = null;
        if (isDomainSpecific) {
            if (domainId != null) {
                sharedDomainId = domainId;
            } else {
                sharedDomainId = _domainMgr.getDomain(Domain.ROOT_DOMAIN).getId();
                subdomainAccess = true;
            }
        }

        if (_configMgr.isOfferingForVpc(ntwkOff)){
            throw new InvalidParameterValueException("Network offering can be used for VPC networks only");
        }
        if (ntwkOff.getInternalLb()) {
            throw new InvalidParameterValueException("Internal Lb can be enabled on vpc networks only");
        }

        owner = _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
		
		Network network = _networkMgr.createGuestNetwork(
        		networkOfferingId.longValue(),
        		response.getVlanName(),
        		response.getVlanDescription(),
        		gateway,
        		cidr,
        		String.valueOf(response.getVlanNum()),
        		networkDomain,
        		owner,
        		sharedDomainId,
        		pNtwk,
        		zoneId,
        		aclType,
        		subdomainAccess,
        		null, //vpcId,
        		ip6Gateway, //ip6Gateway,
        		ip6Cidr, //ip6Cidr,
        		displayNetwork, //displayNetwork,
        		null //isolatedPvlan
        		);

        
        if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            // Create vlan ip range
            _configMgr.createVlanAndPublicIpRange(pNtwk.getDataCenterId(), network.getId(), physicalNetworkId,
                    false, (Long) null, startIP, endIP, gateway, netmask, vlanNum.toString(), null, startIPv6,
                    endIPv6, ip6Gateway, ip6Cidr);
        }

        txn.commit();

        // if the network offering has persistent set to true, implement the network
        if ( ntwkOff.getIsPersistent() ) {
            try {
                if ( network.getState() == Network.State.Setup ) {
                    s_logger.debug("Network id=" + network.getId() + " is already provisioned");
                    return network;
                }
                DeployDestination dest = new DeployDestination(zone, null, null, null);
                UserVO callerUser = _userDao.findById(UserContext.current().getCallerUserId());
                Journal journal = new Journal.LogJournal("Implementing " + network, s_logger);
                ReservationContext context = new ReservationContextImpl(UUID.randomUUID().toString(), journal, callerUser, caller);
                s_logger.debug("Implementing network " + network + " as a part of network provision for persistent network");
                Pair<NetworkGuru, NetworkVO> implementedNetwork = _networkMgr.implementNetwork(network.getId(), dest, context);
                if (implementedNetwork.first() == null) {
                    s_logger.warn("Failed to provision the network " + network);
                }
                network = implementedNetwork.second();
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Failed to implement persistent guest network " + network + "due to ", ex);
                CloudRuntimeException e = new CloudRuntimeException("Failed to implement persistent guest network");
                e.addProxyObject(network.getUuid(), "networkId");
                throw e;
            }
        }
        return network;
    }
    
    protected NetworkAPIVlanResponse createNewVlan(String name, String description, Long networkAPIEnvironmentId) {
    	
    	CreateNewVlanInNetworkAPICommand cmd = new CreateNewVlanInNetworkAPICommand();
    	cmd.setVlanName(name);
    	cmd.setVlanDescription(description);
    	cmd.setNetworkAPIEnvironmentId(networkAPIEnvironmentId);

		Answer answer = null;
    	try {
    		ConcurrentMap<String, String> cfg = new ConcurrentHashMap<String, String>();
	    	answer = callCommand(cmd, cfg);
	    	if (answer == null || !answer.getResult()) {
	    		throw new CloudRuntimeException("Error creating VLAN in networkAPI");
	    	}
    	} catch (ConfigurationException e) {
    		throw new CloudRuntimeException("Error creating VLAN in networkAPI", e);
    	}
    	return (NetworkAPIVlanResponse) answer;
    }
    
    private Answer callCommand(Command cmd, ConcurrentMap<String, String> cfg) throws ConfigurationException {
//    	Long zoneId = Long.valueOf(cfg.get("zoneId"));
    	Long zoneId = 1L;

		cfg.put("name", "napivlan");
		cfg.put("zoneId", String.valueOf(1L));
		cfg.put("podId", String.valueOf(1L /*FIXME*/));
		cfg.put("clusterId", String.valueOf(1L /*FIXME*/));
		cfg.put("environmentId", "120");


		String username = _configServer.getConfigValue(Config.NetworkAPIUsername.key(), Config.ConfigurationParameterScope.global.name(), null);
		String password = _configServer.getConfigValue(Config.NetworkAPIPassword.key(), Config.ConfigurationParameterScope.global.name(), null);
		String url = _configServer.getConfigValue(Config.NetworkAPIUrl.key(), Config.ConfigurationParameterScope.global.name(), null);

        cfg.putIfAbsent("guid", "networkapi"); // FIXME
        cfg.putIfAbsent("url", url);
        cfg.putIfAbsent("username", username);
        cfg.putIfAbsent("password", password);

        NetworkAPIResource resource = new NetworkAPIResource();
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(cfg);
        resource.configure("networkapi", params);

        Host host = _resourceMgr.addHost(zoneId, resource, resource.getType(), cfg);
		
		Answer answer = _agentMgr.easySend(host.getId(), cmd);
		return answer;
    }
    
    @Override
    public Network validateNic(NicProfile nicProfile, VirtualMachineProfile<? extends VirtualMachine> vm, Network network, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
    	
    	Long networkAPIVlanId = null;
    	ValidateNicInVlanCommand cmd = new ValidateNicInVlanCommand();
    	cmd.setNicIp(nicProfile.getIp4Address());
//    	cmd.setVlanId(network.get);
    	cmd.setVlanNum(Long.valueOf(getVlanNum(nicProfile.getBroadCastUri())));

		ConcurrentMap<String, String> cfg = new ConcurrentHashMap<String, String>();
		
		cfg.put("name", "napivlan-" + networkAPIVlanId);
		cfg.put("zoneId", String.valueOf(dest.getDataCenter().getId()));
		cfg.put("podId", String.valueOf(1L /*FIXME*/));
		cfg.put("clusterId", String.valueOf(1L /*FIXME*/));
		cfg.put("environmentId", String.valueOf(this.getEnvironmentIdFromPod(null)));
		
		String msg = "Unable to validate nic " + nicProfile + " from VM " + vm;
		try {
	    	Answer answer = this.callCommand(cmd, cfg);
	    	if (answer == null || !answer.getResult()) {
	    		msg = answer == null ? msg : answer.getDetails();
	    		throw new CloudRuntimeException(msg);
	    	}
		} catch (ConfigurationException e) {
			throw new CloudRuntimeException(msg, e);
		}
		// everything is ok
    	return network;
    }

    /**
     * Get the number of vlan associate with {@code network}.
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

	protected Long getEnvironmentIdFromPod(Cluster cluster) {
		// FIXME Search for networkapiID from pod or cluster
		return 120L;
	}

	@Override
	public List<Class<?>> getCommands() {
		List<Class<?>> cmdList = new ArrayList<Class<?>>();
		cmdList.add(AddNetworkApiVlanCmd.class);
		cmdList.add(AddNetworkViaNetworkapiCmd.class);
		return cmdList;
	}

}
