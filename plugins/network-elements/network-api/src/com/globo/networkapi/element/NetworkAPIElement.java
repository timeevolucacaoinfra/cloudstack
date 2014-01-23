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

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.ExternalLoadBalancerDeviceManagerImpl;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.offering.NetworkOffering;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.google.gson.Gson;

@Component
@Local(value = { NetworkElement.class, LoadBalancingServiceProvider.class })
public class NetworkAPIElement extends ExternalLoadBalancerDeviceManagerImpl implements NetworkElement, LoadBalancingServiceProvider, IpDeployer {
	private static final Logger s_logger = Logger
			.getLogger(NetworkAPIElement.class);

	private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    NetworkModel _networkManager;

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
		s_logger.debug("Checking if NetworkAPI can handle service "
				+ service.getName() + " on network " + network.getDisplayText());
		return true;
	}

    private boolean canHandle(Network config, List<LoadBalancingRule> rules) {
        return true;
    }

    @Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		super.configure(name, params);
		s_logger.debug("Configure " + name + " params " + params);
		// _resourceMgr.registerResourceStateAdapter(name, this);
		return true;
	}

	@Override
	public boolean implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		s_logger.debug("entering networkapiElement implement function for network "
				+ network.getDisplayText()
				+ " (state "
				+ network.getState()
				+ ")");

		return true;
	}

	@Override
	public boolean prepare(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		s_logger.debug("prepare method");
		return true;
	}

	@Override
	public boolean release(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			ReservationContext context) throws ConcurrentOperationException,
			ResourceUnavailableException {
		s_logger.debug("release method");
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
		s_logger.debug("isReady method");
		return true;
	}

	@Override
	public boolean shutdownProviderInstances(
			PhysicalNetworkServiceProvider provider, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
		// Nothing to do here.
		s_logger.debug("shutdownProviderInstances method");
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
        ExternalLoadBalancerDeviceVO lbDevice = getExternalLoadBalancerForNetwork(network);
        if (lbDevice == null) {
            s_logger.error("Cannot find external load balanacer for network " + network.getName());
            s_logger.error("Make F5 as dummy ip deployer, since we likely met this when clean up resource after shutdown network");
            return this;
        }
        if (_networkManager.isNetworkInlineMode(network)) {
            return getIpDeployerForInlineMode(network);
        }
        return this;
	}

    @Override
    public boolean applyLBRules(Network config, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        if (!canHandle(config, rules)) {
            return false;
        }

		s_logger.debug("Called applyLBRules config " + config + " rules " + rules);
        return applyLoadBalancerRules(config, rules);
    }

	@Override
	public boolean validateLBRule(Network network, LoadBalancingRule rule) {
		s_logger.debug("Called validateLBRule Network " + network + " rule " + rule);
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
        return true;
	}


}
