// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.globo.dnsapi.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.NetworkElement;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.server.ConfigurationServer;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.dnsapi.DnsAPINetworkVO;
import com.globo.dnsapi.DnsAPIVirtualMachineVO;
import com.globo.dnsapi.api.AddDnsApiHostCmd;
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
import com.globo.dnsapi.commands.UpdateRecordCommand;
import com.globo.dnsapi.dao.DnsAPINetworkDao;
import com.globo.dnsapi.dao.DnsAPIVirtualMachineDao;
import com.globo.dnsapi.model.Domain;
import com.globo.dnsapi.model.Export;
import com.globo.dnsapi.model.Record;
import com.globo.dnsapi.resource.DnsAPIResource;
import com.globo.dnsapi.response.DnsAPIDomainListResponse;
import com.globo.dnsapi.response.DnsAPIDomainResponse;
import com.globo.dnsapi.response.DnsAPIExportResponse;
import com.globo.dnsapi.response.DnsAPIRecordListResponse;
import com.globo.dnsapi.response.DnsAPIRecordResponse;
import com.globo.networkapi.NetworkAPINetworkVO;
import com.globo.networkapi.dao.NetworkAPINetworkDao;

@Component
@Local(NetworkElement.class)
public class DnsAPIElement extends AdapterBase implements ResourceStateAdapter, NetworkElement, DnsAPIElementService {

	private static final Logger s_logger = Logger.getLogger(DnsAPIElement.class);
	
	private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();
	
	private static final String AUTHORITY_TYPE = "M";
	
	private static final String RECORD_TYPE = "A";
	private static final String REVERSE_RECORD_TYPE = "PTR";
	
	// DAOs
	@Inject
	DataCenterDao _dcDao;
	@Inject
	DnsAPINetworkDao _dnsapiNetworkDao;
	@Inject
	DnsAPIVirtualMachineDao _dnsapiVmDao;
	@Inject
	HostDao _hostDao;
	@Inject
	NetworkAPINetworkDao _napiNetworkDao;
	@Inject
	PhysicalNetworkDao _physicalNetworkDao;
	
	// Managers
	@Inject
	AgentManager _agentMgr;
	@Inject
	ResourceManager _resourceMgr;
	@Inject
	ConfigurationServer _configServer;
	
    public DnsAPIElement() {

    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }
    
    private static Map<Service, Map<Capability, String>> setCapabilities() {
    	Map<Service, Map<Capability, String>> caps = new HashMap<Service, Map<Capability, String>>();
        caps.put(Service.Dns, new HashMap<Capability, String>());
        return caps;
    }

    @Override
    public Provider getProvider() {
        return Provider.DnsAPI;
    }

    @Override
    @DB
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
    InsufficientCapacityException {
    	s_logger.debug("Entering implement method for DnsAPI");
    	Long zoneId = network.getDataCenterId();
    	DataCenter zone = _dcDao.findById(zoneId);
		if (zone == null) {
			throw new CloudRuntimeException(
					"Could not find zone associated to this network");
		}

		NetworkAPINetworkVO napiNetworkVO = _napiNetworkDao.findByNetworkId(network.getId());
		if (napiNetworkVO == null) {
			throw new CloudRuntimeException("Could not find VLAN associated to this network");
		}
		
		String domainSuffix = _configServer.getConfigValue(Config.DNSAPIDomainSuffix.key(),
				Config.ConfigurationParameterScope.global.name(), null);
		String reverseDomainSuffix = _configServer.getConfigValue(Config.DNSAPIReverseDomainSuffix.key(),
				Config.ConfigurationParameterScope.global.name(), null);		
		
		this.signIn(zoneId, null, null);
		
		/* Create new domain in DNS API */
		// domainName is of form zoneName-vlanId.domainSuffix
    	String domainName = zone.getName() + "-" + napiNetworkVO.getNapiVlanId() + "." + domainSuffix;
    	domainName = domainName.toLowerCase();
    	s_logger.debug("Creating domain " + domainName);
    	Domain createdDomain = this.getOrCreateDomain(zoneId, domainName, false);
    	
    	/* Create new reverse domain in DNS API */
    	String[] octets = network.getCidr().split("\\/")[0].split("\\.");
    	String reverseDomainName = octets[2] + "." + octets[1] + "." + octets[0] + "." + reverseDomainSuffix;
    	s_logger.debug("Creating reverse domain " + reverseDomainName);
    	Domain reverseDomain = this.getOrCreateDomain(zoneId, reverseDomainName, true);
    	
    	/* Export changes to Bind in DNS API */
    	this.scheduleBindExport(zoneId);

    	/* Save in the database */
    	this.saveDomainDB(network, createdDomain, reverseDomain);
    	
        return true;
    }

    @Override
    @DB
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
    ResourceUnavailableException, InsufficientCapacityException {
    	s_logger.debug("Entering prepare method for DnsAPI");
    	
    	if (vm.getType() != VirtualMachine.Type.User) {
    		// We create DNS API mapping only for User VMs
    		return true;
    	}
    	
    	Long zoneId = network.getDataCenterId();
    	DataCenter zone = _dcDao.findById(zoneId);
		if (zone == null) {
			throw new CloudRuntimeException(
					"Could not find zone associated to this network");
		}
		
		DnsAPINetworkVO dnsapiNetworkVO = getDnsAPINetworkVO(network);
		if (dnsapiNetworkVO == null) {
			throw new CloudRuntimeException("Could not obtain DNS mapping");
		}

		long domainId = dnsapiNetworkVO.getDnsapiDomainId();
		long reverseDomainId = dnsapiNetworkVO.getDnsapiReverseDomainId();
		
		this.signIn(zoneId, null, null);
		
		/* Create new A record in DNS API */
		String recordName = (vm.getHostName() != null ? vm.getHostName() : vm.getUuid());
    	Record createdRecord = this.createOrUpdateRecord(zoneId, domainId, recordName, nic.getIp4Address(), false);
    	
		/* Create new PTR record in DNS API */
		// Need domain name for full reverse record content
    	GetDomainInfoCommand cmdInfo = new GetDomainInfoCommand(domainId);
    	Answer answerInfo = callCommand(cmdInfo, zoneId);
    	Domain domain = ((DnsAPIDomainResponse) answerInfo).getDomain();
    	if (domain == null) {
    		// Domain doesn't exist in DNS API
    		throw new CloudRuntimeException("Could not get Domain info from DNS API");
    	}
    	
    	String[] octets = nic.getIp4Address().split("\\.");
		String reverseRecordName = octets[3];    	
		String reverseRecordContent = vm.getHostName() + "." + domain.getName();
    	
		Record createdReverseRecord = this.createOrUpdateRecord(zoneId, reverseDomainId, reverseRecordName, reverseRecordContent, true);
		
    	/* Export changes to Bind in DNS API */
    	this.scheduleBindExport(zoneId);
    	
    	/* Save in the database */
    	this.saveRecordDB(vm, createdRecord, createdReverseRecord);

    	return true;
    }

    @Override
    @DB
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
    	s_logger.debug("Entering release method for DnsAPI");
    	
    	if (vm.getType() != VirtualMachine.Type.User) {
    		// We handle only User VMs
    		return true;
    	}
    	
    	Long zoneId = network.getDataCenterId();
    	DataCenter zone = _dcDao.findById(zoneId);
		if (zone == null) {
			throw new CloudRuntimeException(
					"Could not find zone associated to this network");
		}

		Transaction txn = Transaction.currentTxn();
		txn.start();
		
		DnsAPIVirtualMachineVO dnsapiVMVO = this.getDnsAPIVMVO(vm);
		if (dnsapiVMVO == null) {
			throw new CloudRuntimeException("Could not obtain DNS mapping for this VM");
		}

		long recordId = dnsapiVMVO.getDnsapiRecordId();
		long reverseRecordId = dnsapiVMVO.getDnsapiReverseRecordId();
		
		this.signIn(zoneId, null, null);
		
		/* Remove record from DNS API */
		this.removeRecord(zoneId, recordId);
    	
    	/* Remove reverse record from DNS API */
		this.removeRecord(zoneId, reverseRecordId);
		
    	/* Export changes to Bind in DNS API */
    	this.scheduleBindExport(zoneId);
    	
    	/* Remove entry from mapping table */
    	_dnsapiVmDao.remove(dnsapiVMVO.getId());
    	
    	txn.commit();
    	
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
    	s_logger.debug("Entering shutdown method for DnsAPI");
        return true;
    }

    @Override
    @DB
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
    	s_logger.debug("Entering destroy method for DnsAPI");
    	Long zoneId = network.getDataCenterId();
    	DataCenter zone = _dcDao.findById(zoneId);
		if (zone == null) {
			throw new CloudRuntimeException(
					"Could not find zone associated to this network");
		}

		Transaction txn = Transaction.currentTxn();
		txn.start();
		
		DnsAPINetworkVO dnsapiNetworkVO = getDnsAPINetworkVO(network);
		if (dnsapiNetworkVO == null) {
			throw new CloudRuntimeException("Could not obtain DNS mapping");
		}

		long domainId = dnsapiNetworkVO.getDnsapiDomainId();
		long reverseDomainId = dnsapiNetworkVO.getDnsapiReverseDomainId();
		    	
		this.signIn(zoneId, null, null);
		
		/* Remove domain from DNS API */
		this.removeDomain(zoneId, domainId, false);
    	
    	/* Remove reverse domain from DNS API */
		this.removeDomain(zoneId, reverseDomainId, true);
		
    	/* Export changes to Bind in DNS API */
    	this.scheduleBindExport(zoneId);
    	
    	/* Remove entry from mapping table */
    	_dnsapiNetworkDao.remove(dnsapiNetworkVO.getId());
    	
    	txn.commit();
    	
        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

	@Override
	public List<Class<?>> getCommands() {
		List<Class<?>> cmdList = new ArrayList<Class<?>>();
		cmdList.add(AddDnsApiHostCmd.class);
		return cmdList;
	}
	
	@Override
	public HostVO createHostVOForConnectedAgent(HostVO host,
			StartupCommand[] cmd) {
		return null;
	}

	@Override
	public HostVO createHostVOForDirectConnectAgent(HostVO host,
			StartupCommand[] startup, ServerResource resource,
			Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupCommand)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;	}

	@Override
	public DeleteHostAnswer deleteHost(HostVO host, boolean isForced,
			boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (!(host.getType() == Host.Type.L2Networking)) {
            return null;
        }
        return new DeleteHostAnswer(true);
	}
	
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(name, this);
        return true;
    }
    
	private Answer callCommand(Command cmd, Long zoneId) {
		
		HostVO dnsApiHost = getDnsAPIHost(zoneId);
		if (dnsApiHost == null) {
			throw new CloudRuntimeException("Could not find the DNS API resource");
		}
		
		Answer answer = _agentMgr.easySend(dnsApiHost.getId(), cmd);
		if (answer == null || !answer.getResult()) {
			String msg = "Error executing command " + cmd;
			msg = answer == null ? msg : answer.getDetails();
			throw new CloudRuntimeException(msg);
		}
		
		return answer;
	}
	
	private HostVO getDnsAPIHost(Long zoneId) {
		return _hostDao.findByTypeNameAndZoneId(zoneId, Provider.DnsAPI.getName(), Type.L2Networking);
	}
	
	@Override
	@DB
	public Host addDNSAPIHost(Long physicalNetworkId, String username, String password, String url) {
		
		if (username == null || username.trim().isEmpty()) {
			throw new InvalidParameterValueException("Invalid username: " + username);
		}
		
		if (password == null || password.trim().isEmpty()) {
			throw new InvalidParameterValueException("Invalid password: " + password);
		}
		
		if (url == null || url.trim().isEmpty()) {
			throw new InvalidParameterValueException("Invalid url: " + url);
		}
		
		// validate physical network and zone
		// Check if physical network exists
		PhysicalNetwork pNtwk = null;
		if (physicalNetworkId != null) {
			pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
			if (pNtwk == null) {
				throw new InvalidParameterValueException(
						"Unable to find a physical network having the specified physical network id");
			}
		} else {
			throw new InvalidParameterValueException("Invalid physicalNetworkId: " + physicalNetworkId);
		}

		Long zoneId = pNtwk.getDataCenterId();

		Map<String, String> params = new HashMap<String, String>();
		params.put("guid", "dnsapi-" + String.valueOf(zoneId));
		params.put("zoneId", String.valueOf(zoneId));
		params.put("name", Provider.DnsAPI.getName());
		
		params.put("url", url);
		params.put("username", username);
		params.put("password", password);
		
		Map<String, Object> hostDetails = new HashMap<String, Object>();
		hostDetails.putAll(params);
		
		Transaction txn = Transaction.currentTxn();
		txn.start();

		try {
			DnsAPIResource resource = new DnsAPIResource();
			resource.configure(Provider.DnsAPI.getName(), hostDetails);
			
			Host host = _resourceMgr.addHost(zoneId, resource, resource.getType(),
					params);
			
			if (host == null) {
				throw new CloudRuntimeException("Failed to add DNS API host");
			}
			
			// this.signIn(zoneId, username, password);
			
			txn.commit();
			
			return host;
		} catch (ConfigurationException e) {
			txn.rollback();
			throw new CloudRuntimeException(e);
		}
	}
	
	private void signIn(long zoneId, String username, String password) {
	
		if (username == null || password == null) {
			HostVO dnsAPIHost = this.getDnsAPIHost(zoneId);
			_hostDao.loadDetails(dnsAPIHost);
			username = dnsAPIHost.getDetail("username");
			password = dnsAPIHost.getDetail("password");
		}
		
		SignInCommand cmd = new SignInCommand(username, password);
		callCommand(cmd, zoneId);
	}

	private DnsAPINetworkVO getDnsAPINetworkVO(Network network) {
		return _dnsapiNetworkDao.findByNetworkId(network.getId());
	}
	
	private DnsAPIVirtualMachineVO getDnsAPIVMVO(VirtualMachineProfile<? extends VirtualMachine> vm) {
		return _dnsapiVmDao.findByVirtualMachineId(vm.getId());
	}
	
	private Domain getOrCreateDomain(Long zoneId, String domainName, boolean reverse) {
		Long templateId = Long.valueOf(_configServer.getConfigValue(Config.DNSAPITemplateId.key(),
				Config.ConfigurationParameterScope.global.name(), null));

    	// Check if domain already exists
    	Command cmdList;
    	if (reverse) {
    		cmdList = new ListReverseDomainCommand(domainName);
    	} else {
    		cmdList = new ListDomainCommand(domainName);
    	}
    	Answer answerList = callCommand(cmdList, zoneId);
    	List<Domain> domainList = ((DnsAPIDomainListResponse) answerList).getDomainList();
    	if (domainList.size() == 0) {
    		// Doesn't exist yet, create it
    		Command cmdCreate;
    		if (reverse) {
    			cmdCreate = new CreateReverseDomainCommand(domainName, templateId, AUTHORITY_TYPE);
    		} else {
    			cmdCreate = new CreateDomainCommand(domainName, templateId, AUTHORITY_TYPE);
    		}
        	Answer answerCreateDomain = callCommand(cmdCreate, zoneId);
        	return ((DnsAPIDomainResponse) answerCreateDomain).getDomain();
    	} else if (domainList.size() == 1) {
    		return domainList.get(0);
    	} else {
    		throw new CloudRuntimeException("Multiple domains already exist in DNS API");
    	}
	}
		
	private void removeDomain(Long zoneId, Long domainId, boolean reverse) {
    	// Check if domain exists
		Command cmdInfo;
		if (reverse) {
			cmdInfo = new GetReverseDomainInfoCommand(domainId);
		} else {
			cmdInfo = new GetDomainInfoCommand(domainId);
		}
    	Answer answerInfo = callCommand(cmdInfo, zoneId);
    	Domain domain = ((DnsAPIDomainResponse) answerInfo).getDomain();
    	if (domain == null) {
    		// Domain doesn't exist in DNS API
    		// Do nothing, continue
    	} else {
    		// Remove domain
    		Command cmdRemove;
    		if (reverse) {
    			cmdRemove = new RemoveReverseDomainCommand(domainId);
    		} else {
    			cmdRemove = new RemoveDomainCommand(domainId);
    		}
    		callCommand(cmdRemove, zoneId);
    	}
	}
		
	private void saveDomainDB(Network network, Domain createdDomain, Domain reverseDomain) {
    	Transaction txn = Transaction.currentTxn();
		txn.start();
		
    	DnsAPINetworkVO dnsapiNetworkVO = _dnsapiNetworkDao.findByNetworkId(network.getId());
    	if (dnsapiNetworkVO == null) {
    		// Entry in mapping table doesn't exist yet, create it
    		dnsapiNetworkVO = new DnsAPINetworkVO(network.getId(), createdDomain.getId(), reverseDomain.getId());
    		_dnsapiNetworkDao.persist(dnsapiNetworkVO);
    	} else {
    		// An entry already exists
    		if (dnsapiNetworkVO.getNetworkId() == network.getId() && dnsapiNetworkVO.getDnsapiDomainId() == createdDomain.getId() && dnsapiNetworkVO.getDnsapiReverseDomainId() == reverseDomain.getId()) {
    			// All the same, entry already exists, nothing to do
    			return;
    		} else {
    			// Outdated info, update it
    			DnsAPINetworkVO newDnsapiNetworkVO = new DnsAPINetworkVO(network.getId(), createdDomain.getId(), reverseDomain.getId());
    			_dnsapiNetworkDao.update(dnsapiNetworkVO.getId(), newDnsapiNetworkVO);
    		}
    	}
    	
    	txn.commit();
	}
	
	private Record createOrUpdateRecord(Long zoneId, long domainId, String name, String content, boolean reverse) {
		// Check if record already exists
		ListRecordCommand cmdList;
		if (reverse) {
			cmdList = new ListRecordCommand(domainId, content);
		} else {
			cmdList = new ListRecordCommand(domainId, name);
		}
		Answer answerList = callCommand(cmdList, zoneId);
		List<Record> recordList = ((DnsAPIRecordListResponse) answerList).getRecordList();
		Command cmd;
		if (recordList.size() == 0) {
    		// Doesn't exist yet, create it
			if (reverse) {
	        	cmd = new CreateRecordCommand(domainId, name, content, REVERSE_RECORD_TYPE); // Reverse record
			} else {
	        	cmd = new CreateRecordCommand(domainId, name, content, RECORD_TYPE); // Regular record
	    	}
		} else if (recordList.size() == 1) {
			// Record already exists, we should update it with newer info
			if (reverse) {
				cmd = new UpdateRecordCommand(domainId, recordList.get(0).getId(), name, content, REVERSE_RECORD_TYPE);
			} else {
				cmd = new UpdateRecordCommand(domainId, recordList.get(0).getId(), name, content, RECORD_TYPE);
			}
		} else {
			throw new CloudRuntimeException("Multiple records in DNS API");
		}
    	Answer answerCreateRecord = callCommand(cmd, zoneId);
    	return ((DnsAPIRecordResponse) answerCreateRecord).getRecord();
	}
	
	private void removeRecord(Long zoneId, Long recordId) {
    	// Check if record exists
    	GetRecordInfoCommand cmdInfo = new GetRecordInfoCommand(recordId);
    	Answer answerInfo = callCommand(cmdInfo, zoneId);
    	Record record = ((DnsAPIRecordResponse) answerInfo).getRecord();
    	if (record == null) {
    		// Record doesn't exist in DNS API
    		// Do nothing, continue
    	} else {
    		// Remove record
    		RemoveRecordCommand cmdRemove = new RemoveRecordCommand(recordId);
    		callCommand(cmdRemove, zoneId);
    	}
	}
	
	private void saveRecordDB(VirtualMachineProfile<? extends VirtualMachine> vm, Record createdRecord, Record createdReverseRecord) {
    	Transaction txn = Transaction.currentTxn();
		txn.start();
		
		long vmId = vm.getId();
    	DnsAPIVirtualMachineVO dnsapiVMVO = this.getDnsAPIVMVO(vm);
    	if (dnsapiVMVO == null) {
    		// Entry in mapping table doesn't exist yet, create it
    		dnsapiVMVO = new DnsAPIVirtualMachineVO(vmId, createdRecord.getId(), createdReverseRecord.getId());
    		_dnsapiVmDao.persist(dnsapiVMVO);
    	} else {
    		// An entry already exists
    		if (dnsapiVMVO.getVirtualMachineId() == vmId && dnsapiVMVO.getDnsapiRecordId() == createdRecord.getId() && dnsapiVMVO.getDnsapiReverseRecordId() == createdReverseRecord.getId()) {
    			// All the same, entry already exists, nothing to do
    			return;
    		} else {
    			// Outdated info, update it
    			dnsapiVMVO.setVirtualMachineId(vmId);
    			dnsapiVMVO.setDnsapiRecordId(createdRecord.getId());
    			dnsapiVMVO.setDnsapiReverseRecordId(createdReverseRecord.getId());
    			_dnsapiVmDao.update(dnsapiVMVO.getId(), dnsapiVMVO);
    		}
    	}
    	txn.commit();
	}
	
	private void scheduleBindExport(Long zoneId) {
    	ScheduleExportCommand cmdExport = new ScheduleExportCommand();
    	Answer answerExport = callCommand(cmdExport, zoneId);
    	Export export = ((DnsAPIExportResponse) answerExport).getExport();
    	if (export != null) {
    		s_logger.debug(export.getResult());
    	}
	}

}
