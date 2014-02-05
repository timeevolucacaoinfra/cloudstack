package com.globo.networkapi.element;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.vm.VirtualMachineManager;
import com.globo.networkapi.commands.AllocateVlanCommand;
import com.globo.networkapi.resource.NetworkAPIResource;
import com.globo.networkapi.response.NetworkAPIVlanResponse;


public class NetworkAPIManager implements NetworkAPIService {
	
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

	@Override
	public Object allocateVlan(Network network, Cluster cluster) throws ResourceUnavailableException {

		PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(network.getPhysicalNetworkId());
		// FIXME NULL!!!
        long zoneId = pNetwork.getDataCenterId();
		
        NetworkAPIResource resource = new NetworkAPIResource();
        Map<String, String> cfg = new HashMap<String, String>();
        cfg.put("guid", "networkapi"); // FIXME
        cfg.put("name", network.getDisplayText());
        cfg.put("url", "https://networkapi.globoi.com");
        cfg.put("username", "gcloud");
        cfg.put("password", "gcloud");
        cfg.put("environmentId", "120");
        Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.L2Networking, cfg);
		
		AllocateVlanCommand cmd = new AllocateVlanCommand();
		cmd.setEnvironmentId(getEnvironmentIdFromPod(cluster));
		cmd.setVlanName("tst-" + network.getName());
		cmd.setVlanDescription(network.getDisplayText());
		Answer answer = _agentMgr.easySend(host.getId(), cmd);
		if (answer == null || !answer.getResult()) {
			String msg = "Unable do execute command";
			s_logger.error(msg);
            throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
		}
		NetworkAPIVlanResponse response = (NetworkAPIVlanResponse) answer;
		s_logger.info("Vlan allocated in " + response.getVlanNum());
		
		return null;
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

}
