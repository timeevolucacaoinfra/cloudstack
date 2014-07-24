/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.globo.globodns.cloudstack.resource;

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
import com.globo.globodns.client.GloboDns;
import com.globo.globodns.client.GloboDnsException;
import com.globo.globodns.client.model.Authentication;
import com.globo.globodns.client.model.Domain;
import com.globo.globodns.client.model.Export;
import com.globo.globodns.client.model.Record;
import com.globo.globodns.cloudstack.commands.CreateDomainCommand;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateRecordAndReverseCommand;
import com.globo.globodns.cloudstack.commands.CreateRecordCommand;
import com.globo.globodns.cloudstack.commands.CreateReverseDomainCommand;
import com.globo.globodns.cloudstack.commands.GetDomainInfoCommand;
import com.globo.globodns.cloudstack.commands.GetRecordInfoCommand;
import com.globo.globodns.cloudstack.commands.GetReverseDomainInfoCommand;
import com.globo.globodns.cloudstack.commands.ListDomainCommand;
import com.globo.globodns.cloudstack.commands.ListRecordCommand;
import com.globo.globodns.cloudstack.commands.ListReverseDomainCommand;
import com.globo.globodns.cloudstack.commands.RemoveDomainCommand;
import com.globo.globodns.cloudstack.commands.RemoveRecordCommand;
import com.globo.globodns.cloudstack.commands.RemoveReverseDomainCommand;
import com.globo.globodns.cloudstack.commands.ScheduleExportCommand;
import com.globo.globodns.cloudstack.commands.SignInCommand;
import com.globo.globodns.cloudstack.commands.UpdateRecordCommand;
import com.globo.globodns.cloudstack.response.GloboDnsDomainListResponse;
import com.globo.globodns.cloudstack.response.GloboDnsDomainResponse;
import com.globo.globodns.cloudstack.response.GloboDnsExportResponse;
import com.globo.globodns.cloudstack.response.GloboDnsRecordListResponse;
import com.globo.globodns.cloudstack.response.GloboDnsRecordResponse;

public class GloboDnsResource extends ManagerBase implements ServerResource {
	private String _zoneId;
	
	private String _guid;
	
	private String _name;
	
	private String _username;
	
	private String _url;
	
	private String _password;
	
	protected GloboDns _globoDns;
	
	private static final String RECORD_TYPE = "A";
	private static final String REVERSE_RECORD_TYPE = "PTR";
	private static final String REVERSE_DOMAIN_SUFFIX = "in-addr.arpa";

	private static final Logger s_logger = Logger.getLogger(GloboDnsResource.class);

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

		_globoDns = GloboDns.buildHttpApi(_url, _username, _password);

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
		cmd.setVersion(GloboDnsResource.class.getPackage().getImplementationVersion());
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
		} else if (cmd instanceof UpdateRecordCommand) {
			return execute((UpdateRecordCommand) cmd);
		}
		return Answer.createUnsupportedCommandAnswer(cmd);
	}
	
	public Answer execute(SignInCommand cmd) {
		try {
			Authentication auth = _globoDns.getAuthAPI().signIn(cmd.getEmail(), cmd.getPassword());
			if (auth != null) {
				return new Answer(cmd, true, "Signed in successfully");
			} else {
				return new Answer(cmd, false, "Unable to sign in on GloboDNS");
			}
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(CreateDomainCommand cmd) {
		try {
			Domain domain = _globoDns.getDomainAPI().createDomain(cmd.getName(), cmd.getTemplateId(), cmd.getAuthorityType());
			if (domain != null) {
				return new GloboDnsDomainResponse(cmd, domain);
			} else {
				return new Answer(cmd, false, "Unable to create domain in GloboDNS");
			}
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(RemoveDomainCommand cmd) {
		try {
			_globoDns.getDomainAPI().removeDomain(cmd.getDomainId());
			return new Answer(cmd, true, "Domain removed");
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(RemoveReverseDomainCommand cmd) {
		try {
			_globoDns.getDomainAPI().removeReverseDomain(cmd.getReverseDomainId());
			return new Answer(cmd, true, "Reverse domain removed");
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(CreateReverseDomainCommand cmd) {
		try {
			Domain reverseDomain = _globoDns.getDomainAPI().createReverseDomain(cmd.getName(), cmd.getTemplateId(), cmd.getAuthorityType());
			if (reverseDomain != null) {
				return new GloboDnsDomainResponse(cmd, reverseDomain);
			} else {
				return new Answer(cmd, false, "Unable to create reverse domain in GloboDNS");
			}
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(ListDomainCommand cmd) {
		try {
			List<Domain> result;
			if (cmd.getQuery() == null) {
				result = _globoDns.getDomainAPI().listAll();
			} else {
				result = _globoDns.getDomainAPI().listByQuery(cmd.getQuery());
			}
			return new GloboDnsDomainListResponse(cmd, result);
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(ListReverseDomainCommand cmd) {
		try {
			List<Domain> result;
			if (cmd.getQuery() == null) {
				result = _globoDns.getDomainAPI().listAllReverse();
			} else {
				result = _globoDns.getDomainAPI().listReverseByQuery(cmd.getQuery());
			}
			return new GloboDnsDomainListResponse(cmd, result);
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(GetDomainInfoCommand cmd) {
		try {
			Domain domain = _globoDns.getDomainAPI().getById(cmd.getDomainId());
			return new GloboDnsDomainResponse(cmd, domain);
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(GetReverseDomainInfoCommand cmd) {
		try {
			Domain domainReverse = _globoDns.getDomainAPI().getReverseById(cmd.getReverseDomainId());
			return new GloboDnsDomainResponse(cmd, domainReverse);
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(ListRecordCommand cmd) {
		try {
			List<Record> result;
			if (cmd.getQuery() == null) {
				result = _globoDns.getRecordAPI().listAll(cmd.getDomainId());
			} else {
				result = _globoDns.getRecordAPI().listByQuery(cmd.getDomainId(), cmd.getQuery());
			}
			return new GloboDnsRecordListResponse(cmd, result);
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(CreateRecordCommand cmd) {
		try {
			Record record = _globoDns.getRecordAPI().createRecord(cmd.getDomainId(), cmd.getName(), cmd.getContent(), cmd.getType());
			if (record != null) {
				return new GloboDnsRecordResponse(cmd, record);
			} else {
				return new Answer(cmd, false, "Unable to create record in GloboDNS");
			}
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(UpdateRecordCommand cmd) {
		try {
			_globoDns.getRecordAPI().updateRecord(cmd.getRecordId(), cmd.getDomainId(), cmd.getName(), cmd.getContent());
			Record record = _globoDns.getRecordAPI().getById(cmd.getRecordId());
			if (record != null) {
				return new GloboDnsRecordResponse(cmd, record);
			} else {
				return new Answer(cmd, false, "Unable to update record in GloboDNS");
			}
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(GetRecordInfoCommand cmd) {
		try {
			Record record = _globoDns.getRecordAPI().getById(cmd.getRecordId());
			return new GloboDnsRecordResponse(cmd, record);
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(RemoveRecordCommand cmd) {
		try {
			_globoDns.getRecordAPI().removeRecord(cmd.getRecordId());
			return new Answer(cmd, true, "Record removed");
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	public Answer execute(ScheduleExportCommand cmd) {
		try {
			Export export = _globoDns.getExportAPI().scheduleExport();
			if (export != null) {
				return new GloboDnsExportResponse(cmd, export);
			} else {
				return new Answer(cmd, false, "Unable to schedule export in GloboDNS");
			}
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	private Domain searchDomain(String name) {
		if (name == null) {
			return null;
		}
		List<Domain> candidates = _globoDns.getDomainAPI().listByQuery(name);
		for (Domain candidate: candidates) {
			if (name.equals(candidate.getName())) {
				return candidate;
			}
		}
		return null;
		
		
	}
	
	private Record searchRecord(String recordName, Long domainId) {
		if (recordName == null || domainId == null) {
			return null;
		}
		List<Record> candidates = _globoDns.getRecordAPI().listByQuery(domainId, recordName);
		// GloboDns search name in name and content. I need to iterate to check if recordName exists only in name
		for (Record candidate: candidates) {
			if (recordName.equalsIgnoreCase(candidate.getContent())) {
				s_logger.debug("Record " + recordName + " in domain id " + domainId + " found in GloboDNS");
				return candidate;
			}
		}
		s_logger.debug("Record " + recordName + " in domain id " + domainId + " not found in GloboDNS");
		return null;
	}
	
	private boolean createOrUpdateRecord(Long domainId, String name, String ip, String type) {
		Record record = this.searchRecord(name, domainId);
		if (record == null) {
			// Create new record
			record = _globoDns.getRecordAPI().createRecord(domainId, name, ip, type);
			s_logger.info("Created record " + name + " in domain " + domainId);
		} else {
			if (!ip.equals(record.getContent())) {
				// ip is incorrect. Fix.
				_globoDns.getRecordAPI().updateRecord(record.getId(), domainId, name, ip);
			}
		}
		return record != null;
	}

	public Answer execute(CreateOrUpdateRecordAndReverseCommand cmd) {
		try {
			Domain domain = searchDomain(cmd.getDomain());
			if (domain == null) {
				return new Answer(cmd, false, "Invalid domain");
			}
			
			boolean created = createOrUpdateRecord(domain.getId(), cmd.getName(), cmd.getIp(), RECORD_TYPE);
			if (!created) {
				return new Answer(cmd, false, "Unable to create record " + cmd.getName() + " at " + cmd.getDomain());
			}
			
			// create reverse
	    	String[] octets = cmd.getIp().split("\\.");
			String reverseRecordName = octets[3];
			String reverseRecordContent = cmd.getName() + '.' + domain.getName();
	    	String reverseDomainName = octets[2] + '.' + octets[1] + '.' + octets[0] + '.' + REVERSE_DOMAIN_SUFFIX;
	    	
	    	Domain reverseDomain = searchDomain(reverseDomainName);
			if (reverseDomain == null) {
				return new Answer(cmd, false, "Invalid reverse domain");
			}

			created = createOrUpdateRecord(reverseDomain.getId(), reverseRecordName, reverseRecordContent, REVERSE_RECORD_TYPE);
			if (!created) {
				return new Answer(cmd, false, "Unable to create reverse record to ip " + cmd.getIp());
			}
			return new Answer(cmd);
		} catch (GloboDnsException e) {
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
}
