package com.globo.dnsapi.resource;

import java.util.List;
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
import com.globo.dnsapi.commands.CreateDomainCommand;
import com.globo.dnsapi.commands.CreateRecordCommand;
import com.globo.dnsapi.commands.CreateReverseDomainCommand;
import com.globo.dnsapi.commands.GetDomainInfoCommand;
import com.globo.dnsapi.commands.GetRecordInfoCommand;
import com.globo.dnsapi.commands.GetReverseDomainInfoCommand;
import com.globo.dnsapi.commands.ListDomainCommand;
import com.globo.dnsapi.commands.ListRecordCommand;
import com.globo.dnsapi.commands.ListReverseDomainCommand;
import com.globo.dnsapi.commands.RemoveDomainCommand;
import com.globo.dnsapi.commands.RemoveRecordCommand;
import com.globo.dnsapi.commands.RemoveReverseDomainCommand;
import com.globo.dnsapi.commands.ScheduleExportCommand;
import com.globo.dnsapi.commands.SignInCommand;
import com.globo.dnsapi.exception.DNSAPIException;
import com.globo.dnsapi.http.HttpJsonRequestProcessor;
import com.globo.dnsapi.model.Authentication;
import com.globo.dnsapi.model.Domain;
import com.globo.dnsapi.model.Export;
import com.globo.dnsapi.model.Record;
import com.globo.dnsapi.response.DnsAPIDomainListResponse;
import com.globo.dnsapi.response.DnsAPIDomainResponse;
import com.globo.dnsapi.response.DnsAPIExportResponse;
import com.globo.dnsapi.response.DnsAPIRecordListResponse;
import com.globo.dnsapi.response.DnsAPIRecordResponse;

public class DnsAPIResource extends ManagerBase implements ServerResource {
	private String _zoneId;
	
	private String _guid;
	
	private String _name;
	
	private String _username;
	
	private String _url;
	
	private String _password;
	
	protected HttpJsonRequestProcessor _dnsapi;
	
	private static final Logger s_logger = Logger.getLogger(DnsAPIResource.class);

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
		cmd.setVersion(DnsAPIResource.class.getPackage().getImplementationVersion());
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
		} else if (cmd instanceof CreateDomainCommand) {
			return execute((CreateDomainCommand) cmd);
		} else if (cmd instanceof RemoveDomainCommand) {
			return execute((RemoveDomainCommand) cmd);
		} else if (cmd instanceof RemoveReverseDomainCommand) {
			return execute((RemoveReverseDomainCommand) cmd);
		} else if (cmd instanceof CreateReverseDomainCommand) {
			return execute((CreateReverseDomainCommand) cmd);
		} else if (cmd instanceof ListDomainCommand) {
			return execute((ListDomainCommand) cmd);
		} else if (cmd instanceof ListReverseDomainCommand) {
			return execute((ListReverseDomainCommand) cmd);
		} else if (cmd instanceof GetDomainInfoCommand) {
			return execute((GetDomainInfoCommand) cmd);
		} else if (cmd instanceof GetReverseDomainInfoCommand) {
			return execute((GetReverseDomainInfoCommand) cmd);
		} else if (cmd instanceof ListRecordCommand) {
			return execute((ListRecordCommand) cmd);
		} else if (cmd instanceof CreateRecordCommand) {
			return execute((CreateRecordCommand) cmd);
		} else if (cmd instanceof GetRecordInfoCommand) {
			return execute((GetRecordInfoCommand) cmd);
		} else if (cmd instanceof RemoveRecordCommand) {
			return execute((RemoveRecordCommand) cmd);
		} else if (cmd instanceof ScheduleExportCommand) {
			return execute((ScheduleExportCommand) cmd);
		}
		return Answer.createUnsupportedCommandAnswer(cmd);
	}
	
	public Answer execute(SignInCommand cmd) {
		try {
			Authentication auth = _dnsapi.getAuthAPI().signIn(cmd.getEmail(), cmd.getPassword());
			if (auth != null) {
				return new Answer(cmd, true, "Signed in successfully");
			} else {
				return new Answer(cmd, false, "Unable to sign in on DNS API");
			}
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(CreateDomainCommand cmd) {
		try {
			Domain domain = _dnsapi.getDomainAPI().createDomain(cmd.getName(), cmd.getTemplateId(), cmd.getAuthorityType());
			if (domain != null) {
				return new DnsAPIDomainResponse(cmd, domain);
			} else {
				return new Answer(cmd, false, "Unable to create domain in DNS API");
			}
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(RemoveDomainCommand cmd) {
		try {
			_dnsapi.getDomainAPI().removeDomain(cmd.getDomainId());
			return new Answer(cmd, true, "Domain removed");
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(RemoveReverseDomainCommand cmd) {
		try {
			_dnsapi.getDomainAPI().removeReverseDomain(cmd.getReverseDomainId());
			return new Answer(cmd, true, "Reverse domain removed");
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(CreateReverseDomainCommand cmd) {
		try {
			Domain reverseDomain = _dnsapi.getDomainAPI().createReverseDomain(cmd.getName(), cmd.getTemplateId(), cmd.getAuthorityType());
			if (reverseDomain != null) {
				return new DnsAPIDomainResponse(cmd, reverseDomain);
			} else {
				return new Answer(cmd, false, "Unable to create reverse domain in DNS API");
			}
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(ListDomainCommand cmd) {
		try {
			List<Domain> result;
			if (cmd.getQuery() == null) {
				result = _dnsapi.getDomainAPI().listAll();
			} else {
				result = _dnsapi.getDomainAPI().listByQuery(cmd.getQuery());
			}
			return new DnsAPIDomainListResponse(cmd, result);
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(ListReverseDomainCommand cmd) {
		try {
			List<Domain> result;
			if (cmd.getQuery() == null) {
				result = _dnsapi.getDomainAPI().listAllReverse();
			} else {
				result = _dnsapi.getDomainAPI().listReverseByQuery(cmd.getQuery());
			}
			return new DnsAPIDomainListResponse(cmd, result);
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(GetDomainInfoCommand cmd) {
		try {
			Domain domain = _dnsapi.getDomainAPI().getById(cmd.getDomainId());
			return new DnsAPIDomainResponse(cmd, domain);
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(GetReverseDomainInfoCommand cmd) {
		try {
			Domain domainReverse = _dnsapi.getDomainAPI().getReverseById(cmd.getReverseDomainId());
			return new DnsAPIDomainResponse(cmd, domainReverse);
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(ListRecordCommand cmd) {
		try {
			List<Record> result;
			if (cmd.getQuery() == null) {
				result = _dnsapi.getRecordAPI().listAll(cmd.getDomainId());
			} else {
				result = _dnsapi.getRecordAPI().listByQuery(cmd.getDomainId(), cmd.getQuery());
			}
			return new DnsAPIRecordListResponse(cmd, result);
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(CreateRecordCommand cmd) {
		try {
			Record record = _dnsapi.getRecordAPI().createRecord(cmd.getDomainId(), cmd.getName(), cmd.getContent(), cmd.getType());
			if (record != null) {
				return new DnsAPIRecordResponse(cmd, record);
			} else {
				return new Answer(cmd, false, "Unable to create record in DNS API");
			}
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(GetRecordInfoCommand cmd) {
		try {
			Record record = _dnsapi.getRecordAPI().getById(cmd.getRecordId());
			return new DnsAPIRecordResponse(cmd, record);
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(RemoveRecordCommand cmd) {
		try {
			_dnsapi.getRecordAPI().removeRecord(cmd.getRecordId());
			return new Answer(cmd, true, "Record removed");
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(ScheduleExportCommand cmd) {
		try {
			Export export = _dnsapi.getExportAPI().scheduleExport();
			if (export != null) {
				return new DnsAPIExportResponse(cmd, export);
			} else {
				return new Answer(cmd, false, "Unable to schedule export in DNS API");
			}
		} catch (DNSAPIException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
}
