package com.globo.networkapi.element;

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
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
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
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.net.Ip4Address;
import com.cloud.utils.net.NetUtils;
import com.globo.networkapi.commands.AddNetworkApiVlanCmd;
import com.globo.networkapi.commands.GetVlanInfoFromNetworkAPICommand;
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
    ConfigurationDao _configDao;
    
    @Override
    public Network createNetworkFromNetworkAPIVlan(Long vlanId, Long zoneId, Long networkOfferingId, Long physicalNetworkId) throws ResourceUnavailableException, ConfigurationException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {
    	
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
		
		cfg.put("name", "napivlan-" + vlanId);
		cfg.put("zoneId", String.valueOf(zoneId));
		cfg.put("podId", String.valueOf(1L /*FIXME*/));
		cfg.put("clusterId", String.valueOf(1L /*FIXME*/));
		cfg.put("environmentId", "120");

		NetworkAPIVlanResponse response = (NetworkAPIVlanResponse) callCommand(cmd, cfg);
		
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
        		(String) null, //networkDomain,
        		owner,
        		1l, //sharedDomainId,
        		pNtwk,
        		zoneId,
        		ACLType.Domain,
        		true, //subdomainAccess,
        		null, //vpcId,
        		null, //ip6Gateway,
        		null, //ip6Cidr,
        		true, //displayNetwork,
        		null //isolatedPvlan
        		);

        
        return network;
    }
    
    private Answer callCommand(Command cmd, ConcurrentMap<String, String> cfg) throws ConfigurationException {
    	Long zoneId = Long.valueOf(cfg.get("zoneId"));

		String username = _configDao.getValueAndInitIfNotExist("networkapi.username", "Network", "");
		String password = _configDao.getValue("networkapi.password");
		String url = _configDao.getValue("networkapi.url");
		

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
    
    /**
     * Get the number of vlan associate with {@code network}.
     * @param network
     * @return
     */
    private Integer getVlanId(Network network) {
    	if (network == null || network.getBroadcastUri() == null) {
    		return null;
    	}
    	try {
    		Integer vlanNum = Integer.valueOf(network.getBroadcastUri().getHost());
        	return vlanNum;
    	} catch (NumberFormatException nfe) {
    		s_logger.error("Invalid Vlan number in network " + network.getId() + " " + network.getDisplayText());
    		return null;
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
		return cmdList;
	}

}
