package com.globo.dnsapi.resource;

import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

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
import com.globo.dnsapi.commands.SignInCommand;
import com.globo.dnsapi.exception.DNSAPIException;
import com.globo.dnsapi.http.HttpJsonRequestProcessor;
import com.globo.dnsapi.model.Authentication;

public class DNSAPIResource extends ManagerBase implements ServerResource {
	private String _zoneId;
	
	private String _guid;
	
	private String _name;
	
	private String _username;
	
	private String _url;
	
	private String _password;
	
	private String _token = null;
	
	protected HttpJsonRequestProcessor _dnsapi;
	
	private static final Logger s_logger = Logger.getLogger(DNSAPIResource.class);

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
	
		_zoneId = (String) params.get("zoneId");
		if (_zoneId == null) {
			throw new ConfigurationException("Unable to find zone");
		}
		
		_guid = (String) params.get("guid");
		if (_guid == null) {
			throw new ConfigurationException("Unable to find guid");
		}
		
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

		_dnsapi = new HttpJsonRequestProcessor(_url);

		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
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
		cmd.setGuid(_guid);
		cmd.setDataCenter(_zoneId);
		cmd.setPod("");
		cmd.setPrivateIpAddress("");
		cmd.setStorageIpAddress("");
		cmd.setVersion(DNSAPIResource.class.getPackage().getImplementationVersion());
		return new StartupCommand[] {cmd};
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		return new PingCommand(getType(), id);
	}

	@Override
	public void disconnected() {
		return;
	}

	@Override
	public IAgentControl getAgentControl() {
		return null;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
		return;
	}

	@Override
	public Answer executeRequest(Command cmd) {
		if (cmd instanceof ReadyCommand) {
			return new ReadyAnswer((ReadyCommand) cmd);
		} else if (cmd instanceof MaintainCommand) {
			return new MaintainAnswer((MaintainCommand) cmd);
		} else if (cmd instanceof SignInCommand) {
			return execute((SignInCommand) cmd);
		}
		return Answer.createUnsupportedCommandAnswer(cmd);
	}
	
	public Answer execute(SignInCommand cmd) {
		try {
			Authentication auth = _dnsapi.getAuthAPI().signIn(cmd.getEmail(), cmd.getPassword());
			if (auth != null) {
				_token = auth.getToken();
				return new Answer(cmd);
			} else {
				return new Answer(cmd, false, "Unable to sign in on DNS API");
			}
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
}
