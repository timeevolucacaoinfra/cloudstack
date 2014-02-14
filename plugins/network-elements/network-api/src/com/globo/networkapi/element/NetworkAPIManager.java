package com.globo.networkapi.element;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip4Address;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
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

    @Inject
    NetworkModel _networkManager;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    ResourceManager _resourceMgr;
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
    NetworkManager _networkMgr;
    @Inject
    ConfigurationServer _configServer;

	@Override
	public Network createNetwork(String name, String displayText, Long zoneId,
			Long networkOfferingId, Long physicalNetworkId,
			String networkDomain, ACLType aclType, String accountName,
			Long projectId, Long domainId, boolean subdomainAccess,
			boolean displayNetwork, Long aclId)
			throws ResourceAllocationException, ResourceUnavailableException,
			ConcurrentOperationException, InsufficientCapacityException {

		Long napiEnvironmentId = getEnvironmentIdFromPod(null);
		NetworkAPIVlanResponse response = createNewVlan(name, displayText,
				napiEnvironmentId);
		Long napiVlanId = response.getVlanId();

		return createNetworkFromNetworkAPIVlan(napiVlanId, zoneId,
				networkOfferingId, physicalNetworkId, networkDomain, aclType,
				accountName, projectId, domainId, subdomainAccess,
				displayNetwork, aclId);
	}


	@Override
	public Network createNetworkFromNetworkAPIVlan(Long vlanId, Long zoneId,
			Long networkOfferingId, Long physicalNetworkId,
			String networkDomain, ACLType aclType, String accountName,
			Long projectId, Long domainId, boolean subdomainAccess,
			boolean displayNetwork, Long aclId)
			throws ResourceUnavailableException, 
			ResourceAllocationException, ConcurrentOperationException,
			InsufficientCapacityException {
    	
        // Validate network offering
        NetworkOfferingVO ntwkOff = _networkOfferingDao.findById(networkOfferingId);
        if (ntwkOff == null || ntwkOff.isSystemOnly()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find network offering by specified id");
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
		
		Ip4Address networkAddress = response.getNetworkAddress();
		Ip4Address gateway = new Ip4Address(networkAddress.toLong()+1);
		String cidr = NetUtils.getCidrFromGatewayAndNetmask(gateway.ip4(), response.getMask().ip4());

		s_logger.info("Creating network with name " + response.getVlanName() +
				" (" + response.getVlanId() +
				"), network " + networkAddress.ip4() +
				" and gateway " + gateway.ip4()
				);
		
		Account owner = UserContext.current().getCaller();

        Network network = _networkMgr.createGuestNetwork(
        		networkOfferingId.longValue(),
        		response.getVlanName(),
        		response.getVlanDescription(),
        		gateway.ip4(),
        		cidr,
        		String.valueOf(response.getVlanNum()),
        		networkDomain,
        		owner,
        		Domain.ROOT_DOMAIN, //sharedDomainId ????,
        		pNtwk,
        		zoneId,
        		aclType,
        		subdomainAccess,
        		null, //vpcId,
        		null, //ip6Gateway,
        		null, //ip6Cidr,
        		displayNetwork, //displayNetwork,
        		null //isolatedPvlan
        		);

        
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
    	Long zoneId = Long.valueOf(cfg.get("zoneId"));

		cfg.put("name", "napivlan");
		cfg.put("zoneId", String.valueOf(zoneId));
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
