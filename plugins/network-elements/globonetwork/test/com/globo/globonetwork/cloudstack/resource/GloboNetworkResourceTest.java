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
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.rules.FirewallRule;
import com.globo.globonetwork.client.api.EquipmentAPI;
import com.globo.globonetwork.client.api.IpAPI;
import com.globo.globonetwork.client.api.VipAPI;
import com.globo.globonetwork.client.api.VipEnvironmentAPI;
import com.globo.globonetwork.client.api.VlanAPI;
import com.globo.globonetwork.client.exception.GloboNetworkException;
import com.globo.globonetwork.client.http.HttpXMLRequestProcessor;
import com.globo.globonetwork.client.model.Equipment;
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
        when(_resource._globoNetworkApi.getEquipmentAPI()).thenReturn(mock(EquipmentAPI.class));
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
    
    static long IP_SEQUENCE = 100;
    private long getNewIpID() {
        return ++IP_SEQUENCE;
    }
    
    private Vip buildFakeVipValidatedAndCreated(Long vipEnvironment, Long realEnvironment, Long vipIpId, String... reals) throws GloboNetworkException {
        Vip vip = this.buildFakeVip(vipEnvironment, realEnvironment, vipIpId, reals);
        vip.setCreated(true);
        return vip;
    }
    
    private Vip buildFakeVip(Long vipEnvironment, Long realEnvironment, Long vipIpId, String... reals) throws GloboNetworkException {
        Long vipId = 987L;
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

        // real
        String realPort = "8080";

        Ip vipIp = new Ip();
        vipIp.setId(vipIpId);
        vipIp.setOct1(Integer.valueOf(vipIpStr.split("\\.")[0]));
        vipIp.setOct2(Integer.valueOf(vipIpStr.split("\\.")[1]));
        vipIp.setOct3(Integer.valueOf(vipIpStr.split("\\.")[2]));
        vipIp.setOct4(Integer.valueOf(vipIpStr.split("\\.")[3]));
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp(vipIpStr, vipEnvironment)).thenReturn(vipIp);

        VipEnvironment environmentVip = new VipEnvironment();
        environmentVip.setFinality(vipFinality);
        environmentVip.setClient(vipClient);
        environmentVip.setEnvironmentName(vipEnvironmentName);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(vipEnvironment, null, null, null)).thenReturn(environmentVip);

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

        
        for (String realAddr: reals) {
            Ip ip = new Ip();
            ip.setId(getNewIpID());
            when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment(realAddr, realEnvironment)).thenReturn(ip);

            RealIP realIp = new RealIP();
            realIp.setIpId(ip.getId());
            realIp.setName("vm-" + ip.getId());
            realIp.setRealIp(realAddr);
            realIp.setRealPort(Integer.valueOf(realPort));
            vip.setRealsIp(Arrays.asList(realIp));
        }
        
        return vip;
    }

    @Test
    public void testAddVipDefaultValuesResultSuccess() throws Exception {
        
        Long vipEnvironmentId = 123L;
        Long realEnvironmentId = 546L;
        Long vipIpId = 345L;
        String realIp = "10.0.0.54";
        
        Vip vip = buildFakeVip(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);

        // Make sure VIP doesn't exist yet by returning empty list
        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(new ArrayList<Vip>());

        when(_resource._globoNetworkApi.getVipAPI().add(vipIpId, null, null, vip.getFinality(), vip.getClient(), vip.getEnvironment(), vip.getCache(), vip.getMethod(), "(nenhum)", "TCP", "", 5,
                        vip.getHost(), 0, vip.getBusinessArea(), vip.getServiceName(), null, vip.getRealsIp(), Arrays.asList(10), null, vip.getServicePorts(), null)).thenReturn(vip);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip.getId())).thenReturn(vip);

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setHost(vip.getHost());
        cmd.setIpv4(vip.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip.getServicePorts());
        cmd.setBusinessArea(vip.getBusinessArea());
        cmd.setServiceName(vip.getServiceName());
        cmd.setMethodBal("leastconn");
        cmd.setRuleState(FirewallRule.State.Add);

        for (RealIP vipReal : vip.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(String.valueOf(vipReal.getRealPort())));
            real.setRevoked(false);
            real.setEnvironmentId(realEnvironmentId);
            cmd.setRealList(Arrays.asList(real));
        }

        Answer answer = _resource.execute(cmd);
        
        verify(_resource._globoNetworkApi.getVipAPI()).validate(vip.getId());
        verify(_resource._globoNetworkApi.getVipAPI()).create(vip.getId());

        assertNotNull(answer);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);

        GloboNetworkVipResponse response = (GloboNetworkVipResponse)answer;
        assertEquals(vip.getId(), response.getId());
        assertEquals(vip.getIps().get(0), response.getIp());
        assertEquals(vip.getHost(), response.getName());
        assertEquals(vip.getMethod(), response.getMethod());
        assertEquals(vip.getCache(), response.getCache());
        assertEquals(vip.getServicePorts(), response.getPorts());

        assertEquals(1, response.getReals().size());
        assertEquals(1, vip.getRealsIp().size());
        GloboNetworkVipResponse.Real responseReal = response.getReals().get(0);
        assertEquals(vip.getRealsIp().get(0).getRealIp(), responseReal.getIp());
        assertEquals(vip.getRealsIp().get(0).getName(), responseReal.getVmName());
    }

    @Test
    public void testUpdateVip() throws Exception {

        Long vipEnvironmentId = 123L;
        Long realEnvironmentId = 546L;
        Long vipIpId = 345L;
        String realIp = "10.0.0.54";
        
        Vip vip = buildFakeVip(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);

        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(Arrays.asList(vip));

        // Vip after updating
        String vipHostNew = "vip.newdomain.com";
        String vipBusinessAreaNew = "vipbusinessnew";
        String vipServiceNameNew = "vipservicenew";
        String vipMethodBalNew = "round-robin";

        Vip vip2 = buildFakeVip(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);
        vip2.setHost(vipHostNew);
        vip2.setBusinessArea(vipBusinessAreaNew);
        vip2.setMethod(vipMethodBalNew);
        vip2.setServiceName(vipServiceNameNew);
        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip2);

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setHost(vipHostNew);
        cmd.setIpv4(vip2.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip2.getServicePorts());
        cmd.setBusinessArea(vipBusinessAreaNew);
        cmd.setServiceName(vipServiceNameNew);
        cmd.setMethodBal("roundrobin");
        cmd.setRuleState(FirewallRule.State.Add);

        for (RealIP vipReal : vip2.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(String.valueOf(vipReal.getRealPort())));
            real.setRevoked(false);
            real.setEnvironmentId(realEnvironmentId);
            cmd.setRealList(Arrays.asList(real));
        }

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).alter(vip.getId(), vipIpId, null, null, false, false, vip2.getFinality(), vip2.getClient(), vip2.getEnvironment(), vip2.getCache(), vipMethodBalNew,
                "(nenhum)", "TCP", "", 5, vipHostNew, 0, vipBusinessAreaNew, vipServiceNameNew, null, vip2.getRealsIp(), Arrays.asList(10), null, vip2.getServicePorts(), null);
        verify(_resource._globoNetworkApi.getVipAPI()).validate(vip.getId());

        assertNotNull(answer);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);

        GloboNetworkVipResponse response = (GloboNetworkVipResponse)answer;
        assertEquals(vip2.getId(), response.getId());
        assertEquals(vip2.getIps().get(0), response.getIp());
        assertEquals(vipHostNew, response.getName());
        assertEquals(vipMethodBalNew, response.getMethod());
        assertEquals(vip2.getCache(), response.getCache());
        assertEquals(vip2.getServicePorts(), response.getPorts());

        assertEquals(1, response.getReals().size());
        assertEquals(1, vip2.getRealsIp().size());
        GloboNetworkVipResponse.Real responseReal = response.getReals().get(0);
        assertEquals(vip2.getRealsIp().get(0).getRealIp(), responseReal.getIp());
        assertEquals(vip2.getRealsIp().get(0).getName(), responseReal.getVmName());
    }

    @Test
    public void testAddRealInVip() throws Exception {
        Long vipEnvironmentId = 123L;
        Long realEnvironmentId = 546L;
        Long vipIpId = 345L;
        Long realEquipId = 1212L;
        String realIp = "1.2.3.4";
        String realName = "mynewreal";
        String realPort = "8080";
        Long realEquipmentId = 999L;

        Vip vip = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId);

        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(Arrays.asList(vip));

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setHost(vip.getHost());
        cmd.setIpv4(vip.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip.getServicePorts());
        cmd.setBusinessArea(vip.getBusinessArea());
        cmd.setServiceName(vip.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setRuleState(FirewallRule.State.Add);

        GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
        real.setIp(realIp);
        real.setVmName(realName);
        real.setPorts(Arrays.asList(realPort));
        real.setRevoked(false);
        real.setEnvironmentId(realEnvironmentId);
        cmd.setRealList(Arrays.asList(real));

        Equipment realEquipment = new Equipment();
        realEquipment.setId(realEquipmentId);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName(realName)).thenReturn(realEquipment);

        Ip equipIp = new Ip();
        equipIp.setId(realEquipId);
        equipIp.setOct1(Integer.valueOf(realIp.split("\\.")[0]));
        equipIp.setOct2(Integer.valueOf(realIp.split("\\.")[1]));
        equipIp.setOct3(Integer.valueOf(realIp.split("\\.")[2]));
        equipIp.setOct4(Integer.valueOf(realIp.split("\\.")[3]));
        when(_resource._globoNetworkApi.getIpAPI().findIpsByEquipment(realEquipmentId)).thenReturn(Arrays.asList(equipIp));

        // Vip after updating
        Vip vip2 = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip2);

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).addReal(vip.getId(), realEquipId, realEquipmentId, null, null);

        assertNotNull(answer);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);

        GloboNetworkVipResponse response = (GloboNetworkVipResponse)answer;
        assertEquals(vip2.getId(), response.getId());
        assertEquals(vip2.getIps().get(0), response.getIp());
        assertEquals(vip2.getHost(), response.getName());
        assertEquals(vip2.getMethod(), response.getMethod());
        assertEquals(vip2.getCache(), response.getCache());
        assertEquals(vip2.getServicePorts(), response.getPorts());

        assertEquals(1, response.getReals().size());
        assertEquals(1, vip2.getRealsIp().size());
        GloboNetworkVipResponse.Real responseReal = response.getReals().get(0);
        assertEquals(vip2.getRealsIp().get(0).getRealIp(), responseReal.getIp());
        assertEquals(vip2.getRealsIp().get(0).getName(), responseReal.getVmName());
    }

    @Test
    public void testRemoveRealFromVip() throws Exception {
        Long vipEnvironmentId = 123L;
        Long realEnvironmentId = 546L;
        Long vipIpId = 345L;
        String realIp = "10.0.0.54";
        Long realEquipId = 1212L;
        String realPort = "8080";
        Long realEquipmentId = 999L;

        Vip vip = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);

        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(Arrays.asList(vip));

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setHost(vip.getHost());
        cmd.setIpv4(vip.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip.getServicePorts());
        cmd.setBusinessArea(vip.getBusinessArea());
        cmd.setServiceName(vip.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setRuleState(FirewallRule.State.Add);

        for (RealIP vipReal : vip.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(String.valueOf(vipReal.getRealPort())));
            real.setRevoked(true);
            real.setEnvironmentId(realEnvironmentId);
            cmd.setRealList(Arrays.asList(real));
        }

        Equipment realEquipment = new Equipment();
        realEquipment.setId(realEquipmentId);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName(vip.getRealsIp().get(0).getName())).thenReturn(realEquipment);

        Ip equipIp = new Ip();
        equipIp.setId(realEquipId);
        equipIp.setOct1(Integer.valueOf(realIp.split("\\.")[0]));
        equipIp.setOct2(Integer.valueOf(realIp.split("\\.")[1]));
        equipIp.setOct3(Integer.valueOf(realIp.split("\\.")[2]));
        equipIp.setOct4(Integer.valueOf(realIp.split("\\.")[3]));
        when(_resource._globoNetworkApi.getIpAPI().findIpsByEquipment(realEquipmentId)).thenReturn(Arrays.asList(equipIp));

        // Vip after updating
        Vip vip2 = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip2);

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).removeReal(vip.getId(), vip.getRealsIp().get(0).getIpId(), realEquipmentId, null, Integer.valueOf(realPort));

        assertNotNull(answer);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);

        GloboNetworkVipResponse response = (GloboNetworkVipResponse)answer;
        assertEquals(vip2.getId(), response.getId());
        assertEquals(vip2.getIps().get(0), response.getIp());
        assertEquals(vip2.getHost(), response.getName());
        assertEquals(vip2.getMethod(), response.getMethod());
        assertEquals(vip2.getCache(), response.getCache());
        assertEquals(vip2.getServicePorts(), response.getPorts());

        assertEquals(0, response.getReals().size());
    }
    
    @Test
    public void testUpdatePersistenceInVip() throws Exception {
        Long vipEnvironmentId = 123L;
        Long realEnvironmentId = 546L;
        Long vipIpId = 345L;
        String realIp = "10.0.0.54";
        Long realEquipId = 1212L;
        Long realEquipmentId = 999L;
        String persistenceMethod = "cookie";

        Vip vip = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);

        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(Arrays.asList(vip));

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setHost(vip.getHost());
        cmd.setIpv4(vip.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip.getServicePorts());
        cmd.setBusinessArea(vip.getBusinessArea());
        cmd.setServiceName(vip.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setRuleState(FirewallRule.State.Add);
        
        LbStickinessPolicy persistencePolicy = new LbStickinessPolicy("Cookie", null);
        cmd.setPersistencePolicy(persistencePolicy);

        for (RealIP vipReal : vip.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(String.valueOf(vipReal.getRealPort())));
            real.setRevoked(true);
            real.setEnvironmentId(realEnvironmentId);
            cmd.setRealList(Arrays.asList(real));
        }

        Equipment realEquipment = new Equipment();
        realEquipment.setId(realEquipmentId);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName(vip.getRealsIp().get(0).getName())).thenReturn(realEquipment);

        Ip equipIp = new Ip();
        equipIp.setId(realEquipId);
        equipIp.setOct1(Integer.valueOf(realIp.split("\\.")[0]));
        equipIp.setOct2(Integer.valueOf(realIp.split("\\.")[1]));
        equipIp.setOct3(Integer.valueOf(realIp.split("\\.")[2]));
        equipIp.setOct4(Integer.valueOf(realIp.split("\\.")[3]));
        when(_resource._globoNetworkApi.getIpAPI().findIpsByEquipment(realEquipmentId)).thenReturn(Arrays.asList(equipIp));

        // Vip after updating
        Vip vip2 = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);
        vip2.setPersistence(persistenceMethod);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip2);

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).alterPersistence(vip.getId(), persistenceMethod);

        assertNotNull(answer);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);

        GloboNetworkVipResponse response = (GloboNetworkVipResponse)answer;
        assertEquals(vip2.getId(), response.getId());
        assertEquals(vip2.getIps().get(0), response.getIp());
        assertEquals(vip2.getHost(), response.getName());
        assertEquals(vip2.getMethod(), response.getMethod());
        assertEquals(vip2.getCache(), response.getCache());
        assertEquals(vip2.getServicePorts(), response.getPorts());
        assertEquals(vip2.getPersistence(), response.getPersistence());

        assertEquals(1, response.getReals().size());
        assertEquals(1, vip2.getRealsIp().size());
        GloboNetworkVipResponse.Real responseReal = response.getReals().get(0);
        assertEquals(vip2.getRealsIp().get(0).getRealIp(), responseReal.getIp());
        assertEquals(vip2.getRealsIp().get(0).getName(), responseReal.getVmName());
    }
    
    @Test
    public void testUpdateHealthcheckInVip() throws Exception {
        Long vipEnvironmentId = 123L;
        Long realEnvironmentId = 546L;
        Long vipIpId = 345L;
        String realIp = "10.0.0.54";
        Long realEquipId = 1212L;
        Long realEquipmentId = 999L;
        String healthcheckType = "HTTP";
        String healthcheck = "200 OK";
        Long expectedHealthcheckId = 25L;

        Vip vip = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);

        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(Arrays.asList(vip));

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setHost(vip.getHost());
        cmd.setIpv4(vip.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip.getServicePorts());
        cmd.setBusinessArea(vip.getBusinessArea());
        cmd.setServiceName(vip.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setRuleState(FirewallRule.State.Add);
        
        LbHealthCheckPolicy healthcheckPolicy = new LbHealthCheckPolicy(healthcheck, "", 0, 0, 0, 0);
        cmd.setHealthcheckPolicy(healthcheckPolicy);

        for (RealIP vipReal : vip.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(String.valueOf(vipReal.getRealPort())));
            real.setRevoked(true);
            real.setEnvironmentId(realEnvironmentId);
            cmd.setRealList(Arrays.asList(real));
        }

        Equipment realEquipment = new Equipment();
        realEquipment.setId(realEquipmentId);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName(vip.getRealsIp().get(0).getName())).thenReturn(realEquipment);

        Ip equipIp = new Ip();
        equipIp.setId(realEquipId);
        equipIp.setOct1(Integer.valueOf(realIp.split("\\.")[0]));
        equipIp.setOct2(Integer.valueOf(realIp.split("\\.")[1]));
        equipIp.setOct3(Integer.valueOf(realIp.split("\\.")[2]));
        equipIp.setOct4(Integer.valueOf(realIp.split("\\.")[3]));
        when(_resource._globoNetworkApi.getIpAPI().findIpsByEquipment(realEquipmentId)).thenReturn(Arrays.asList(equipIp));

        // Vip after updating
        Vip vip2 = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);
        vip2.setHealthcheck(healthcheck);
        vip2.setHealthcheckType(healthcheckType);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip2);

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).alterHealthcheck(vip.getId(), healthcheckType, _resource.buildHealthcheckString(healthcheck, vip.getHost()), expectedHealthcheckId);

        assertNotNull(answer);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);

        GloboNetworkVipResponse response = (GloboNetworkVipResponse)answer;
        assertEquals(vip2.getId(), response.getId());
        assertEquals(vip2.getIps().get(0), response.getIp());
        assertEquals(vip2.getHost(), response.getName());
        assertEquals(vip2.getMethod(), response.getMethod());
        assertEquals(vip2.getCache(), response.getCache());
        assertEquals(vip2.getServicePorts(), response.getPorts());
        assertEquals(vip2.getHealthcheck(), response.getHealthcheck());
        assertEquals(vip2.getHealthcheckType(), response.getHealthcheckType());

        assertEquals(1, response.getReals().size());
        assertEquals(1, vip2.getRealsIp().size());
        GloboNetworkVipResponse.Real responseReal = response.getReals().get(0);
        assertEquals(vip2.getRealsIp().get(0).getRealIp(), responseReal.getIp());
        assertEquals(vip2.getRealsIp().get(0).getName(), responseReal.getVmName());
    }
}
