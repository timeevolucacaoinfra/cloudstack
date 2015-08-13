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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.globo.globonetwork.client.api.GloboNetworkAPI;
import com.globo.globonetwork.client.api.NetworkAPI;
import com.globo.globonetwork.client.api.PoolAPI;
import com.globo.globonetwork.client.model.IPv4Network;
import com.globo.globonetwork.client.model.Network;
import com.globo.globonetwork.client.model.Pool;
import com.globo.globonetwork.client.model.VipJson;
import com.globo.globonetwork.client.model.VipPoolMap;
import com.globo.globonetwork.client.model.VipXml;
import com.globo.globonetwork.client.model.Vlan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
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
import com.globo.globonetwork.client.model.Equipment;
import com.globo.globonetwork.client.model.Ip;
import com.globo.globonetwork.client.model.Ipv4;
import com.globo.globonetwork.client.model.Real.RealIP;
import com.globo.globonetwork.client.model.Vip;
import com.globo.globonetwork.client.model.VipEnvironment;
import com.globo.globonetwork.cloudstack.commands.AddOrRemoveVipInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;
import org.mockito.internal.stubbing.answers.ThrowsExceptionClass;

public class GloboNetworkResourceTest {
    GloboNetworkResource _resource;
    Ipv4 vipIp;
    @Before
    public void setUp() throws ConfigurationException {
        _resource = new GloboNetworkResource();
        _resource._globoNetworkApi = mock(GloboNetworkAPI.class);
        when(_resource._globoNetworkApi.getEquipmentAPI()).thenReturn(mock(EquipmentAPI.class));
        when(_resource._globoNetworkApi.getIpAPI()).thenReturn(mock(IpAPI.class));
        when(_resource._globoNetworkApi.getVipAPI()).thenReturn(mock(VipAPI.class));
        when(_resource._globoNetworkApi.getVipEnvironmentAPI()).thenReturn(mock(VipEnvironmentAPI.class));
        when(_resource._globoNetworkApi.getVlanAPI()).thenReturn(mock(VlanAPI.class));
        when(_resource._globoNetworkApi.getNetworkAPI()).thenReturn(mock(NetworkAPI.class));
        when(_resource._globoNetworkApi.getPoolAPI()).thenReturn(mock(PoolAPI.class));
        when(_resource._globoNetworkApi.getVipAPI()).thenReturn(mock(VipAPI.class));
    }

    static long s_ipSequence = 100;

    @Test
    public void testAddVipDefaultValuesResultSuccess() throws Exception {

        Long vipEnvironmentId = 123L;
        Long realEnvironmentId = 546L;
        Long vipIpId = 345L;
        Long equipId = 123l;
        Long envId = 121l;
        Long vlanId = 12l;
        String realIp = "10.0.0.54";
        List<String> ports = Arrays.asList(new String[] { "80:8080", "443:8443" });

        Vip vip = buildFakeVip(vipEnvironmentId, realEnvironmentId, vipIpId, ports, realIp);

        // Make sure VIP doesn't exist yet by returning empty list
        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(new ArrayList<Vip>());

        when(
                _resource._globoNetworkApi.getVipAPI().add(vipIpId, null, null, vip.getFinality(), vip.getClient(), vip.getEnvironment(), vip.getCache(), vip.getMethod(),
                        "(nenhum)", "TCP", "", 5, vip.getHost(), 0, vip.getBusinessArea(), vip.getServiceName(), null, vip.getRealsIp(), Arrays.asList(0), null,
                        ports, null)).thenReturn(vip);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip.getId())).thenReturn(vip);



        Network network = new IPv4Network();
        network.setVlanId(vlanId);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(vipIp.getNetworkId(), false)).thenReturn(network);

        Vlan vlan = new Vlan();
        vlan.setEnvironment(envId);
        when(_resource._globoNetworkApi.getVlanAPI().getById(vlanId)).thenReturn(vlan);

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setHost(vip.getHost());
        cmd.setIpv4(vip.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip.getServicePorts());
        cmd.setBusinessArea(vip.getBusinessArea());
        cmd.setServiceName(vip.getServiceName());
        cmd.setMethodBal("leastconn");
        cmd.setRuleState(FirewallRule.State.Add);
        cmd.setPorts(ports);

        List<String> equipNames = new ArrayList<String>();
        List<Long> equipIds = new ArrayList<Long>();
        List<Integer> priorities = new ArrayList<Integer>();
        List<Long> weights = new ArrayList<Long>();
        List<Long> idPoolMembers = new ArrayList<Long>();

        List<GloboNetworkVipResponse.Real> realList = new ArrayList<GloboNetworkVipResponse.Real>();
        for (RealIP vipReal : vip.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(vipReal.getVipPort() + ":" + vipReal.getRealPort()));
            real.setRevoked(false);
            real.setEnvironmentId(realEnvironmentId);
            realList.add(real);

            Equipment eq = new Equipment();
            eq.setName(real.getVmName() + "_eq_name");
            eq.setId(equipId);
            when(_resource._globoNetworkApi.getEquipmentAPI().listByName(real.getVmName())).thenReturn(eq);


            equipNames.add(eq.getName());
            equipIds.add(eq.getId());
            priorities.add(0);
            weights.add(0l);
            idPoolMembers.add(0l);

        }
        cmd.setRealList(realList);


        Long poolId = 50l;
        List<VipPoolMap> vipPortsToPools = new ArrayList<VipPoolMap>();
        vipPortsToPools.add(new VipPoolMap(50l,80));
        vipPortsToPools.add(new VipPoolMap(50l,443));

        Pool pool = new Pool();
        pool.setId(poolId);

        mockPoolSave(pool, new RealIP(101l, 8080, "10.0.0.54", 80), false);
        mockPoolSave(pool, new RealIP(101l, 8443, "10.0.0.54", 443), true);


//            vipIpId, null, null, vip.getFinality(), vip.getClient(), vip.getEnvironment(), vip.getCache(), vip.getMethod(),
//                    "(nenhum)", "TCP", "", 5, vip.getHost(), 0, vip.getBusinessArea(), vip.getServiceName(), null, vip.getRealsIp(), Arrays.asList(0), null,
//                    ports, null)).thenReturn(vip);
        Vip vip2 = new VipJson();
        vip2.setId(vip.getId());
        when(_resource._globoNetworkApi.getVipAPI().save(vipIp.getId(),
                null,
                vip.getFinality(),
                vip.getClient(),
                vip.getEnvironment(),
                vip.getCache(),
                "(nenhum)",
                5,
                vip.getHost(),
                vip.getBusinessArea(),
                vip.getServiceName(),
                null,
                vipPortsToPools,
                null,
                null)
        ).thenReturn(vip2);


        when(_resource._globoNetworkApi.getVipAPI().getById(vip.getId())).thenReturn(vip);


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
        List<String> ports = Arrays.asList(new String[] { "80:8080", "443:8443" });

        Vip vip = buildFakeVip(vipEnvironmentId, realEnvironmentId, vipIpId, ports, realIp);

        // Vip after updating
        String vipHostNew = "vip.newdomain.com";
        String vipBusinessAreaNew = "vipbusinessnew";
        String vipServiceNameNew = "vipservicenew";
        String vipMethodBalNew = "round-robin";

        Vip vip2 = buildFakeVipWithPools(vipEnvironmentId, realEnvironmentId, vipIpId, ports, 100l);
        vip2.setHost(vipHostNew);
        vip2.setBusinessArea(vipBusinessAreaNew);
        vip2.setMethod(vipMethodBalNew);
        vip2.setServiceName(vipServiceNameNew);

        when(_resource._globoNetworkApi.getVipAPI().getByPk(vip2.getId())).thenReturn(vip2);



        Long envId = 121l;
        Long vlanId = 12l;
        Network network = new IPv4Network();
        network.setVlanId(vlanId);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(vipIp.getNetworkId(), false)).thenReturn(network);

        Vlan vlan = new Vlan();
        vlan.setEnvironment(envId);
        when(_resource._globoNetworkApi.getVlanAPI().getById(vlanId)).thenReturn(vlan);


        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setVipId(vip.getId());
        cmd.setHost(vipHostNew);
        cmd.setIpv4(vip2.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip2.getServicePorts());
        cmd.setBusinessArea(vipBusinessAreaNew);
        cmd.setServiceName(vipServiceNameNew);
        cmd.setMethodBal("roundrobin");
        cmd.setRuleState(FirewallRule.State.Add);

        List<GloboNetworkVipResponse.Real> realList = new ArrayList<GloboNetworkVipResponse.Real>();
        for (String port : ports) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp("10.1.0.1_" + port);
            real.setVmName("vm-1_" + port);
            real.setPorts(Arrays.asList(port));
            real.setRevoked(false);
            real.setEnvironmentId(realEnvironmentId);
            realList.add(real);

            Ip ip = new Ipv4();
            ip.setId(Long.valueOf(port.split(":")[0]));
            when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment(real.getIp(), realEnvironmentId, false)).thenReturn(ip);

            Equipment eq = new Equipment();
            eq.setName("123_eq_name");
            eq.setId(123l);
            when(_resource._globoNetworkApi.getEquipmentAPI().listByName("vm-1_" + port)).thenReturn(eq);
        }
        cmd.setRealList(realList);


        Answer answer = _resource.execute(cmd);

//        verify(_resource._globoNetworkApi.getVipAPI()).alter(vip.getId(), vipIpId, null, null, false, false, vip2.getFinality(), vip2.getClient(), vip2.getEnvironment(),
//                vip2.getCache(), vipMethodBalNew, "(nenhum)", "TCP", "", 5, vipHostNew, 0, vipBusinessAreaNew, vipServiceNameNew, null, vip2.getRealsIp(), Arrays.asList(0), null,
//                vip2.getServicePorts(), null);
//        verify(_resource._globoNetworkApi.getVipAPI()).validate(vip.getId());

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


    // vip already exists and test add real
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

        when(_resource._globoNetworkApi.getVipAPI().getByPk(vipIpId)).thenReturn(vip);


        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(Arrays.asList(vip));

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setVipId(vip.getId());
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
        real.setPorts(vip.getServicePorts());
        real.setRevoked(false);
        real.setEnvironmentId(realEnvironmentId);
        cmd.setRealList(Arrays.asList(real));

        Equipment realEquipment = new Equipment();
        realEquipment.setId(realEquipmentId);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName(realName)).thenReturn(realEquipment);

        Ipv4 equipIpv4 = new Ipv4();
        equipIpv4.setId(realEquipId);
        equipIpv4.setOct1(Integer.valueOf(realIp.split("\\.")[0]));
        equipIpv4.setOct2(Integer.valueOf(realIp.split("\\.")[1]));
        equipIpv4.setOct3(Integer.valueOf(realIp.split("\\.")[2]));
        equipIpv4.setOct4(Integer.valueOf(realIp.split("\\.")[3]));
        Ip equipIp = (Ip) equipIpv4;
        when(_resource._globoNetworkApi.getIpAPI().findIpsByEquipment(realEquipmentId)).thenReturn(Arrays.asList(equipIp));

        // Vip after updating
        Vip vip2 = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip2);

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).addReal(vip.getId(), realEquipId, realEquipmentId, 80, 8080);

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

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setVipId(vip.getId());
        cmd.setHost(vip.getHost());
        cmd.setIpv4(vip.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip.getServicePorts());
        cmd.setBusinessArea(vip.getBusinessArea());
        cmd.setServiceName(vip.getServiceName());
        cmd.setMethodBal("leastconn");
        cmd.setRuleState(FirewallRule.State.Add);

        List<GloboNetworkVipResponse.Real> realList = new ArrayList<GloboNetworkVipResponse.Real>();
        for (RealIP vipReal : vip.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(vipReal.getVipPort() + ":" + vipReal.getRealPort()));
            real.setRevoked(true);
            real.setEnvironmentId(realEnvironmentId);
            realList.add(real);
        }
        cmd.setRealList(realList);

        Equipment realEquipment = new Equipment();
        realEquipment.setId(realEquipmentId);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName(vip.getRealsIp().get(0).getName())).thenReturn(realEquipment);

        Ipv4 equipIpv4 = new Ipv4();
        equipIpv4.setId(realEquipId);
        equipIpv4.setOct1(Integer.valueOf(realIp.split("\\.")[0]));
        equipIpv4.setOct2(Integer.valueOf(realIp.split("\\.")[1]));
        equipIpv4.setOct3(Integer.valueOf(realIp.split("\\.")[2]));
        equipIpv4.setOct4(Integer.valueOf(realIp.split("\\.")[3]));
        Ip equipIp = (Ip) equipIpv4;
        when(_resource._globoNetworkApi.getIpAPI().findIpsByEquipment(realEquipmentId)).thenReturn(Arrays.asList(equipIp));

        // Vip after updating
        Vip vip2 = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip).thenReturn(vip2);

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).removeReal(vip.getId(), vip.getRealsIp().get(0).getIpId(), realEquipmentId, Integer.valueOf(vip.getServicePorts().get(0).split(":")[0]), Integer.valueOf(realPort));

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

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setVipId(vip.getId());
        cmd.setHost(vip.getHost());
        cmd.setIpv4(vip.getIps().get(0));
        cmd.setVipEnvironmentId(vipEnvironmentId);
        cmd.setPorts(vip.getServicePorts());
        cmd.setBusinessArea(vip.getBusinessArea());
        cmd.setServiceName(vip.getServiceName());
        cmd.setMethodBal("leastconn");
        cmd.setRuleState(FirewallRule.State.Add);

        LbStickinessPolicy persistencePolicy = new LbStickinessPolicy("Cookie", null);
        cmd.setPersistencePolicy(persistencePolicy);

        List<GloboNetworkVipResponse.Real> realList = new ArrayList<GloboNetworkVipResponse.Real>();
        for (RealIP vipReal : vip.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(vipReal.getVipPort() + ":" + vipReal.getRealPort()));
            real.setRevoked(true);
            real.setEnvironmentId(realEnvironmentId);
            realList.add(real);
        }
        cmd.setRealList(realList);

        Equipment realEquipment = new Equipment();
        realEquipment.setId(realEquipmentId);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName(vip.getRealsIp().get(0).getName())).thenReturn(realEquipment);

        Ipv4 equipIpv4 = new Ipv4();
        equipIpv4.setId(realEquipId);
        equipIpv4.setOct1(Integer.valueOf(realIp.split("\\.")[0]));
        equipIpv4.setOct2(Integer.valueOf(realIp.split("\\.")[1]));
        equipIpv4.setOct3(Integer.valueOf(realIp.split("\\.")[2]));
        equipIpv4.setOct4(Integer.valueOf(realIp.split("\\.")[3]));
        Ip equipIp = (Ip) equipIpv4;
        when(_resource._globoNetworkApi.getIpAPI().findIpsByEquipment(realEquipmentId)).thenReturn(Arrays.asList(equipIp));

        // Vip after updating
        Vip vip2 = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);
        vip2.setPersistence(persistenceMethod);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip).thenReturn(vip2);

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
        Long expectedHealthcheckId = 2L;

        Vip vip = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);

        when(_resource._globoNetworkApi.getVipAPI().getByIp(vip.getIps().get(0))).thenReturn(Arrays.asList(vip));

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setVipId(vip.getId());
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

        List<GloboNetworkVipResponse.Real> realList = new ArrayList<GloboNetworkVipResponse.Real>();
        for (RealIP vipReal : vip.getRealsIp()) {
            GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
            real.setIp(vipReal.getRealIp());
            real.setVmName(vipReal.getName());
            real.setPorts(Arrays.asList(vipReal.getVipPort() + ":" + vipReal.getRealPort()));
            real.setRevoked(true);
            real.setEnvironmentId(realEnvironmentId);
            realList.add(real);
        }
        cmd.setRealList(realList);

        Equipment realEquipment = new Equipment();
        realEquipment.setId(realEquipmentId);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName(vip.getRealsIp().get(0).getName())).thenReturn(realEquipment);

        Ipv4 equipIpv4 = new Ipv4();
        equipIpv4.setId(realEquipId);
        equipIpv4.setOct1(Integer.valueOf(realIp.split("\\.")[0]));
        equipIpv4.setOct2(Integer.valueOf(realIp.split("\\.")[1]));
        equipIpv4.setOct3(Integer.valueOf(realIp.split("\\.")[2]));
        equipIpv4.setOct4(Integer.valueOf(realIp.split("\\.")[3]));
        Ip equipIp = (Ip) equipIpv4;
        when(_resource._globoNetworkApi.getIpAPI().findIpsByEquipment(realEquipmentId)).thenReturn(Arrays.asList(equipIp));

        // Vip after updating
        Vip vip2 = buildFakeVipValidatedAndCreated(vipEnvironmentId, realEnvironmentId, vipIpId, realIp);
        vip2.setHealthcheck(healthcheck);
        vip2.setHealthcheckType(healthcheckType);

        when(_resource._globoNetworkApi.getVipAPI().getById(vip2.getId())).thenReturn(vip2);

        Answer answer = _resource.execute(cmd);

        verify(_resource._globoNetworkApi.getVipAPI()).alterHealthcheck(vip.getId(), healthcheckType, _resource.buildHealthcheckString(healthcheck, vip.getHost()),
                expectedHealthcheckId);

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

    @Test
    public void testAddAndEnableRealWithOnePortMapping() throws GloboNetworkException {
        Vip vip = this.buildFakeVipValidatedAndCreated(1L, 1L, 1L);
        vip.setServicePorts(Arrays.asList(new String[] { "80:8080" }));

        Ipv4 ip = createIp();
        Equipment equipment = createEquipment();
        VipAPI vipApiMock = setupAddOrRemoveRealMocks(ip, equipment);

        _resource.addAndEnableReal(vip, "equipment", "10.170.10.2", vip.getServicePorts());

        verify(vipApiMock, times(1)).addReal(vip.getId(), ip.getId(), equipment.getId(), 80, 8080);
    }

    @Test
    public void testAddAndEnableRealWithMoreThanOnePortMapping() throws GloboNetworkException {
        Vip vip = this.buildFakeVipValidatedAndCreated(1L, 1L, 1L);
        vip.setServicePorts(Arrays.asList(new String[] { "80:8080", "443:8443" }));

        Ipv4 ip = createIp();
        Equipment equipment = createEquipment();
        VipAPI vipApiMock = setupAddOrRemoveRealMocks(ip, equipment);

        _resource.addAndEnableReal(vip, "equipment", "10.170.10.2", vip.getServicePorts());

        verify(vipApiMock, times(1)).addReal(vip.getId(), ip.getId(), equipment.getId(), 80, 8080);
        verify(vipApiMock, times(1)).addReal(vip.getId(), ip.getId(), equipment.getId(), 443, 8443);
    }

    @Test
    public void testRemoveRealWithOnePortMapping() throws GloboNetworkException {
        Vip vip = this.buildFakeVipValidatedAndCreated(1L, 1L, 1L, "10.170.10.2");
        vip.setServicePorts(Arrays.asList(new String[] { "80:8080" }));

        Ipv4 ip = createIp();
        ip.setId(vip.getRealsIp().get(0).getIpId());
        Equipment equipment = createEquipment();
        VipAPI vipApiMock = setupAddOrRemoveRealMocks(ip, equipment);

        _resource.removeReal(vip, "equipment", "10.170.10.2", vip.getServicePorts());
        verify(vipApiMock, times(1)).removeReal(vip.getId(), ip.getId(), equipment.getId(), 80, 8080);
    }

    @Test
    public void testRemoveRealWithMoreThanOnePortMapping() throws GloboNetworkException {
        Vip vip = this.buildFakeVipValidatedAndCreated(1L, 1L, 1L, "10.170.10.2");
        vip.setServicePorts(Arrays.asList(new String[] { "80:8080", "443:8443" }));

        Ipv4 ip = createIp();
        ip.setId(vip.getRealsIp().get(0).getIpId());
        Equipment equipment = createEquipment();
        VipAPI vipApiMock = setupAddOrRemoveRealMocks(ip, equipment);

        _resource.removeReal(vip, "equipment", "10.170.10.2", vip.getServicePorts());
        verify(vipApiMock, times(1)).removeReal(vip.getId(), ip.getId(), equipment.getId(), 80, 8080);
        verify(vipApiMock, times(1)).removeReal(vip.getId(), ip.getId(), equipment.getId(), 443, 8443);
    }

    @Test
    public void testCreateVipResponseGivenVipWithOnePortMapping() throws GloboNetworkException {
        Vip vip = this.buildFakeVipValidatedAndCreated(1L, 1L, 1L, "10.170.10.2");
        vip.setServicePorts(Arrays.asList(new String[] { "80:8080" }));

        GloboNetworkVipResponse answer = (GloboNetworkVipResponse) _resource.createVipResponse(vip, new AddOrRemoveVipInGloboNetworkCommand());

        assertTrue(answer.getResult());
        assertEquals(1, answer.getReals().get(0).getPorts().size());
    }

    @Test
    public void testCreateVipResponseGivenVipWithMoreThanOnePortMapping() throws GloboNetworkException {
        Vip vip = this.buildFakeVipValidatedAndCreated(1L, 1L, 1L, "10.170.10.2");
        vip.setServicePorts(Arrays.asList(new String[] { "80:8080", "43:8443" }));

        GloboNetworkVipResponse answer = (GloboNetworkVipResponse) _resource.createVipResponse(vip, new AddOrRemoveVipInGloboNetworkCommand());

        assertTrue(answer.getResult());
        assertEquals(2, answer.getReals().get(0).getPorts().size());
    }



    private Pool mockPoolSave(Pool pool, RealIP realIp, boolean nullInList) throws GloboNetworkException {

        List<Integer> realsPriorities = new ArrayList<Integer>();
        realsPriorities.add(0); // ok

        List<String> equipNames = new ArrayList<String>();
        equipNames.add("vm-101_eq_name");

        List<Long> equipIds = new ArrayList<Long>();
        equipIds.add(123l);

        List<Long> idPoolMembers = new ArrayList<Long>();
        idPoolMembers.add(0l);
        List<Long> realsWeights = new ArrayList<Long>();
        realsWeights.add(0l);

        List<RealIP> realsIpList = new ArrayList<RealIP>();

        realsIpList.add(!nullInList ? realIp : null );

        List<Integer> realPorts = new ArrayList<Integer>();
        realPorts.add(realIp.getRealPort());



        when(_resource._globoNetworkApi.getPoolAPI().save(eq((Long) null),
                anyString(),
                eq(realIp.getRealPort()),
                eq(121l),
                eq("least-conn"),
                eq("TCP"),
                eq((String) null),
                eq(""),
                eq(0),
                eq(realsIpList),
                eq(equipNames),
                eq(equipIds),
                eq(realsPriorities),
                eq(realsWeights),
                eq(realPorts),
                eq(idPoolMembers),
                eq((String) null),
                eq((String) null)
        )).thenReturn(pool);

        return pool;
    }


    protected VipAPI setupAddOrRemoveRealMocks(Ipv4 ip, Equipment equipment) throws GloboNetworkException {
        EquipmentAPI equipmentAPIMock = mock(EquipmentAPI.class);
        when(equipmentAPIMock.listByName("equipment")).thenReturn(equipment);

        IpAPI ipApiMock = mock(IpAPI.class);
        when(ipApiMock.findIpsByEquipment(equipment.getId())).thenReturn(Arrays.asList(new Ip[]{ip}));

        VipAPI vipApiMock = mock(VipAPI.class);

        when(_resource._globoNetworkApi.getEquipmentAPI()).thenReturn(equipmentAPIMock);
        when(_resource._globoNetworkApi.getIpAPI()).thenReturn(ipApiMock);
        when(_resource._globoNetworkApi.getVipAPI()).thenReturn(vipApiMock);
        return vipApiMock;
    }

    private long getNewIpID() {
        return ++s_ipSequence;
    }

    private Vip buildFakeVipValidatedAndCreated(Long vipEnvironment, Long realEnvironment, Long vipIpId, String... reals) throws GloboNetworkException {
        List<String> ports = Arrays.asList(new String[] { "80:8080" });
        Vip vip = this.buildFakeVip(vipEnvironment, realEnvironment, vipIpId, ports, reals);
        vip.setCreated(true);
        return vip;
    }


    private Vip buildFakeVipWithPools(Long vipEnvironmentId, Long realEnvironmentId, Long vipIpId, List<String> ports, Long poolId) throws GloboNetworkException {
        Long vipId = 987L;
        String vipIpStr = "192.168.1.15";
        String vipHost = "vip.domain.com";
        String vipBusinessArea = "vipbusiness";
        String vipServiceName = "vipservice";
        String vipMethodBal = "least-conn";
        String vipFinality = "BACKEND";
        String vipClient = "CLIENT";
        String vipEnvironmentName = "TESTAPI";
        String vipCache = "(nenhum)";

        vipIp  = new Ipv4();
        vipIp.setId(vipIpId);
        vipIp.setOct1(Integer.valueOf(vipIpStr.split("\\.")[0]));
        vipIp.setOct2(Integer.valueOf(vipIpStr.split("\\.")[1]));
        vipIp.setOct3(Integer.valueOf(vipIpStr.split("\\.")[2]));
        vipIp.setOct4(Integer.valueOf(vipIpStr.split("\\.")[3]));
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp(vipIpStr, vipEnvironmentId, false)).thenReturn(vipIp);

        VipEnvironment environmentVip = new VipEnvironment();
        environmentVip.setId(vipEnvironmentId);
        environmentVip.setFinality(vipFinality);
        environmentVip.setClient(vipClient);
        environmentVip.setEnvironmentName(vipEnvironmentName);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(vipEnvironmentId, null, null, null)).thenReturn(environmentVip);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(null, vipFinality, vipClient, vipEnvironmentName)).thenReturn(environmentVip);

        Vip vip = new VipJson();
        vip.setId(vipId);
        vip.setIps(Arrays.asList(vipIpStr));
        vip.setServicePorts(ports);
        vip.setHost(vipHost);
        vip.setBusinessArea(vipBusinessArea);
        vip.setMethod(vipMethodBal);
        vip.setServiceName(vipServiceName);
        vip.setFinality(vipFinality);
        vip.setClient(vipClient);
        vip.setEnvironment(vipEnvironmentName);
        vip.setCache(vipCache);
        vip.setCreated(false);

        List<Pool> pools = new ArrayList<Pool>();

        Pool pool = new Pool();
        pool.setId(poolId);
        pool.setLbMethod(vipMethodBal);
        pool.setPoolCreated(false);
        pool.setDefaultPort(80);
        pool.setIdentifier("POOL_1");
        pool.setDefaultLimit(0);
        pool.setEnvironment(120l);
        pools.add(pool);

        return vip;
    }

    private Vip buildFakeVip(Long vipEnvironment, Long realEnvironment, Long vipIpId, List<String> servicePorts, String... reals) throws GloboNetworkException {
        Long vipId = 987L;
        String vipIpStr = "192.168.1.15";
        String vipHost = "vip.domain.com";
        String vipBusinessArea = "vipbusiness";
        String vipServiceName = "vipservice";
        String vipMethodBal = "least-conn";
        String vipFinality = "BACKEND";
        String vipClient = "CLIENT";
        String vipEnvironmentName = "TESTAPI";
        String vipCache = "(nenhum)";

        vipIp  = new Ipv4();
        vipIp.setId(vipIpId);
        vipIp.setOct1(Integer.valueOf(vipIpStr.split("\\.")[0]));
        vipIp.setOct2(Integer.valueOf(vipIpStr.split("\\.")[1]));
        vipIp.setOct3(Integer.valueOf(vipIpStr.split("\\.")[2]));
        vipIp.setOct4(Integer.valueOf(vipIpStr.split("\\.")[3]));
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp(vipIpStr, vipEnvironment, false)).thenReturn(vipIp);

        VipEnvironment environmentVip = new VipEnvironment();
        environmentVip.setId(vipEnvironment);
        environmentVip.setFinality(vipFinality);
        environmentVip.setClient(vipClient);
        environmentVip.setEnvironmentName(vipEnvironmentName);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(vipEnvironment, null, null, null)).thenReturn(environmentVip);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(null, vipFinality, vipClient, vipEnvironmentName)).thenReturn(environmentVip);

        Vip vip = new VipXml();
        vip.setId(vipId);
        vip.setIps(Arrays.asList(vipIpStr));
        vip.setServicePorts(servicePorts);
        vip.setHost(vipHost);
        vip.setBusinessArea(vipBusinessArea);
        vip.setMethod(vipMethodBal);
        vip.setServiceName(vipServiceName);
        vip.setFinality(vipFinality);
        vip.setClient(vipClient);
        vip.setEnvironment(vipEnvironmentName);
        vip.setCache(vipCache);
        vip.setCreated(false);

        List<RealIP> realIpList = new ArrayList<RealIP>();
        for (String realAddr : reals) {
            Ip ip = new Ipv4();
            ip.setId(getNewIpID());
            when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment(realAddr, realEnvironment, false)).thenReturn(ip);

            RealIP realIp = new RealIP();
            realIp.setIpId(ip.getId());
            realIp.setName("vm-" + ip.getId());
            realIp.setRealIp(realAddr);
            realIp.setVipPort(Integer.valueOf(servicePorts.get(0).split(":")[0]));
            realIp.setRealPort(Integer.valueOf(servicePorts.get(0).split(":")[1]));
            realIpList.add(realIp);
        }
        vip.setRealsIp(realIpList);

        return vip;
    }
    protected Ipv4 createIp() {
        Ipv4 ip = new Ipv4();
        ip.setOct1(10);
        ip.setOct2(170);
        ip.setOct3(10);
        ip.setOct4(2);
        return ip;
    }

    protected Equipment createEquipment() {
        Equipment equipment = new Equipment();
        equipment.setId(1L);
        return equipment;
    }
}
