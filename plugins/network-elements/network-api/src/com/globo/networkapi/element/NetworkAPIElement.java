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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.amazonaws.util.StringInputStream;
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
import com.cloud.network.element.IpDeployingRequester;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.SourceNatServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

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
//        capabilities.put(Service.StaticNat, null);
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
    
    protected List<?> callNetworkapi(String s_method, String path, String body) {
		final String networkapi_host = "http://networkapi.qa01.globoi.com";
		final String user = System.getenv("NAPI_USER");
		final String password = System.getenv("NAPI_PASS");
		final XStream xstream = getXStream();

		String url = networkapi_host + path;

		HttpClient client = new HttpClient();
		HttpMethodBase method = null;
		if ("GET".equalsIgnoreCase(s_method)) {
			method = new GetMethod(url);
		} else if ("POST".equalsIgnoreCase(s_method)) {
			method = new PostMethod(url);
			((PostMethod) method).setRequestBody(body);
			
		}

		method.addRequestHeader("NETWORKAPI_USERNAME", user);
		method.addRequestHeader("NETWORKAPI_PASSWORD", password);
		String networkapi_xml = null;
    	try {
    		int status_code = client.executeMethod(method);
    		s_logger.debug("status_code = " + status_code);
    		networkapi_xml = method.getResponseBodyAsString();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	} finally {
    		s_logger.debug("Called networkapi " + s_method + " " + url + ":\n" + body + "\n---------\n" + networkapi_xml);
    		method.releaseConnection();
    	}

    	if (networkapi_xml == null) {
    		return null;
    	}
		List<?> results = (List<?>) xstream.fromXML(networkapi_xml);
    	return results;
    }

    protected XStream getXStream() {
        XStream xstream = new XStream() {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                        if (definedIn == Object.class) {
                            return false;
                        }
                        return super.shouldSerializeMember(definedIn, fieldName);
                    }
                };
            }
        };
        xstream.alias("networkapi", List.class);
        xstream.alias("vlan", Vlan.class);
        xstream.alias("network", com.globo.networkapi.element.Network.class);
        return xstream;
    }
    
    protected com.globo.networkapi.element.Network addNetwork(long ip_version, long vlanId, long network_type_id, Long environment_vip_id) {
    	
    	s_logger.debug("addNetwork with ip_version=" + ip_version +  " vlanId=" + vlanId + " network_type_id=" + network_type_id);
    	com.globo.networkapi.element.Network napi_network = new com.globo.networkapi.element.Network();
    	napi_network.setNetwork(ip_version);
    	napi_network.setId_vlan(vlanId);
    	napi_network.setId_network_type(network_type_id);
    	
    	XStream xstream = getXStream();
    	String body = xstream.toXML(napi_network);
    	return (com.globo.networkapi.element.Network) callNetworkapi("POST", "/network/add/", body).get(0);
    }
    
    protected void create_networks(Long vlanId, Long... napi_network_ids) {

    	s_logger.debug("create_networks with vlanId=" + vlanId +  " ids=" + StringUtils.join(napi_network_ids, ','));
    	StringBuilder s = new StringBuilder();
    	s.append("{'network': { [");
    	for (Long napi_network_id : napi_network_ids) {
    		s.append(napi_network_id);
    		s.append(',');
    	}
    	s.deleteCharAt(s.length()-1);
    	s.append("], 'id_vlan': ");
    	s.append(vlanId);
    	s.append("} }");
    	
    	Object o = callNetworkapi("POST", "/network/add/", s.toString());
    	return;
    }

	@SuppressWarnings("unchecked")
	@Override
	public boolean implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		
		try {
			s_logger.debug("entering networkapiElement implement function for network "
					+ network.getDisplayText()
					+ " (state "
					+ network.getState()
					+ ")");
			
			long ambienteId = 82;
			Long vlanId = Long.parseLong(network.getBroadcastUri().getHost());
			List<Vlan> vlans = (List<Vlan>) callNetworkapi("GET", "/vlan/ambiente/" + ambienteId + "/", null);
			Vlan currentVlan = null;
			for (Vlan vlan : vlans) {
				if (vlanId.equals(vlan.getNum_vlan())) {
					currentVlan = vlan;
					break;
				}
			}
			
			if (currentVlan == null) {
				throw new RuntimeException("Vlan with id " + vlanId + " not found in networkapi environmentId " + ambienteId);
			}
			
			// call id_network = Network.add_network(self, network=0 /* ipv4 */, id_vlan=vlan_id,id_network_type=6, id_environment_vip=None)
			com.globo.networkapi.element.Network napi_network = addNetwork(0, currentVlan.getId(), 6, null);
			
			// call Network.create_networks(ids=[id_network], vlan_id)
			create_networks(currentVlan.getId(), napi_network.getId());
		
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
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		s_logger.debug("***** Here we call networkapi to alocate VLAN");
		return true;
	}

	@Override
	public boolean release(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
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
            s_logger.error("Make networkapi as dummy ip deployer, since we likely met this when clean up resource after shutdown network");
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
        s_logger.debug("**** Configuring LB in networkapi");
        return applyLoadBalancerRules(config, rules);
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

}
