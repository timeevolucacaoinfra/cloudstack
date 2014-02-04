package com.globo.networkapi.element;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.globo.networkapi.commands.AllocateVlanCommand;
import com.globo.networkapi.response.NetworkAPIVlanResponse;


public class NetworkAPIManager implements NetworkAPIService {
	
	private static final Logger s_logger = Logger
			.getLogger(NetworkAPIManager.class);

    @Inject
    NetworkModel _networkManager;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    NetworkAPIService _networkAPIService;
    @Inject
    AgentManager _agentMgr;
    @Inject
    ResourceManager _resourceMgr;

	@Override
	public Object allocateVlan(Network network, Cluster cluster) {
		
		AllocateVlanCommand cmd = new AllocateVlanCommand();
		cmd.setEnvironmentId(getEnvironmentIdFromPod(cluster));
		cmd.setVlanName("tst-" + network.getName());
		cmd.setVlanDescription(network.getDisplayText());
		Answer answer = _agentMgr.sendTo(cluster.getDataCenterId(), HypervisorType.Any, cmd);
		if (answer.getResult()) {
			NetworkAPIVlanResponse response = (NetworkAPIVlanResponse) answer;
			s_logger.info("Vlan allocated in " + response.getVlanNum());
		}
		
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
