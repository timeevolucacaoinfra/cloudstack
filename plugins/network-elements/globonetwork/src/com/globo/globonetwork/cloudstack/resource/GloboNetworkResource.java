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
package com.globo.globonetwork.cloudstack.resource;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.cloud.utils.net.Ip4Address;
import com.cloud.utils.net.NetUtils;
import com.globo.globonetwork.client.exception.GloboNetworkErrorCodeException;
import com.globo.globonetwork.client.exception.GloboNetworkException;
import com.globo.globonetwork.client.http.HttpXMLRequestProcessor;
import com.globo.globonetwork.client.model.Environment;
import com.globo.globonetwork.client.model.VipEnvironment;
import com.globo.globonetwork.client.model.Equipment;
import com.globo.globonetwork.client.model.IPv4Network;
import com.globo.globonetwork.client.model.Ip;
import com.globo.globonetwork.client.model.Real.RealIP;
import com.globo.globonetwork.client.model.Vip;
import com.globo.globonetwork.client.model.Vlan;
import com.globo.globonetwork.cloudstack.commands.AcquireNewIpForLbCommand;
import com.globo.globonetwork.cloudstack.commands.ActivateNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.AddAndEnableRealInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.AddVipInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.CreateNewVlanInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.DeallocateVlanFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.DisableAndRemoveRealInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.GenerateUrlForEditingVipCommand;
import com.globo.globonetwork.cloudstack.commands.GetVipInfoFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.GetVlanInfoFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.GloboNetworkErrorAnswer;
import com.globo.globonetwork.cloudstack.commands.ListAllEnvironmentsFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.RegisterEquipmentAndIpInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.RemoveNetworkInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.RemoveVipFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.UnregisterEquipmentAndIpInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.ValidateNicInVlanCommand;
import com.globo.globonetwork.cloudstack.response.GloboNetworkAllEnvironmentResponse;
import com.globo.globonetwork.cloudstack.response.GloboNetworkAndIPResponse;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse.Real;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVlanResponse;

public class GloboNetworkResource extends ManagerBase implements ServerResource {
	private String _zoneId;
	
	private String _guid;
	
	private String _name;
	
	private String _username;
	
	private String _url;
	
	private String _password;
	
	protected HttpXMLRequestProcessor _globoNetworkApi;
	
	private static final Logger s_logger = Logger.getLogger(GloboNetworkResource.class);
	
	private static final long NETWORK_TYPE = 6; // Rede invalida de equipamentos
	
	private static final Long EQUIPMENT_TYPE = 10L;
	

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		
		try {
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

			_globoNetworkApi = new HttpXMLRequestProcessor(_url, _username, _password);

			if (params.containsKey("readTimeout")) {
				_globoNetworkApi.setReadTimeout(Integer.valueOf((String) params.get("readTimeout")));
			}
			
			if (params.containsKey("connectTimeout")) {
				_globoNetworkApi.setConnectTimeout(Integer.valueOf((String) params.get("connectTimeout")));
			}

			if (params.containsKey("numberOfRetries")) {
				_globoNetworkApi.setNumberOfRetries(Integer.valueOf((String) params.get("numberOfRetries")));
			}

			return true;
		} catch (NumberFormatException e) {
			s_logger.error("Invalid number in configuration parameters", e);
			throw new ConfigurationException("Invalid number in configuration parameters: " + e);
		}
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
		StartupCommand cmd = new StartupCommand(getType());
		cmd.setName(_name);
		cmd.setGuid(_guid);
		cmd.setDataCenter(_zoneId);
		cmd.setPod("");
		cmd.setPrivateIpAddress("");
		cmd.setStorageIpAddress("");
		cmd.setVersion(GloboNetworkResource.class.getPackage().getImplementationVersion());
		return new StartupCommand[] {cmd};
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		return new PingCommand(getType(), id);
	}

	@Override
	public void disconnected() {
	}

	@Override
	public IAgentControl getAgentControl() {
		return null;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
	}

	@Override
	public Answer executeRequest(Command cmd) {
		if (cmd instanceof ReadyCommand) {
			return new ReadyAnswer((ReadyCommand) cmd);
		} else if (cmd instanceof MaintainCommand) {
			return new MaintainAnswer((MaintainCommand) cmd);
		} else if (cmd instanceof GetVlanInfoFromGloboNetworkCommand) {
			return execute((GetVlanInfoFromGloboNetworkCommand) cmd);
		} else if (cmd instanceof ValidateNicInVlanCommand) {
			return execute((ValidateNicInVlanCommand) cmd);
		} else if (cmd instanceof CreateNewVlanInGloboNetworkCommand) {
			return execute((CreateNewVlanInGloboNetworkCommand) cmd);
		} else if (cmd instanceof ActivateNetworkCommand) {
			return execute((ActivateNetworkCommand) cmd);
		} else if (cmd instanceof ListAllEnvironmentsFromGloboNetworkCommand) {
			return execute((ListAllEnvironmentsFromGloboNetworkCommand) cmd);
		} else if (cmd instanceof RemoveNetworkInGloboNetworkCommand) {
			return execute((RemoveNetworkInGloboNetworkCommand) cmd);
		} else if (cmd instanceof DeallocateVlanFromGloboNetworkCommand) {
			return execute((DeallocateVlanFromGloboNetworkCommand) cmd);
		} else if (cmd instanceof RegisterEquipmentAndIpInGloboNetworkCommand) {
			return execute((RegisterEquipmentAndIpInGloboNetworkCommand) cmd);
		} else if (cmd instanceof UnregisterEquipmentAndIpInGloboNetworkCommand) {
			return execute((UnregisterEquipmentAndIpInGloboNetworkCommand) cmd);
		} else if (cmd instanceof GetVipInfoFromGloboNetworkCommand) {
			return execute((GetVipInfoFromGloboNetworkCommand) cmd);
		} else if (cmd instanceof AddAndEnableRealInGloboNetworkCommand) {
			return execute((AddAndEnableRealInGloboNetworkCommand) cmd);
		} else if (cmd instanceof DisableAndRemoveRealInGloboNetworkCommand) {
			return execute((DisableAndRemoveRealInGloboNetworkCommand) cmd);
		} else if (cmd instanceof GenerateUrlForEditingVipCommand) {
			return execute((GenerateUrlForEditingVipCommand) cmd);
		} else if (cmd instanceof RemoveVipFromGloboNetworkCommand) {
			return execute((RemoveVipFromGloboNetworkCommand) cmd);
        } else if (cmd instanceof AcquireNewIpForLbCommand) {
            return execute((AcquireNewIpForLbCommand) cmd);
		} else if (cmd instanceof AddVipInGloboNetworkCommand) {
		    return execute((AddVipInGloboNetworkCommand) cmd);
		}
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer handleGloboNetworkException(Command cmd, GloboNetworkException e) {
		if (e instanceof GloboNetworkErrorCodeException) {
			GloboNetworkErrorCodeException ex = (GloboNetworkErrorCodeException) e;
			s_logger.error("Error accessing GloboNetwork: " + ex.getCode() + " - " + ex.getDescription(), ex);
			return new GloboNetworkErrorAnswer(cmd, ex.getCode(), ex.getDescription());
		} else {
			s_logger.error("Generic error accessing GloboNetwork", e);
			return new Answer(cmd, false, e.getMessage());
		}
	}
	
	/**
	 * Validate if Nic ip and vlan number belongs to GloboNetwork VlanId
	 * @param cmd
	 * @return
	 */
	public Answer execute(ValidateNicInVlanCommand cmd) {
		try {
			Vlan vlan = _globoNetworkApi.getVlanAPI().getById(cmd.getVlanId());
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
			if (!(ipLong >= NetUtils.ip2Long(ipRange[0]) && ipLong <= NetUtils.ip2Long(ipRange[1]))) {
				return new Answer(cmd, false, "Nic IP " + cmd.getNicIp() + " does not belong to network " + networkAddress + " in vlanId " + cmd.getVlanId());
			}
			return new Answer(cmd);
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(AddAndEnableRealInGloboNetworkCommand cmd) {
		try {
			Vip vip = _globoNetworkApi.getVipAPI().getById(cmd.getVipId());
			if (vip == null || !cmd.getVipId().equals(vip.getId())) {
				return new Answer(cmd, false, "Vip request " + cmd.getVipId() + " not found in GloboNetwork");
			}

			Equipment equipment = _globoNetworkApi.getEquipmentAPI().listByName(cmd.getEquipName());
			if (equipment == null) {
				// Equipment doesn't exist
				return new Answer(cmd, false, "Equipment " + cmd.getEquipName() + " doesn't exist in GloboNetwork");
			}
			
			List<Ip> ips = _globoNetworkApi.getIpAPI().findIpsByEquipment(equipment.getId());
			Ip ip = null;
			for (Ip equipIp: ips) {
				String equipIpString = equipIp.getOct1() + "." + equipIp.getOct2() + "." + equipIp.getOct3() + "." + equipIp.getOct4();
				if (equipIpString.equals(cmd.getIp())) {
					ip = equipIp;
				}
			}
			
			if (ip == null) {
				return new Answer(cmd, false, "IP doesn't exist in this GloboNetwork environment");
			}
			
			if (!vip.getValidated()) {
				_globoNetworkApi.getVipAPI().validate(cmd.getVipId());
			}
			
			if (!vip.getCreated()) {
				s_logger.info("Requesting GloboNetwork to create vip " + vip.getId());
				_globoNetworkApi.getVipAPI().create(cmd.getVipId());
			}
			
			if (vip.getRealsIp() != null) {
				for (RealIP realIp:  vip.getRealsIp()) {
					if (ip.getId().equals(realIp.getIpId())) {
						// real already added. Only ensure is enabled
						_globoNetworkApi.getVipAPI().enableReal(cmd.getVipId(), ip.getId(), equipment.getId(), null, null);
						return new Answer(cmd, true, "Real enabled successfully"); 
					}
				}
			}

			// added reals are always enabled by default
			_globoNetworkApi.getVipAPI().addReal(cmd.getVipId(), ip.getId(), equipment.getId(), null, null);
			return new Answer(cmd, true, "Real added and enabled successfully");
			
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(DisableAndRemoveRealInGloboNetworkCommand cmd) {
		try {
			Vip vip = _globoNetworkApi.getVipAPI().getById(cmd.getVipId());
			if (vip == null || !cmd.getVipId().equals(vip.getId())) {
				return new Answer(cmd, false, "Vip request " + cmd.getVipId() + " not found in GloboNetwork");
			}
			
			Equipment equipment = _globoNetworkApi.getEquipmentAPI().listByName(cmd.getEquipName());
			if (equipment == null) {
				// Equipment doesn't exist. So, there is no Vip either.
				return new Answer(cmd, true, "Equipment " + cmd.getEquipName() + " doesn't exist in GloboNetwork");
			}

			if (vip.getRealsIp() != null) {
				for (RealIP realIp:  vip.getRealsIp()) {
					if (cmd.getIp().equals(realIp.getRealIp())) {
						// real exists in vip. Remove it.
						_globoNetworkApi.getVipAPI().removeReal(cmd.getVipId(), realIp.getIpId(), equipment.getId(), realIp.getVipPort(), realIp.getRealPort());
						return new Answer(cmd, true, "Real removed successfully"); 
					}
				}
			}
			return new Answer(cmd, true, "Real not in vipId " + cmd.getVipId());
			
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	private Answer execute(RemoveVipFromGloboNetworkCommand cmd) {
		try {

			Vip vip = _globoNetworkApi.getVipAPI().getById(cmd.getVipId());
			
			if (vip == null || !cmd.getVipId().equals(vip.getId())) {
				return new Answer(cmd, true, "Vip request " + cmd.getVipId() + " was previously removed from GloboNetwork");
			}
			
			// remove VIP from network device
			if (vip.getCreated()) {
				s_logger.info("Requesting GloboNetwork to remove vip from network device vip_id=" + vip.getId());
				_globoNetworkApi.getVipAPI().removeScriptVip(cmd.getVipId());
			}
			
			// remove VIP from GloboNetwork DB
			s_logger.info("Requesting GloboNetwork to remove vip from GloboNetwork DB vip_id=" + vip.getId());
			_globoNetworkApi.getVipAPI().removeVip(cmd.getVipId());
			
			return new Answer(cmd);
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}

	public Answer execute(GetVlanInfoFromGloboNetworkCommand cmd) {
		try {
			Vlan vlan = _globoNetworkApi.getVlanAPI().getById(cmd.getVlanId());
			return createResponse(vlan, cmd);
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}

	public Answer execute(CreateNewVlanInGloboNetworkCommand cmd) {
		try {
			Vlan vlan = _globoNetworkApi.getVlanAPI().allocateWithoutNetwork(cmd.getGloboNetworkEnvironmentId(), cmd.getVlanName(), cmd.getVlanDescription());
			
			/*Network network = */_globoNetworkApi.getNetworkAPI().addNetworkIpv4(vlan.getId(), Long.valueOf(NETWORK_TYPE), null);
			
			// Bug in GloboNetworkApi: I need to have a second call to get networkid
			vlan = _globoNetworkApi.getVlanAPI().getById(vlan.getId());
			return createResponse(vlan, cmd);
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(ActivateNetworkCommand cmd) {
		try {
			_globoNetworkApi.getNetworkAPI().createNetworks(cmd.getNetworkId(), cmd.getVlanId());
			return new Answer(cmd, true, "Network created");
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(ListAllEnvironmentsFromGloboNetworkCommand cmd) {
		try {
			List<Environment> apiEnvironmentList = _globoNetworkApi.getEnvironmentAPI().listAll();
			
			List<GloboNetworkAllEnvironmentResponse.Environment> environmentList = new ArrayList<GloboNetworkAllEnvironmentResponse.Environment>(apiEnvironmentList.size());
			for (Environment apiEnvironment : apiEnvironmentList) {
				GloboNetworkAllEnvironmentResponse.Environment environment = new GloboNetworkAllEnvironmentResponse.Environment();
				environment.setId(apiEnvironment.getId());
				environment.setDcDivisionName(apiEnvironment.getDcDivisionName());
				environment.setL3GroupName(apiEnvironment.getL3GroupName());
				environment.setLogicalEnvironmentName(apiEnvironment.getLogicalEnvironmentName());
				environmentList.add(environment);
			}
			
			return new GloboNetworkAllEnvironmentResponse(cmd, environmentList);
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(RemoveNetworkInGloboNetworkCommand cmd) {
		try {
			_globoNetworkApi.getVlanAPI().remove(cmd.getVlanId());
			
			return new Answer(cmd, true, "Network removed");
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(DeallocateVlanFromGloboNetworkCommand cmd) {
		try {
			_globoNetworkApi.getVlanAPI().deallocate(cmd.getVlanId());
			return new Answer(cmd, true, "Vlan deallocated");
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(RegisterEquipmentAndIpInGloboNetworkCommand cmd) {
		try {
			Equipment equipment = _globoNetworkApi.getEquipmentAPI().listByName(cmd.getVmName());
			if (equipment == null) {
				s_logger.info("Registering virtualmachine " + cmd.getVmName() + " in GloboNetwork");
				// Equipment (VM) does not exist, create it
				equipment = _globoNetworkApi.getEquipmentAPI().insert(cmd.getVmName(), EQUIPMENT_TYPE, cmd.getEquipmentModelId(), cmd.getEquipmentGroupId());
			}
			
			Vlan vlan = _globoNetworkApi.getVlanAPI().getById(cmd.getVlanId());
			
			// Make sure this vlan has one IPv4 network associated to it
			if (vlan.getIpv4Networks().size() == 0) {
				return new Answer(cmd, false, "No IPv4 networks in this vlan");
			} else if (vlan.getIpv4Networks().size() > 1) {
				return new Answer(cmd, false, "Multiple IPv4 networks in this vlan");
			}
			Long networkId = vlan.getIpv4Networks().get(0).getId();
			
			Ip ip = _globoNetworkApi.getIpAPI().findByIpAndEnvironment(cmd.getNicIp(), cmd.getEnvironmentId());
			if (ip == null) {
				// Doesn't exist, create it
				ip = _globoNetworkApi.getIpAPI().saveIpv4(cmd.getNicIp(), equipment.getId(), cmd.getNicDescription(), networkId);
			} else {
				ip = _globoNetworkApi.getIpAPI().getIpv4(ip.getId());
				if (!ip.getEquipments().contains(cmd.getVmName())) {
					_globoNetworkApi.getIpAPI().assocIpv4(ip.getId(), equipment.getId(), networkId);
				}
			}
			
			if (ip == null) {
				return new Answer(cmd, false, "Could not register NIC in GloboNetwork");
			}
			
			return new Answer(cmd, true, "NIC " + cmd.getNicIp() + " registered successfully in GloboNetwork");
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}

	public Answer execute(UnregisterEquipmentAndIpInGloboNetworkCommand cmd) {
		try {
			Equipment equipment = _globoNetworkApi.getEquipmentAPI().listByName(cmd.getVmName());
			if (equipment == null) {
				s_logger.warn("VM was removed from GloboNetwork before being destroyed in Cloudstack. This is not critical, logging inconsistency: VM UUID " + cmd.getVmName());
				return new Answer(cmd);
			}
			
			if (cmd.getEnvironmentId() != null && cmd.getNicIp() != null) {
				Ip ip = _globoNetworkApi.getIpAPI().findByIpAndEnvironment(cmd.getNicIp(), cmd.getEnvironmentId());
				if (ip == null) {
					// Doesn't exist, ignore
					s_logger.warn("IP was removed from GloboNetwork before being destroyed in Cloudstack. This is not critical, logging inconsistency: IP " + cmd.getNicIp());
				} else {
					_globoNetworkApi.getEquipmentAPI().removeIP(equipment.getId(), ip.getId());
				}
			}

			// if there are no more IPs in equipment, remove it.
			List<Ip> ipList = _globoNetworkApi.getIpAPI().findIpsByEquipment(equipment.getId());
			if (ipList.size() == 0) {
				_globoNetworkApi.getEquipmentAPI().delete(equipment.getId());
			}

			return new Answer(cmd, true, "NIC " + cmd.getNicIp() + " deregistered successfully in GloboNetwork");
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(GetVipInfoFromGloboNetworkCommand cmd) {
		try {
			long vipId = cmd.getVipId();
			Vip vip = _globoNetworkApi.getVipAPI().getById(vipId);
			
			return this.createVipResponse(vip, cmd);
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(GenerateUrlForEditingVipCommand cmd) {

		try {
			String url = _globoNetworkApi.getVipAPI().generateVipEditingUrl(cmd.getVipId(), cmd.getVipServerUrl());
			return new Answer(cmd, true, url);
		} catch (GloboNetworkException e) {
			return handleGloboNetworkException(cmd, e);
		}
	}
	
	public Answer execute(AcquireNewIpForLbCommand cmd) {
	    try {
    	    long vipEnvironmentId = cmd.getVipEnvironmentId();
    	    Ip globoIp = _globoNetworkApi.getIpAPI().getAvailableIp4ForVip(vipEnvironmentId, "");
    	    if (globoIp == null) {
    	        return new Answer(cmd, false, "Acquired new Ip for environment vip " + vipEnvironmentId + " returns no answer");
    	    }
    	    
            GloboNetworkAndIPResponse answer = new GloboNetworkAndIPResponse(cmd);
            
            // ip information
            answer.setIp(new com.cloud.utils.net.Ip(globoIp.getIpString()));
            answer.setIpId(globoIp.getId());
            
            // get network information
            Long networkId = globoIp.getNetworkId();
            IPv4Network network = _globoNetworkApi.getNetworkAPI().getNetworkIpv4(networkId);
            if (network == null) {
                return new Answer(cmd, false, "Network with id " + networkId + " not found");
            }
            answer.setNetworkId(networkId);
            answer.setNetworkAddress(network.getNetworkAddressAsString());
            answer.setNetworkBroadcast(network.getBroadcast());
            answer.setNetworkMask(network.getMaskAsString());
            answer.setActive(Boolean.TRUE.equals(network.getActive()));
            long size = network.getBlock();
            
            answer.setNetworkCidr(network.getNetworkAddressAsString() + "/" + size);
            // TODO Gateway is always the first ip in network. Change GloboNetwork to send gateway by itself
            answer.setNetworkGateway(NetUtils.getIpRangeStartIpFromCidr(network.getNetworkAddressAsString(), size));

            // get vlan information
            Long vlanId = network.getVlanId();
            Vlan vlan = _globoNetworkApi.getVlanAPI().getById(vlanId);
            if (vlan == null) {
                return new Answer(cmd, false, "Vlan with id " + vlanId + " not found");
            }
            answer.setVlanId(vlanId);
            answer.setVlanName(vlan.getName());
            answer.setVlanDescription(vlan.getDescription());
            answer.setVlanNum(vlan.getVlanNum().intValue());
    	    return answer;
        } catch (GloboNetworkException e) {
            return handleGloboNetworkException(cmd, e);
        }
	}
	
   public Answer execute(AddVipInGloboNetworkCommand cmd) {
        try {
            
            // FIXME! These parameters will always be null?
            Long expectedHealthcheckId = null;
            List<Integer> realsPriorities = null;
            String l7Filter = null;
            List<Integer> realsWeights = null;
            String healthcheck = null;
            
            // FIXME! Remove hard coded values
            String persistence = "(nenhum)";
            Integer timeout = 10;
            String cache = "(nenhum)";
            Integer maxConn = 5;
            String healthcheckType = "TCP";
            
            // Values that come directly from command
            String balancingMethod = cmd.getMethodBal();
            String businessArea = cmd.getBusinessArea();
            String host = cmd.getHost();
            String serviceName = cmd.getServiceName();
            List<String> ports = cmd.getPorts();
            
            // Process IPs and set RealIP objects to create VIP
            List<RealIP> realsIp = new ArrayList<RealIP>();
            for (GloboNetworkVipResponse.Real real : cmd.getRealList()) {
                RealIP realIP = new RealIP();
                realIP.setName(real.getVmName());
                realIP.setRealIp(real.getIp());
            }
            
            // Check VIP IP in its environment
            Ip ip = _globoNetworkApi.getIpAPI().checkVipIp(cmd.getIpv4(), cmd.getVipEnvironmentId());
            if (ip == null) {
                return new Answer(cmd, false, "IP " + cmd.getIpv4() + " cannot be checked against VIP environment " + cmd.getVipEnvironmentId());
            }
            Long ipv4Id = ip.getId();
            
            // Search for a VIP environment to get its info (needed for finality, client and environment)
            VipEnvironment environmentVip = _globoNetworkApi.getVipEnvironmentAPI().search(cmd.getVipEnvironmentId(), null, null, null);
            if (environmentVip == null) {
                return new Answer(cmd, false, "Could not find VIP environment " + cmd.getVipEnvironmentId());
            }
            String finality = environmentVip.getFinality();
            String client = environmentVip.getClient();
            String environment = environmentVip.getEnvironmentName();
            
            
            // TODO Check if the VIP already exists before creating it
            
            
            // Actually add the VIP to GloboNetwork
            Vip vip = _globoNetworkApi.getVipAPI().add(ipv4Id, null, expectedHealthcheckId,
                    finality, client, environment, cache, balancingMethod, persistence, 
                    healthcheckType, healthcheck, timeout, host, maxConn, businessArea, serviceName, 
                    l7Filter, realsIp, realsPriorities, realsWeights, ports, null);
            
            // TODO Validate and create VIP
            
            
            return this.createVipResponse(vip, cmd);
        } catch (GloboNetworkException e) {
            return handleGloboNetworkException(cmd, e);
        }
    }
   
    private Answer createVipResponse(Vip vip, Command cmd) {
        if (vip == null) {
            return new Answer(cmd, false, "Vip request was not created in GloboNetwork");
        }

        // Using a map rather than a list because different ports come in different objects
        // even though they have the same ID
        // Example
        // {
        //    "id_ip": "33713",
        //    "port_real": "8180",
        //    "port_vip": "80",
        //    "real_ip": "10.20.30.40",
        //    "real_name": "MACHINE01"
        // },
        // {
        //    "id_ip": "33713",
        //    "port_real": "8280",
        //    "port_vip": "80",
        //    "real_ip": "10.20.30.40",
        //    "real_name": "MACHINE01"
        // },

        Map<Long, Real> reals = new HashMap<Long, Real>();
        for(RealIP real : vip.getRealsIp()) {
            Real realResponse = reals.get(real.getIpId());
            if (realResponse == null) {
                // Doesn't exist yet, first time iterating, so add IP parameter and add to list
                realResponse = new Real();
                realResponse.setIp(real.getRealIp());
                realResponse.setVmName(real.getName());
                reals.put(real.getIpId(), realResponse);
            }
            realResponse.getPorts().add(String.valueOf(real.getVipPort()) + ":" + String.valueOf(real.getRealPort()));
        }

        GloboNetworkVipResponse vipResponse = new GloboNetworkVipResponse(cmd,
                vip.getId(), // id
                vip.getHost(), // name
                vip.getIps().size() == 1 ? vip.getIps().get(0) : vip.getIps().toString(), // ip
                null, // network
                vip.getCache(), // cache
                vip.getMethod(), // method
                vip.getPersistence(), // persistence
                vip.getHealthcheckType(), // healtcheck type
                vip.getHealthcheck(), // healthcheck
                vip.getMaxConn(), // maxconn,
                vip.getPorts(),
                reals.values());
         
        return vipResponse;
    }

	private Answer createResponse(Vlan vlan, Command cmd) {
		
		if (vlan.getIpv4Networks().isEmpty()) {
			// Error code 116 from GloboNetwork: 116 : VlanNaoExisteError,
			return new GloboNetworkErrorAnswer(cmd, 116, "No networks in this VLAN");
		}
		
		IPv4Network ipv4Network = vlan.getIpv4Networks().get(0);
		
		String vlanName = vlan.getName();
		String vlanDescription = vlan.getDescription();
		Long vlanId = vlan.getId();
		Long vlanNum = vlan.getVlanNum();
		Ip4Address mask = new Ip4Address(ipv4Network.getMaskOct1() + "." + ipv4Network.getMaskOct2() + "." + ipv4Network.getMaskOct3() + "." + ipv4Network.getMaskOct4());
		Ip4Address networkAddress = new Ip4Address(ipv4Network.getOct1() + "." + ipv4Network.getOct2() + "." + ipv4Network.getOct3() + "." + ipv4Network.getOct4());
		return new GloboNetworkVlanResponse(cmd, vlanId, vlanName, vlanDescription, vlanNum, networkAddress, mask, ipv4Network.getId(), ipv4Network.getActive());
	}

}
