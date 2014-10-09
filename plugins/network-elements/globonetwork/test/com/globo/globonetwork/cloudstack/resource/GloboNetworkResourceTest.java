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
import java.util.Arrays;
import java.util.List;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.network.rules.FirewallRule;
import com.globo.globonetwork.client.api.IpAPI;
import com.globo.globonetwork.client.api.VipAPI;
import com.globo.globonetwork.client.api.VipEnvironmentAPI;
import com.globo.globonetwork.client.api.VlanAPI;
import com.globo.globonetwork.client.http.HttpXMLRequestProcessor;
import com.globo.globonetwork.client.model.IPv4Network;
import com.globo.globonetwork.client.model.Ip;
import com.globo.globonetwork.client.model.Real.RealIP;
import com.globo.globonetwork.client.model.Vip;
import com.globo.globonetwork.client.model.VipEnvironment;
import com.globo.globonetwork.client.model.Vlan;
import com.globo.globonetwork.cloudstack.commands.AddOrRemoveVipInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.ValidateNicInVlanCommand;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;

public class GloboNetworkResourceTest {
    GloboNetworkResource _resource;

    @Before
    public void setUp() throws ConfigurationException {
        _resource = new GloboNetworkResource();
        _resource._globoNetworkApi = mock(HttpXMLRequestProcessor.class);
        when(_resource._globoNetworkApi.getIpAPI()).thenReturn(mock(IpAPI.class));
        when(_resource._globoNetworkApi.getVipAPI()).thenReturn(mock(VipAPI.class));
        when(_resource._globoNetworkApi.getVipEnvironmentAPI()).thenReturn(mock(VipEnvironmentAPI.class));
        when(_resource._globoNetworkApi.getVlanAPI()).thenReturn(mock(VlanAPI.class));
    }

    @Test
    public void testValidateNicReturnsAnswerResultTrue() throws Exception {

        long vlanId = 100l;

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
        when(_resource._globoNetworkApi.getVlanAPI().getById(vlanId)).thenReturn(vlan);

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
        when(_resource._globoNetworkApi.getVlanAPI().getById(vlanId)).thenReturn(vlan);

        ValidateNicInVlanCommand cmd = new ValidateNicInVlanCommand();
        cmd.setVlanId(vlanId);
        cmd.setNicIp("10.2.3.34");
        Answer answer = _resource.execute(cmd);
        assertFalse(answer.getResult());
    }

    @Test
    public void testAddVipDefaultValuesResultSuccess() throws Exception {
        String realIP = "10.0.0.54";
        Long realEnvironment = 120L;
        Long ipId = 189L;
        String realName = "myvm";
        String realPort = "8080";

        Long vipIpId = 344L;
        Long vipId = 987L;
        Long vipEnvironment = 23L;
        String vipIpStr = "192.168.1.15";
        String vipHost = "vip.domain.com";
        List<String> vipPorts = Arrays.asList("80:8080");
        String vipBusinessArea = "vipbusiness";
        String vipServiceName = "vipservice";
        String vipMethodBal = "least-conn";
        String vipFinality = "BACKEND";
        String vipClient = "CLIENT";
        String vipEnvironmentName = "TESTAPI";
        String vipCache = "(nenhum)";

        Ip ip = new Ip();
        ip.setId(ipId);
        when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment(realIP, realEnvironment)).thenReturn(ip);

        VipEnvironment environmentVip = new VipEnvironment();
        environmentVip.setFinality(vipFinality);
        environmentVip.setClient(vipClient);
        environmentVip.setEnvironmentName(vipEnvironmentName);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(vipEnvironment, null, null, null)).thenReturn(environmentVip);

        Ip vipIp = new Ip();
        vipIp.setId(vipIpId);
        vipIp.setOct1(Integer.valueOf(vipIpStr.split("\\.")[0]));
        vipIp.setOct2(Integer.valueOf(vipIpStr.split("\\.")[1]));
        vipIp.setOct3(Integer.valueOf(vipIpStr.split("\\.")[2]));
        vipIp.setOct4(Integer.valueOf(vipIpStr.split("\\.")[3]));
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp(vipIpStr, vipEnvironment)).thenReturn(vipIp);

        // Make sure VIP doesn't exist yet by returning empty list
        when(_resource._globoNetworkApi.getVipAPI().getByIp(vipIpStr)).thenReturn(new ArrayList<Vip>());

        Vip vip = new Vip();
        vip.setId(vipId);
        vip.setIps(Arrays.asList(vipIpStr));
        vip.setServicePorts(vipPorts);
        vip.setHost(vipHost);
        vip.setBusinessArea(vipBusinessArea);
        vip.setMethod(vipMethodBal);
        vip.setServiceName(vipServiceName);
        vip.setFinality(vipFinality);
        vip.setClient(vipClient);
        vip.setEnvironment(vipEnvironmentName);
        vip.setCache(vipCache);
        vip.setCreated(false);
        RealIP realIp = new RealIP();
        realIp.setIpId(ipId);
        realIp.setName(realName);
        realIp.setRealIp(realIP);
        realIp.setRealPort(Integer.valueOf(realPort));
        vip.setRealsIp(Arrays.asList(realIp));

        when(_resource._globoNetworkApi.getVipAPI().add(vipIpId, null, null, vipFinality, vipClient, vipEnvironmentName, vipCache, vipMethodBal, "(nenhum)", "TCP", "", 5,
                        vipHost, 0, vipBusinessArea, vipServiceName, null, Arrays.asList(realIp), Arrays.asList(10), null, vipPorts, null)).thenReturn(vip);

        when(_resource._globoNetworkApi.getVipAPI().getById(vipId)).thenReturn(vip);

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setHost(vipHost);
        cmd.setIpv4(vipIpStr);
        cmd.setVipEnvironmentId(vipEnvironment);
        cmd.setPorts(vipPorts);
        cmd.setBusinessArea(vipBusinessArea);
        cmd.setServiceName(vipServiceName);
        cmd.setMethodBal("leastconn");
        cmd.setRuleState(FirewallRule.State.Add);
        cmd.setRealsEnvironmentId(realEnvironment);

        GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
        real.setIp(realIP);
        real.setVmName(realName);
        real.setPorts(Arrays.asList(realPort));
        real.setRevoked(false);
        cmd.setRealList(Arrays.asList(real));

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).validate(vipId);
        verify(_resource._globoNetworkApi.getVipAPI()).create(vipId);

        assertNotNull(answer);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);

        GloboNetworkVipResponse response = (GloboNetworkVipResponse)answer;
        assertEquals(vipId, response.getId());
        assertEquals(vipIpStr, response.getIp());
        assertEquals(vipHost, response.getName());
        assertEquals(vipMethodBal, response.getMethod());
        assertEquals(vipCache, response.getCache());
        assertEquals(vipPorts, response.getPorts());

        assertEquals(1, response.getReals().size());
        GloboNetworkVipResponse.Real responseReal = response.getReals().get(0);
        assertEquals(realIP, responseReal.getIp());
        assertEquals(realName, responseReal.getVmName());
    }
}
