package com.globo.networkapi.element;

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

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.ExternalLoadBalancerDeviceManagerImpl;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.networkapi.manager.NetworkAPIService;
import com.globo.networkapi.resource.NetworkAPIResource;
import com.google.gson.Gson;

@Component
@Local(value = { NetworkElement.class, LoadBalancingServiceProvider.class })
public class NetworkAPIElement extends ExternalLoadBalancerDeviceManagerImpl implements NetworkElement, LoadBalancingServiceProvider, IpDeployer {
	private static final Logger s_logger = Logger
			.getLogger(NetworkAPIElement.class);

	private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    DataCenterDao _dcDao;
    @Inject
    NetworkModel _networkManager;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    NetworkAPIService _networkAPIService;
    @Inject
    ResourceManager _resourceMgr;
	@Inject
	HostDao _hostDao;
	@Inject
	PhysicalNetworkDao _physicalNetworkDao;
    
    @Override
	public Map<Service, Map<Capability, String>> getCapabilities() {
		return capabilities;
	}

	private static Map<Service, Map<Capability, String>> setCapabilities() {

//		Capability.SupportedLBAlgorithms, Capability.SupportedLBIsolation,
//        Capability.SupportedProtocols, Capability.TrafficStatistics, Capability.LoadBalancingSupportedIps,
//        Capability.SupportedStickinessMethods, Capability.ElasticLb, Capability.LbSchemes

		// Set capabilities for LB service
        Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();

        // Specifies that the RoundRobin and Leastconn algorithms are supported for load balancing rules
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "least-conn, round-robin, weighted, uri-hash");

        // specifies that F5 BIG IP network element can provide shared mode only
        lbCapabilities.put(Capability.SupportedLBIsolation, "dedicated, shared");

        // Specifies that load balancing rules can be made for either TCP or UDP traffic
        lbCapabilities.put(Capability.SupportedProtocols, "tcp,udp,http");

        // Specifies that this element can measure network usage on a per public IP basis
//        lbCapabilities.put(Capability.TrafficStatistics, "per public ip");

        // Specifies that load balancing rules can only be made with public IPs that aren't source NAT IPs
        lbCapabilities.put(Capability.LoadBalancingSupportedIps, "additional");

        // Support inline mode with firewall
        lbCapabilities.put(Capability.InlineMode, "true");
        
        //support only for public lb
        lbCapabilities.put(Capability.LbSchemes, LoadBalancerContainer.Scheme.Public.toString());

        LbStickinessMethod method;
        List<LbStickinessMethod> methodList = new ArrayList<LbStickinessMethod>();
        method = new LbStickinessMethod(StickinessMethodType.LBCookieBased, "This is cookie based sticky method, can be used only for http");
        methodList.add(method);
//        method.addParam("holdtime", false, "time period (in seconds) for which persistence is in effect.", false);

        Gson gson = new Gson();
        String stickyMethodList = gson.toJson(methodList);
        lbCapabilities.put(Capability.SupportedStickinessMethods, stickyMethodList);
        
		Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        capabilities.put(Service.Lb, lbCapabilities);
		return capabilities;
	}

	@Override
	public Provider getProvider() {
		return Provider.NetworkAPI;
	}

	protected boolean canHandle(Network network, Service service) {
		s_logger.trace("Checking if NetworkAPI can handle service "
				+ service.getName() + " on network " + network.getDisplayText());
		
        DataCenter zone = _dcDao.findById(network.getDataCenterId());
        boolean handleInAdvanceZone = (zone.getNetworkType() == NetworkType.Advanced &&
                network.getGuestType() == Network.GuestType.Shared && network.getTrafficType() == TrafficType.Guest);

        if (!handleInAdvanceZone) {
            s_logger.trace("Not handling network with Type  " + network.getGuestType() + " and traffic type " + network.getTrafficType() + " in zone of type " + zone.getNetworkType());
            return false;
        }

        return (_networkManager.isProviderForNetwork(getProvider(), network.getId()) && _ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(), service, Network.Provider.NetworkAPI));
	}

    @Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		s_logger.debug("Configure " + name + " params " + params);
		super.configure(name, params);
		return true;
	}
    
	@SuppressWarnings("unchecked")
	@Override
	public boolean implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		
		try {
			s_logger.debug("\n\n\n*** entering networkapiElement implement function for network "
					+ network.getDisplayText()
					+ " (state "
					+ network.getState()
					+ ")");
			
//			try {
//				_networkAPIService.allocateVlan(network, dest.getCluster());
//			} catch (ConfigurationException e) {
//				throw new InsufficientNetworkCapacityException("Unable to configure NetworkAPI as resource", Network.class, network.getId());
//			}

		
		} finally {
			s_logger.debug("leaving networkapiElement implement function for network "
					+ network.getDisplayText()
					+ " (state "
					+ network.getState()
					+ ")");
		}

		return true;
	}

	@Override
	public boolean prepare(Network network, NicProfile nic,
			VirtualMachineProfile vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		s_logger.debug("***** Here we call networkapi to alocate VLAN");

		return true;
	}

	@Override
	public boolean release(Network network, NicProfile nic,
			VirtualMachineProfile vm,
			ReservationContext context) throws ConcurrentOperationException,
			ResourceUnavailableException {
		s_logger.debug("*** Here we call networkapi to unallocate VLAN");
		return true;
	}

	@Override
	public boolean shutdown(Network network, ReservationContext context,
			boolean cleanup) throws ConcurrentOperationException,
			ResourceUnavailableException {
		s_logger.debug("shutdown method");
		return true;
	}

	@Override
	public boolean destroy(Network network, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
		s_logger.debug("destroy method");
		if (!canHandle(network, Service.Lb)) {
			return false;
		}

		return true;
	}

	@Override
	public boolean isReady(PhysicalNetworkServiceProvider provider) {
		return _networkAPIService.canEnable(provider.getPhysicalNetworkId());
	}

	@Override
	public boolean shutdownProviderInstances(
			PhysicalNetworkServiceProvider provider, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
    	PhysicalNetwork pNtwk = _physicalNetworkDao.findById(provider.getPhysicalNetworkId());
    	Host host = _hostDao.findByTypeNameAndZoneId(pNtwk.getDataCenterId(), Provider.NetworkAPI.getName(), Type.L2Networking);
    	if (host != null) {
    		_resourceMgr.deleteHost(host.getId(), true, false);
    	}
		return true;
	}

	@Override
	public boolean canEnableIndividualServices() {
		s_logger.debug("canEnableIndividualServices method");
		return true;
	}

	@Override
	public boolean verifyServicesCombination(Set<Service> services) {
		// This element can only function in a Nicra Nvp based
		// SDN network, so Connectivity needs to be present here
		s_logger.debug("verifyServicesCombination method" + services);
//		if (!services.contains(Service.Connectivity)) {
//			s_logger.warn("Unable to provide services without Connectivity service enabled for this element");
//			return false;
//		}
		return true;
	}

	@Override
	public IpDeployer getIpDeployer(Network network) {
        return this;
	}

    @Override
    public boolean applyLBRules(Network config, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
//        if (!canHandle(config, rules)) {
//            return false;
//        }
		s_logger.debug("Called applyLBRules config " + config + " rules " + rules);
        s_logger.debug("**** Configuring LB in networkapi");
//        return applyLoadBalancerRules(config, rules);
        return false;
    }

	@Override
	public boolean validateLBRule(Network network, LoadBalancingRule rule) {
		s_logger.debug("Called validateLBRule Network " + network + " rule " + rule);
        s_logger.debug("**** Validating LB in networkapi (???)");
		return true;
	}

	@Override
	public List<LoadBalancerTO> updateHealthChecks(Network network,
			List<LoadBalancingRule> lbrules) {
		s_logger.debug("Called updateHealthChecks Network " + network + " lbrules " + lbrules);
		return null;
	}

	@Override
	public boolean applyIps(Network network,
			List<? extends PublicIpAddress> ipAddress, Set<Service> services)
			throws ResourceUnavailableException {
        // return true, as IP will be associated as part of LB rule configuration
		s_logger.debug("Called applyIps Network " + network + " ipAddress " + ipAddress + " services " + services);
        s_logger.debug("**** Adicionando reals in networkapi");
        return true;
	}

	@Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, final StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupCommand && resource instanceof NetworkAPIResource)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
	}

	
}
