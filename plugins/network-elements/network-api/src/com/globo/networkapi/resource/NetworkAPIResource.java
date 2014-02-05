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
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.globo.networkapi.RequestProcessor;
import com.globo.networkapi.commands.AllocateVlanCommand;
import com.globo.networkapi.http.HttpXMLRequestProcessor;
import com.globo.networkapi.model.Vlan;
import com.globo.networkapi.response.NetworkAPIVlanResponse;

public class NetworkAPIResource extends ManagerBase implements ServerResource {
	private String _name;
	
	private String _host;
	
	private String _username;
	
	private String _url;
	
	private String _zoneId;

	private String _password;
	
	private Long _environmentId;
	
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
		
		_zoneId = (String) params.get("zoneId");
		if (_zoneId == null) {
			throw new ConfigurationException("Unable to find zone Id in the configuration parameters");
		}

		_username = (String) params.get("username");
		if (_username == null) {
			throw new ConfigurationException("Unable to find username");
		}

		_password = (String) params.get("password");
		if (_password == null) {
			throw new ConfigurationException("Unable to find password");
		}

		_environmentId = (Long) params.get("environmentId");
		if (_environmentId == null) {
			throw new ConfigurationException("Unable to find environmentId");
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
		cmd.setGuid("networkapi");
		cmd.setDataCenter(_zoneId);
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
		} else if (cmd instanceof StartupCommand) {
			// null
		}
		return Answer.createUnsupportedCommandAnswer(cmd);
	}
	
	public Answer execute(AllocateVlanCommand cmd) {
		try {
			List<Vlan> vlans = _napi.getVlanAPI().allocateWithoutNetwork(cmd.getEnvironmentId(), cmd.getVlanName(), cmd.getVlanDescription());
			if (vlans.isEmpty()) {
				return new Answer(cmd, false, "Vlan not created");
			}
			Vlan vlan = vlans.get(0);
			return new NetworkAPIVlanResponse(cmd, vlan.getVlanNum(), vlan.getId());
		} catch (IOException e) {
			return new Answer(cmd, e);
		} catch (XmlPullParserException e) {
			return new Answer(cmd, e);
		}
		
	}
	
}
