package com.globo.networkapi.resource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.xmlpull.v1.XmlPullParserException;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalLoadBalancerCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.net.Ip4Address;
import com.globo.networkapi.RequestProcessor;
import com.globo.networkapi.commands.AllocateVlanCommand;
import com.globo.networkapi.http.HttpXMLRequestProcessor;
import com.globo.networkapi.model.IPv4Network;
import com.globo.networkapi.model.Network;
import com.globo.networkapi.model.Vlan;
import com.globo.networkapi.response.NetworkAPIVlanResponse;

public class NetworkAPIResource extends ManagerBase implements ServerResource {
	private String _name;
	
	private String _username;
	
	private String _url;
	
	private String _password;
	
	private Long _environmentId;
	
	private String _zoneId;

	private String _podId;
	
	private String _clusterId;
	
	private RequestProcessor _napi;
	
	private static final Logger s_logger = Logger.getLogger(NetworkAPIResource.class);

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		
		_name = (String) params.get("name");
		if (_name == null) {
			throw new ConfigurationException("Unable to find name");
		}

		_url = (String) params.get("url");
		if (_url == null) {
			throw new ConfigurationException("Unable to find url");
		}
		
		_username = (String) params.get("username");
		if (_username == null) {
			throw new ConfigurationException("Unable to find username");
		}

		_password = (String) params.get("password");
		if (_password == null) {
			throw new ConfigurationException("Unable to find password");
		}

		_environmentId = Long.valueOf((String) params.get("environmentId"));
		if (_environmentId == null) {
			throw new ConfigurationException("Unable to find environmentId");
		}
		
		_zoneId = (String) params.get("zoneId");
		if (_zoneId == null) {
			throw new ConfigurationException("Unable to find zone Id in the configuration parameters");
		}
		
		_podId = (String) params.get("podId");
		if (_podId == null) {
			throw new ConfigurationException("Unable to find pod in the configuration parameters");
		}

		_clusterId = (String) params.get("clusterId");
		if (_clusterId == null) {
			throw new ConfigurationException("Unable to find cluster in the configuration parameters");
		}
		
		_napi = new HttpXMLRequestProcessor(_url, _username, _password);
		return false;
	}

	@Override
	public boolean start() {
		s_logger.trace("start");
		return true;
	}

	@Override
	public boolean stop() {
		s_logger.trace("stop");
		return true;
	}

	@Override
	public Type getType() {
		return Host.Type.L2Networking;
	}

	@Override
	public StartupCommand[] initialize() {
		s_logger.trace("initialize called");
		StartupCommand cmd = new StartupCommand(getType());
		cmd.setName(_name);
		cmd.setGuid("networkapi");
		cmd.setDataCenter(_zoneId);
		cmd.setPod(_podId);
		cmd.setCluster(_clusterId);
		cmd.setPrivateIpAddress("10.70.129.47"); // resolver ip da networkapi
		cmd.setStorageIpAddress("");
		cmd.setVersion(NetworkAPIResource.class.getPackage().getImplementationVersion());
		return new StartupCommand[] {cmd};
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		s_logger.trace("getCurrentStatus called with id " + id);
		return new PingCommand(getType(), id);
	}

	@Override
	public void disconnected() {
		s_logger.trace("disconnected");
	}

	@Override
	public IAgentControl getAgentControl() {
		s_logger.trace("getAgentControl");
		return null;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
		s_logger.trace("setAgentControl called with args " + agentControl);
	}

	@Override
	public Answer executeRequest(Command cmd) {
		s_logger.trace("executeRequest called with args " + cmd.getClass() + " - " + cmd);
		if (cmd instanceof AllocateVlanCommand) {
			return execute((AllocateVlanCommand) cmd);
		} else if (cmd instanceof ReadyCommand) {
			return new ReadyAnswer((ReadyCommand) cmd);
		} else if (cmd instanceof MaintainCommand) {
			return new MaintainAnswer((MaintainCommand) cmd);
		}
		s_logger.error("*** \n\nError executando command " + cmd.getClass());
		return Answer.createUnsupportedCommandAnswer(cmd);
	}
	
	public Answer execute(AllocateVlanCommand cmd) {
		try {
			List<Vlan> vlans = _napi.getVlanAPI().allocateWithoutNetwork(cmd.getEnvironmentId(), cmd.getVlanName(), cmd.getVlanDescription());
			if (vlans.isEmpty()) {
				return new Answer(cmd, false, "Vlan not created");
			}
			Vlan vlan = vlans.get(0);
			
			List<Network> new_networks = _napi.getNetworkAPI().addNetworkIpv4(vlan.getId(), Long.valueOf(6), null);
			if (new_networks.isEmpty()) {
				return new Answer(cmd, false, "Network not created");
			}
			Network network = new_networks.get(0);
			
			// Tosco, o objeto de criar rede nao esta retornando o ID rede
			vlan = _napi.getVlanAPI().getById(vlan.getId()).get(0);
			IPv4Network ipv4Network = vlan.getIpv4Networks().get(0);
			
			// create network in switches
			_napi.getNetworkAPI().createNetworks(ipv4Network.getId(), vlan.getId());

			Ip4Address mask = new Ip4Address(ipv4Network.getMaskOct1() + "." + ipv4Network.getMaskOct2() + "." + ipv4Network.getMaskOct3() + "." + ipv4Network.getMaskOct4());
			Ip4Address gateway = new Ip4Address(ipv4Network.getOct1() + "." + ipv4Network.getOct2() + "." + ipv4Network.getOct3() + "." + ipv4Network.getOct4());
			s_logger.info("Created network " + ipv4Network.getId() + " with gateway " + gateway + " and mask " + mask);
			return new NetworkAPIVlanResponse(cmd, vlan.getVlanNum(), vlan.getId(), gateway, mask);
		} catch (IOException e) {
			return new Answer(cmd, e);
		} catch (XmlPullParserException e) {
			return new Answer(cmd, e);
		}
		
	}
	
}
