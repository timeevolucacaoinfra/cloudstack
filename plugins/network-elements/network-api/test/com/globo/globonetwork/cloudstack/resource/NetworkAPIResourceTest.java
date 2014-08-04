package com.globo.globonetwork.cloudstack.resource;


import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.globo.globonetwork.client.api.VlanAPI;
import com.globo.globonetwork.client.http.HttpXMLRequestProcessor;
import com.globo.globonetwork.client.model.IPv4Network;
import com.globo.globonetwork.client.model.Vlan;
import com.globo.globonetwork.cloudstack.commands.ValidateNicInVlanCommand;

public class NetworkAPIResourceTest {
	NetworkAPIResource _resource;
	
	@Before
	public void setUp() throws ConfigurationException {

		// ConcurrentMap<String, String> cfg = new ConcurrentHashMap<String, String>();
		
		// Long zoneId = Long.valueOf(cfg.get("zoneId"));
		// Long zoneId = 1L;

		// cfg.put("name", "napivlan");
		// cfg.put("zoneId", String.valueOf(zoneId));
		// cfg.put("podId", String.valueOf(1L /* FIXME */));
		// cfg.put("clusterId", String.valueOf(1L /* FIXME */));

		// String username = "x";
		// String password = "y";
		// String url = "z";
		// cfg.putIfAbsent("guid", "networkapi"); // FIXME
		// cfg.putIfAbsent("url", url);
		// cfg.putIfAbsent("username", username);
		// cfg.putIfAbsent("password", password);

		_resource = new NetworkAPIResource();
		// Map<String, Object> params = new HashMap<String, Object>();
		// params.putAll(cfg);
		// _resource.configure("networkapi", params);
	}

	@Test
	public void testValidateNicReturnsAnswerResultTrue() throws Exception {

		long vlanId = 100l;
		_resource._napi = mock(HttpXMLRequestProcessor.class);
		when(_resource._napi.getVlanAPI()).thenReturn(mock(VlanAPI.class));

		// Returning objects
		Vlan vlan = new Vlan();
		vlan.setId(vlanId);
		
		// Setting network to 10.2.3.0/24
		IPv4Network ipv4Network = new IPv4Network();
		ipv4Network.setOct1(10);
		ipv4Network.setOct2(2);
		ipv4Network.setOct3(3);
		ipv4Network.setOct4(0);
		ipv4Network.setMaskOct1(255);
		ipv4Network.setMaskOct2(255);
		ipv4Network.setMaskOct3(255);
		ipv4Network.setMaskOct4(0);
		ipv4Network.setActive(true);
		
		List<IPv4Network> ipv4Networks = new ArrayList<IPv4Network>();
		ipv4Networks.add(ipv4Network);
		vlan.setIpv4Networks(ipv4Networks);
		
		// Mocking return statement
		when (_resource._napi.getVlanAPI().getById(vlanId)).thenReturn(vlan);
		
		ValidateNicInVlanCommand cmd = new ValidateNicInVlanCommand();
		cmd.setVlanId(vlanId);
		cmd.setNicIp("10.2.3.34");
		cmd.setVlanNum(3929l);
		Answer answer = _resource.execute(cmd);
		assertTrue(answer.getResult());
	}
	
	@Test
	public void testValidateNicReturnsAnswerResultFalse() throws Exception {

		long vlanId = 100L;
		_resource._napi = mock(HttpXMLRequestProcessor.class);
		when(_resource._napi.getVlanAPI()).thenReturn(mock(VlanAPI.class));

		// Returning objects
		Vlan vlan = new Vlan();
		vlan.setId(vlanId);
		
		// Setting network to 10.1.3.0/24
		IPv4Network ipv4Network = new IPv4Network();
		ipv4Network.setOct1(10);
		ipv4Network.setOct2(1);
		ipv4Network.setOct3(3);
		ipv4Network.setOct4(0);
		ipv4Network.setMaskOct1(255);
		ipv4Network.setMaskOct2(255);
		ipv4Network.setMaskOct3(255);
		ipv4Network.setMaskOct4(0);
		ipv4Network.setActive(true);
		
		List<IPv4Network> ipv4Networks = new ArrayList<IPv4Network>();
		ipv4Networks.add(ipv4Network);
		vlan.setIpv4Networks(ipv4Networks);
		
		// Mocking return statement
		when (_resource._napi.getVlanAPI().getById(vlanId)).thenReturn(vlan);
		
		ValidateNicInVlanCommand cmd = new ValidateNicInVlanCommand();
		cmd.setVlanId(vlanId);
		cmd.setNicIp("10.2.3.34");
		Answer answer = _resource.execute(cmd);
		assertFalse(answer.getResult());
	}

}
