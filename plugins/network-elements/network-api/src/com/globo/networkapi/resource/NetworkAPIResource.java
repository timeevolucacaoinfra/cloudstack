package com.globo.networkapi.resource;

import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;

public class NetworkAPIResource implements ServerResource {
	private String _name;
	
	private String _host;
	
	private String _username;
	
	private String _password;
	
	private static final Logger s_logger = Logger.getLogger(NetworkAPIResource.class);

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public void setName(String name) {
		s_logger.trace("setName called with args " + name);
//		_name = name;
	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
		s_logger.trace("setConfigParams called with args " + params);
	}

	@Override
	public Map<String, Object> getConfigParams() {
		s_logger.trace("getConfigParams called");
		return null;
	}

	@Override
	public int getRunLevel() {
		s_logger.trace("getRunLevel called");
		return 0;
	}

	@Override
	public void setRunLevel(int level) {
		s_logger.trace("setRunLevel called with args " + level);
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		
		_name = (String) params.get("name");
		if (_name == null) {
			throw new ConfigurationException("Unable to find name");
		}

		_username = (String) params.get("username");
		if (_name == null) {
			throw new ConfigurationException("Unable to find username");
		}

		_password = (String) params.get("password");
		if (_name == null) {
			throw new ConfigurationException("Unable to find password");
		}

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
		return new StartupCommand[] {};
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
//			return execute((ReadyCommand) cmd);
		} else if (cmd instanceof MaintainCommand) {
//			return execute((MaintainCommand) cmd);
		} else if (cmd instanceof IpAssocCommand) {
//			return execute((IpAssocCommand) cmd, numRetries);
		} else if (cmd instanceof LoadBalancerConfigCommand) {
//			return execute((LoadBalancerConfigCommand) cmd, numRetries);
		} else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
//			return execute((ExternalNetworkResourceUsageCommand) cmd);
		} else {
			return Answer.createUnsupportedCommandAnswer(cmd);
		}
		// FIXME
		return Answer.createUnsupportedCommandAnswer(cmd);
	}
	
}
