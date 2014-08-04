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

public class GloboNetworkResourceTest {
	GloboNetworkResource _resource;
	
	@Before
	public void setUp() throws ConfigurationException {
		_resource = new GloboNetworkResource();
	}

	@Test
	public void testValidateNicReturnsAnswerResultTrue() throws Exception {

		long vlanId = 100l;
		_resource._globoNetworkApi = mock(HttpXMLRequestProcessor.class);
		when(_resource._globoNetworkApi.getVlanAPI()).thenReturn(mock(VlanAPI.class));

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
		when (_resource._globoNetworkApi.getVlanAPI().getById(vlanId)).thenReturn(vlan);
		
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
		_resource._globoNetworkApi = mock(HttpXMLRequestProcessor.class);
		when(_resource._globoNetworkApi.getVlanAPI()).thenReturn(mock(VlanAPI.class));

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
		when (_resource._globoNetworkApi.getVlanAPI().getById(vlanId)).thenReturn(vlan);
		
		ValidateNicInVlanCommand cmd = new ValidateNicInVlanCommand();
		cmd.setVlanId(vlanId);
		cmd.setNicIp("10.2.3.34");
		Answer answer = _resource.execute(cmd);
		assertFalse(answer.getResult());
	}

}
