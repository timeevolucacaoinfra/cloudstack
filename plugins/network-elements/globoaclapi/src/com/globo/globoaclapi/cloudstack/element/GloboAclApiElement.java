//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package com.globo.globoaclapi.cloudstack.element;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.globoaclapi.cloudstack.api.AddGloboAclApiHostCmd;
import com.globo.globoaclapi.cloudstack.commands.ACLRuleCommand;
import com.globo.globoaclapi.cloudstack.commands.CreateACLRuleCommand;
import com.globo.globoaclapi.cloudstack.commands.RemoveACLRuleCommand;
import com.globo.globoaclapi.cloudstack.resource.GloboAclApiResource;
import com.globo.globonetwork.cloudstack.dao.GloboNetworkNetworkDao;
import com.globo.globonetwork.cloudstack.manager.GloboNetworkService;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Local(NetworkElement.class)
public class GloboAclApiElement extends AdapterBase implements FirewallServiceProvider, ResourceStateAdapter, GloboAclApiElementService, Configurable{

    @Inject
    protected NetworkModel _networkManager;
    @Inject
    protected EntityManager _entityMgr;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected AgentManager _agentMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    protected PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    protected GloboNetworkService _globoNetworkService;
    @Inject
    protected GloboNetworkNetworkDao _globoNetworkDao;

    private static final Map<Network.Service, Map<Network.Capability, String>> capabilities = setCapabilities();
    private static final Logger s_logger = Logger.getLogger(GloboAclApiElement.class);

    private static final ConfigKey<Boolean> GloboAclTrustSSL = new ConfigKey<>("ACL", Boolean.class, "globoaclapi.trust.ssl", "true",
            "Set true to trust ACL API SSL certificate", true, ConfigKey.Scope.Global);
    private static final ConfigKey<Integer> GloboAclTimeout = new ConfigKey<>("ACL", Integer.class, "globoaclapi.timeout", "60000",
            "Globo ACL API connection timeout in", true, ConfigKey.Scope.Global);

    @Override
    public boolean applyFWRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        if (!canHandle(network)) {
            return false;
        }

        for(FirewallRule rule : rules){
            if(rule.getState().equals(FirewallRule.State.Add) && rule.getType().equals(FirewallRule.FirewallRuleType.User)){
                this.createFirewallRule(network, rule);
            }else if(rule.getState().equals(FirewallRule.State.Revoke) && rule.getType().equals(FirewallRule.FirewallRuleType.User)){
                this.removeFirewallRule(network, rule);
            }
        }
        return true;
    }

    @Override
    public void createFirewallRule(Network network, FirewallRule rule) {
        String destinationCidr = rule.getSourceCidrList().get(0);
        Integer portStart = rule.getSourcePortStart();
        Integer portEnd = rule.getSourcePortEnd();
        String protocol = rule.getProtocol();
        Integer icmpCode = rule.getIcmpCode();
        Integer icmpType = rule.getIcmpType();

        if (destinationCidr == null || destinationCidr.trim().isEmpty()) {
            throw new InvalidParameterValueException("Invalid source CIDR, value should not be empty.");
        }

        if (protocol.equals("tcp") || protocol.equals("udp")) {
            if (portStart == null) {
                throw new InvalidParameterValueException("Port start should not be empty.");
            }
            if (portEnd != null && portEnd < portStart) {
                throw new InvalidParameterValueException("Port end should not be greater than port start.");
            }
        } else if (protocol.equals("icmp")) {
            if (icmpCode == null || icmpType == null) {
                throw new InvalidParameterValueException("ICMP type and code should not be empty");
            }
        }

        CreateACLRuleCommand cmd = createACLRuleCommand(network, rule, CreateACLRuleCommand.class);

        s_logger.debug("Creating ACL rule" + cmd.getAclRuleDescription());

        callCommand(cmd, network.getDataCenterId());
    }

    @Override
    public void removeFirewallRule(Network network, FirewallRule rule) {
        RemoveACLRuleCommand cmd = createACLRuleCommand(network, rule, RemoveACLRuleCommand.class);
        s_logger.debug("Removing ACL rule" + cmd.getAclRuleDescription());
        callCommand(cmd, network.getDataCenterId());
    }

    protected <T extends ACLRuleCommand> T createACLRuleCommand(Network network, FirewallRule rule, Class<T> type) {
        try {
            T cmd = type.newInstance();
            cmd.setProtocol(rule.getProtocol());
            cmd.setDestinationCidr(rule.getSourceCidrList().get(0));
            cmd.setSourceCidr(network.getCidr());
            cmd.setStartPort(rule.getSourcePortStart());
            cmd.setEndPort(rule.getSourcePortEnd());
            cmd.setIcmpCode(rule.getIcmpCode());
            cmd.setIcmpType(rule.getIcmpType());
            cmd.setVlanNumber(getVlanNumber(network));
            cmd.setEnvironmentId(getEnvironmentId(network));
            cmd.setAclOwner(getCallingUser());
            return cmd;
        } catch (InstantiationException | IllegalAccessException ex) {
            return null;
        }
    }

    protected String getCallingUser() {
        return CallContext.current().getCallingUser().getUsername();
    }

    private Long getVlanNumber(Network network) {
        return _globoNetworkService.getVlanInfoFromGloboNetwork(network).getVlanNum();
    }

    private Long getEnvironmentId(Network network) {
        return _globoNetworkDao.findByNetworkId(network.getId()).getGloboNetworkEnvironmentId();
    }

    private boolean canHandle(Network network) {
        DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        boolean isAdvancedZone = zone.getNetworkType() == DataCenter.NetworkType.Advanced;
        boolean isSharedGuestType = network.getGuestType() == Network.GuestType.Shared;
        boolean isGuestTraffic = network.getTrafficType() == Networks.TrafficType.Guest;

        boolean handleInAdvanceZone = (isAdvancedZone && isSharedGuestType && isGuestTraffic);

        if (!handleInAdvanceZone) {
            s_logger.trace("Not handling network with Type  " + network.getGuestType() + " and traffic type " + network.getTrafficType() + " in zone of type " + zone.getNetworkType());
            return false;
        }

        if (!_networkManager.isProviderSupportServiceInNetwork(network.getId(), Network.Service.Firewall, getProvider())) {
            s_logger.trace("Element " + getProvider().getName() + " doesn't support service " + Network.Service.Firewall.getName() + " in the network " + network);
            return false;
        }

        return true;
    }

    private void callCommand(Command cmd, Long zoneId) {
        Answer answer = _agentMgr.easySend(getHost(zoneId).getId(), cmd);
        if (answer == null || !answer.getResult()) {
            String msg = "Error executing command " + cmd;
            throw new CloudRuntimeException(answer == null ? msg : answer.getDetails());
        }
    }

    private HostVO getHost(Long zoneId) {
        HostVO aclApiHost =  _hostDao.findByTypeNameAndZoneId(zoneId, Network.Provider.GloboAclApi.getName(), Host.Type.L2Networking);
        if (aclApiHost == null) {
            throw new CloudRuntimeException("Could not find the Globo ACL API resource");
        }
        return aclApiHost;
    }

    @Override
    public Map<Network.Service, Map<Network.Capability, String>> getCapabilities() {
        return capabilities;
    }

    private static Map<Network.Service, Map<Network.Capability, String>> setCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<>();

        Map<Network.Capability, String> firewallCapabilities = new HashMap<>();
        firewallCapabilities.put(Network.Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Network.Capability.SupportedEgressProtocols, "tcp,udp,icmp,all");
        firewallCapabilities.put(Network.Capability.MultipleIps, "true");
        firewallCapabilities.put(Network.Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Network.Capability.SupportedTrafficDirection, "ingress,egress");
        capabilities.put(Network.Service.Firewall, firewallCapabilities);

        return capabilities;
    }

    @Override
    public Network.Provider getProvider() {
        return Network.Provider.GloboAclApi;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
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
    public boolean verifyServicesCombination(Set<Network.Service> services) {
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(name, this);
        return true;
    }

    @Override
    public Host addGloboAclApiHost(Long physicalNetworkId, String url, String username, String password) {
        if (url == null || url.trim().isEmpty()) {
            throw new InvalidParameterValueException("Invalid ACL API URL");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidParameterValueException("Invalid ACL API username");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidParameterValueException("Invalid ACL API password");
        }

        PhysicalNetwork physicalNetwork;
        if (physicalNetworkId != null) {
            physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Unable to find a physical network having the specified physical network id");
            }
        } else {
            throw new InvalidParameterValueException("Invalid physicalNetworkId");
        }

        final Map<String, String> params = new HashMap<>();
        final long zoneId = physicalNetwork.getDataCenterId();
        params.put("guid", "globoaclapi-" + String.valueOf(zoneId));
        params.put("zoneId", String.valueOf(zoneId));
        params.put("name", Network.Provider.GloboAclApi.getName());
        params.put("url", url);
        params.put("username", username);
        params.put("password", password);
        params.put("trustssl", GloboAclTrustSSL.value().toString());
        params.put("timeout", GloboAclTimeout.value().toString());

        final Map<String, Object> hostDetails = new HashMap<>();
        hostDetails.putAll(params);

        return Transaction.execute(new TransactionCallbackWithException<Host, CloudRuntimeException>() {

            @Override
            public Host doInTransaction(TransactionStatus status) throws CloudRuntimeException {
                try {
                    GloboAclApiResource resource = new GloboAclApiResource();
                    resource.configure(Network.Provider.GloboAclApi.getName(), hostDetails);

                    Host host = _resourceMgr.addHost(zoneId, resource, resource.getType(), params);

                    if (host == null) {
                        throw new CloudRuntimeException("Failed to add Globo ACL API host");
                    }

                    return host;
                } catch (ConfigurationException e) {
                    throw new CloudRuntimeException(e);
                }
            }
        });
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] != null && resource instanceof GloboAclApiResource)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != Host.Type.L2Networking) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(AddGloboAclApiHostCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return this.getClass().getName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ GloboAclTrustSSL, GloboAclTimeout };
    }
}