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
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.net.Ip4Address;
import com.cloud.utils.net.NetUtils;
import com.globo.networkapi.NetworkAPIException;
import com.globo.networkapi.RequestProcessor;
import com.globo.networkapi.commands.ActivateNetworkCmd;
import com.globo.networkapi.commands.CreateNewVlanInNetworkAPICommand;
import com.globo.networkapi.commands.DeallocateVlanFromNetworkAPICommand;
import com.globo.networkapi.commands.GetVlanInfoFromNetworkAPICommand;
import com.globo.networkapi.commands.ListAllEnvironmentsFromNetworkAPICommand;
import com.globo.networkapi.commands.ValidateNicInVlanCommand;
import com.globo.networkapi.commands.RemoveNetworkInNetworkAPICommand;
import com.globo.networkapi.http.HttpXMLRequestProcessor;
import com.globo.networkapi.model.Environment;
import com.globo.networkapi.model.IPv4Network;
import com.globo.networkapi.model.Vlan;
import com.globo.networkapi.response.NetworkAPIAllEnvironmentResponse;
import com.globo.networkapi.response.NetworkAPIVlanResponse;

public class NetworkAPIResource extends ManagerBase implements ServerResource {
	private String _name;
	
	private String _username;
	
	private String _url;
	
	private String _password;
		
	protected RequestProcessor _napi;
	
	private static final Logger s_logger = Logger.getLogger(NetworkAPIResource.class);
	
	private static final long NETWORK_TYPE = 6; // Rede invalida de equipamentos

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
		/* FIXME */
		cmd.setDataCenter("1");
		cmd.setPod("1");
		cmd.setCluster("1");
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
		if (cmd instanceof ReadyCommand) {
			return new ReadyAnswer((ReadyCommand) cmd);
		} else if (cmd instanceof MaintainCommand) {
			return new MaintainAnswer((MaintainCommand) cmd);
		} else if (cmd instanceof GetVlanInfoFromNetworkAPICommand) {
			return execute((GetVlanInfoFromNetworkAPICommand) cmd);
		} else if (cmd instanceof ValidateNicInVlanCommand) {
			return execute((ValidateNicInVlanCommand) cmd);
		} else if (cmd instanceof CreateNewVlanInNetworkAPICommand) {
			return execute((CreateNewVlanInNetworkAPICommand) cmd);
		} else if (cmd instanceof ActivateNetworkCmd) {
			return execute((ActivateNetworkCmd) cmd);
		} else if (cmd instanceof ListAllEnvironmentsFromNetworkAPICommand) {
			return execute((ListAllEnvironmentsFromNetworkAPICommand) cmd);
		} else if (cmd instanceof RemoveNetworkInNetworkAPICommand) {
			return execute((RemoveNetworkInNetworkAPICommand) cmd);
		} else if (cmd instanceof DeallocateVlanFromNetworkAPICommand) {
			return execute((DeallocateVlanFromNetworkAPICommand) cmd);
		}
		return Answer.createUnsupportedCommandAnswer(cmd);
	}
	
	/**
	 * Validate if Nic ip and vlan number belongs to NetworkAPI VlanId
	 * @param cmd
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	public Answer execute(ValidateNicInVlanCommand cmd) {
		try {
			Vlan vlan = _napi.getVlanAPI().getById(cmd.getVlanId());
			List<IPv4Network> networks = vlan.getIpv4Networks();
			if (networks.isEmpty() || !networks.get(0).getActive()) {
				return new Answer(cmd, false, "No active networks found in VlanId " + cmd.getVlanId());
			}
			
			IPv4Network network = networks.get(0);
			String networkAddress = network.getOct1() + "." + network.getOct2() + "." + network.getOct3() + "." + network.getOct4();
			long ipLong = NetUtils.ip2Long(cmd.getNicIp());
			String netmask = network.getMaskOct1() + "." + network.getMaskOct2() + "." + network.getMaskOct3() + "." + network.getMaskOct4();
			long cidrSize = NetUtils.getCidrSize(netmask);
			String ipRange[] = NetUtils.getIpRangeFromCidr(networkAddress, cidrSize);
			if (!(ipLong > NetUtils.ip2Long(ipRange[0]) && ipLong < NetUtils.ip2Long(ipRange[1]))) {
				return new Answer(cmd, false, "Nic IP " + cmd.getNicIp() + " does not belong to network " + networkAddress + " in vlanId " + cmd.getVlanId());
			}
			return new Answer(cmd);
		} catch (NetworkAPIException e) {
			return new Answer(cmd, e);
		}
	}

	public Answer execute(GetVlanInfoFromNetworkAPICommand cmd) {
		try {
			Vlan vlan = _napi.getVlanAPI().getById(cmd.getVlanId());
			return createResponse(vlan, cmd);
		} catch (NetworkAPIException e) {
			return new Answer(cmd, e);
		}
	}

	public Answer execute(CreateNewVlanInNetworkAPICommand cmd) {
		try {
			Vlan vlan = _napi.getVlanAPI().allocateWithoutNetwork(cmd.getNetworkAPIEnvironmentId(), cmd.getVlanName(), cmd.getVlanDescription());
			
			/*Network network = */_napi.getNetworkAPI().addNetworkIpv4(vlan.getId(), Long.valueOf(NETWORK_TYPE), null);
			
			// Bug in networkapi: I need to have a second call to get networkid
			vlan = _napi.getVlanAPI().getById(vlan.getId());
			return createResponse(vlan, cmd);
		} catch (NetworkAPIException e) {
			return new Answer(cmd, e);
		}
	}
	
	public Answer execute(ActivateNetworkCmd cmd) {
		try {
			_napi.getNetworkAPI().createNetworks(cmd.getNetworkId(), cmd.getVlanId());
			return new Answer(cmd, true, "Network created");
		} catch (NetworkAPIException e) {
			return new Answer(cmd, e);
		}
	}
	
	public Answer execute(ListAllEnvironmentsFromNetworkAPICommand cmd) {
		try {
			List<Environment> environmentList = _napi.getEnvironmentAPI().listAll();
			
			return new NetworkAPIAllEnvironmentResponse(cmd, environmentList);
		} catch (NetworkAPIException e) {
			return new Answer(cmd, e);
		}
	}
	
	public Answer execute(RemoveNetworkInNetworkAPICommand cmd) {
		try {
			_napi.getVlanAPI().remove(cmd.getVlanId());
			
			return new Answer(cmd, true, "Network removed");
		} catch (NetworkAPIException e) {
			return new Answer(cmd, e);
		}
	}
	
	public Answer execute(DeallocateVlanFromNetworkAPICommand cmd) {
		try {
			_napi.getVlanAPI().deallocate(cmd.getVlanId());
			return new Answer(cmd, true, "Vlan deallocated");
		} catch (NetworkAPIException e) {
			return new Answer(cmd, e);
		}
	}
	
	private NetworkAPIVlanResponse createResponse(Vlan vlan, Command cmd) {
		IPv4Network ipv4Network = vlan.getIpv4Networks().get(0);
		
		String vlanName = vlan.getName();
		String vlanDescription = vlan.getDescription();
		Long vlanId = vlan.getId();
		Long vlanNum = vlan.getVlanNum();
		Ip4Address mask = new Ip4Address(ipv4Network.getMaskOct1() + "." + ipv4Network.getMaskOct2() + "." + ipv4Network.getMaskOct3() + "." + ipv4Network.getMaskOct4());
		Ip4Address networkAddress = new Ip4Address(ipv4Network.getOct1() + "." + ipv4Network.getOct2() + "." + ipv4Network.getOct3() + "." + ipv4Network.getOct4());
		return new NetworkAPIVlanResponse(cmd, vlanId, vlanName, vlanDescription, vlanNum, networkAddress, mask, ipv4Network.getId(), ipv4Network.getActive());
	}
	
}
