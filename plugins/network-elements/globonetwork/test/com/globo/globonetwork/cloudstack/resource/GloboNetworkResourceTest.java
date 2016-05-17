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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cloud.exception.InvalidParameterValueException;
import com.globo.globonetwork.client.api.ExpectHealthcheckAPI;
import com.globo.globonetwork.client.api.GloboNetworkAPI;
import com.globo.globonetwork.client.api.NetworkAPI;
import com.globo.globonetwork.client.api.NetworkJsonAPI;
import com.globo.globonetwork.client.api.PoolAPI;
import com.globo.globonetwork.client.model.IPv4Network;
import com.globo.globonetwork.client.model.Network;
import com.globo.globonetwork.client.model.Pool;
import com.globo.globonetwork.client.model.PoolOption;
import com.globo.globonetwork.client.model.VipJson;
import com.globo.globonetwork.client.model.VipPoolMap;
import com.globo.globonetwork.client.model.VipXml;
import com.globo.globonetwork.client.model.Vlan;

import com.globo.globonetwork.client.model.healthcheck.ExpectHealthcheck;
import com.globo.globonetwork.client.model.pool.PoolV3;
import com.globo.globonetwork.cloudstack.commands.ApplyVipInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.ListExpectedHealthchecksCommand;
import com.globo.globonetwork.cloudstack.commands.ListPoolLBCommand;
import com.globo.globonetwork.cloudstack.commands.RemoveVipFromGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.commands.UpdatePoolCommand;
import com.globo.globonetwork.cloudstack.manager.HealthCheckHelper;
import com.globo.globonetwork.cloudstack.response.GloboNetworkExpectHealthcheckResponse;
import com.globo.globonetwork.cloudstack.response.GloboNetworkPoolResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.ConfigurationException;

import com.globo.globonetwork.cloudstack.commands.ListPoolOptionsCommand;
import com.globo.globonetwork.cloudstack.response.GloboNetworkPoolOptionResponse;
import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.Answer;
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
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;

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
        when(_resource._globoNetworkApi.getNetworkJsonAPI()).thenReturn(mock(NetworkJsonAPI.class));
        when(_resource._globoNetworkApi.getPoolAPI()).thenReturn(mock(PoolAPI.class));
        when(_resource._globoNetworkApi.getExpectHealthcheckAPI()).thenReturn(mock(ExpectHealthcheckAPI.class));
        when(_resource._globoNetworkApi.getVipAPI()).thenReturn(mock(VipAPI.class));
    }

    static long s_ipSequence = 100;

    @Test
    public void testTryToRemoveNullVIP(){
        RemoveVipFromGloboNetworkCommand cmd = new RemoveVipFromGloboNetworkCommand();
        Answer answer = _resource.removeVipFromGloboNetwork(cmd, null, true);
        assertTrue(answer.getResult());
        assertEquals("Vip request was previously removed from GloboNetwork", answer.getDetails());
    }

    @Test
    public void testTryToRemoveAlreadyRemovedVIP() throws GloboNetworkException {
        Vip vip = new VipXml();
        vip.setId(1L);
        when(_resource._globoNetworkApi.getVipAPI().getById(vip.getId())).thenReturn(null);

        Answer answer = _resource.removeVipFromGloboNetwork(new RemoveVipFromGloboNetworkCommand(), vip.getId(), true);
        assertTrue(answer.getResult());
        assertEquals("Vip request 1 was previously removed from GloboNetwork", answer.getDetails());
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).getById(vip.getId());
    }

    @Test
    public void testTryToRemoveCreatedVIP() throws GloboNetworkException {
        Vip vip = new VipXml();
        vip.setId(1L);
        vip.setCreated(true);

        when(_resource._globoNetworkApi.getVipAPI().getById(1L)).thenReturn(vip);

        Answer answer = _resource.removeVipFromGloboNetwork(new RemoveVipFromGloboNetworkCommand(), vip.getId(), true);
        assertTrue(answer.getResult());
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).getById(vip.getId());
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).removeScriptVip(vip.getId());
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).removeVip(vip.getId(), true);
    }

    @Test
    public void testTryToRemoveNotCreatedCreatedVIP() throws GloboNetworkException {
        Vip vip = new VipXml();
        vip.setId(1L);
        vip.setCreated(false);

        when(_resource._globoNetworkApi.getVipAPI().getById(1L)).thenReturn(vip);

        Answer answer = _resource.removeVipFromGloboNetwork(new RemoveVipFromGloboNetworkCommand(), vip.getId(), true);
        assertTrue(answer.getResult());
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).getById(vip.getId());
        verify(_resource._globoNetworkApi.getVipAPI(), times(0)).removeScriptVip(vip.getId());
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).removeVip(vip.getId(), true);
    }

    @Test
    public void testTryToRemoveVipWithNetworkApiError() throws GloboNetworkException {
        Vip vip = new VipXml();
        vip.setId(1L);

        when(_resource._globoNetworkApi.getVipAPI().getById(1L)).thenThrow(GloboNetworkException.class);

        Answer answer = _resource.removeVipFromGloboNetwork(new RemoveVipFromGloboNetworkCommand(), vip.getId(), true);
        assertFalse(answer.getResult());
    }

    @Test
    public void testGetBalancingAlgorithmGivenRoundRobin(){
        assertEquals(GloboNetworkResource.LbAlgorithm.RoundRobin, _resource.getBalancingAlgorithm("roundrobin"));
    }

    @Test
    public void testGetBalancingAlgorithmGivenLeastConn(){
        assertEquals(GloboNetworkResource.LbAlgorithm.LeastConn, _resource.getBalancingAlgorithm("leastconn"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetBalancingAlgorithmGivenInvalid(){
        _resource.getBalancingAlgorithm("random");
    }

    @Test
    public void testGetPersistenceMethodNone(){
        assertEquals("(nenhum)", _resource.getPersistenceMethod(new LbStickinessPolicy("None", null)));
        assertEquals("(nenhum)", _resource.getPersistenceMethod(null));
    }

    @Test
    public void testGetPersistenceMethodGivenCookie(){
        assertEquals("cookie", _resource.getPersistenceMethod(new LbStickinessPolicy("Cookie", null)));
    }

    @Test
    public void testGetPersistenceMethodGivenSourceIp(){
        assertEquals("source-ip", _resource.getPersistenceMethod(new LbStickinessPolicy("Source-ip", null)));
    }

    @Test
    public void testGetPersistenceMethodGivenSourceIpWithPersistence(){
        assertEquals("source-ip com persist. entre portas", _resource.getPersistenceMethod(new LbStickinessPolicy("Source-ip with persistence between ports", null)));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetPersistenceMethodGivenInvalidPersistence(){
        _resource.getPersistenceMethod(new LbStickinessPolicy("jsession", null));
    }

    @Test
    public void testGetVipById() throws GloboNetworkException {
        when(_resource._globoNetworkApi.getVipAPI().getByPk(1L)).thenReturn(new VipJson());
        assertNotNull(_resource.getVipById(1L));
    }

    @Test
    public void testGetVipByIdGivenNullId() throws GloboNetworkException {
        assertNull(_resource.getVipById(null));
    }

    @Test
    public void testCreateVip() throws GloboNetworkException {
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        cmd.setBusinessArea("businessarea");
        cmd.setServiceName("servicename");
        String host = "vip";
        VipEnvironment vipEnvironment = new VipEnvironment();
        vipEnvironment.setId(1L);
        vipEnvironment.setClient("client");
        vipEnvironment.setFinality("finality");
        vipEnvironment.setEnvironmentName("environment");
        Ipv4 ip = new Ipv4();
        ip.setId(1L);

        when(_resource._globoNetworkApi.getVipAPI().save(
            1L, null, "finality", "client", "environment", "(nenhum)",
            "(nenhum)", 5, "vip", "businessarea", "servicename", null, null, null, null)).
        thenReturn(new VipJson());

        Vip vip = _resource.createVip(cmd, host, vipEnvironment, ip, null);

        assertNotNull(vip);
        verify(_resource._globoNetworkApi.getVipAPI()).save(
            1L, null, "finality", "client", "environment", "(nenhum)",
            "(nenhum)", 5, "vip", "businessarea", "servicename", null, null, null, null
        );
    }

    @Test
    public void testCreateOnEquipment() throws GloboNetworkException {
        VipXml vip = new VipXml();
        vip.setCreated(false);
        _resource.createOnEquipment(vip);
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).create(vip.getId());
    }

    @Test
    public void testCreateOnEquipmentAnAlreadyCreatedVip() throws GloboNetworkException {
        VipXml vip = new VipXml();
        vip.setCreated(true);
        _resource.createOnEquipment(vip);
        verify(_resource._globoNetworkApi.getVipAPI(), never()).create(vip.getId());
    }

    @Test
    public void testValidate() throws GloboNetworkException {
        when(_resource._globoNetworkApi.getVipAPI().getById(anyLong())).thenReturn(new VipXml());
        assertNotNull(_resource.validate(new VipJson(), new Ipv4()));
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).getById(anyLong());
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).validate(anyLong());
    }

    @Test
    public void testFindPoolByPort(){
        Pool pool = new Pool();
        pool.setDefaultPort(80);
        VipJson vip = new VipJson();
        vip.setPools(Arrays.asList(pool));

        assertNotNull(_resource.findPoolByPortInVip(80, vip));
        assertNull(_resource.findPoolByPortInVip(81, vip));
    }

    @Test
    public void testFindPoolByPorGivenNullVip(){
        assertNull(_resource.findPoolByPortInVip(80, null));
    }


    private PoolV3 mockPoolSave(Long poolId, Long idReturned, Boolean hasPoolMember, Integer port, String ip, String healthCheckType, String healthCheck, String expectedHealthCheck, int maxConn,  String serviceDAction) throws GloboNetworkException {
        PoolV3 expectedPool = new PoolV3();
        expectedPool.setId(poolId);
        expectedPool.setIdentifier(_resource.buildPoolName("vip.domain.com", port));
        expectedPool.setLbMethod("round-robin");
        expectedPool.setMaxconn(maxConn);
        expectedPool.setDefaultPort(port);
        expectedPool.setEnvironment(120l);

        PoolV3.Healthcheck healthcheck = expectedPool.getHealthcheck();
        healthcheck.setHealthcheck(healthCheckType, healthCheck, expectedHealthCheck);
        healthcheck.setDestination("*:*");

        PoolV3.ServiceDownAction serviceDownAction = new PoolV3.ServiceDownAction();
        serviceDownAction.setName(serviceDAction);
        expectedPool.setServiceDownAction(serviceDownAction);

        if (hasPoolMember) {
            PoolV3.PoolMember poolMember = new PoolV3.PoolMember();
            poolMember.setPortReal(port);
            poolMember.setWeight(0);
            poolMember.setPriority(0);
            poolMember.setMemberStatus(7);
            poolMember.setEquipmentId(1L);
            poolMember.setEquipmentName("vm-01");

            PoolV3.Ip ipp = new PoolV3.Ip();
            ipp.setIpFormated(ip);
            ipp.setId(1l);
            poolMember.setIp(ipp);
            expectedPool.getPoolMembers().add(poolMember);
        }

        PoolV3 newPool = new PoolV3();
        newPool.setId(idReturned);
        when(_resource._globoNetworkApi.getPoolAPI().save(expectedPool)
        ).thenReturn(newPool);

        return expectedPool;
    }

    @Test
    public void testGetVipInfosGivenInvalidVlan() throws GloboNetworkException {
        Ipv4 ip = new Ipv4();
        ip.setNetworkId(1L);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(123l, null, null, null)).thenReturn(new VipEnvironment());
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp("10.0.0.1", 123l, false)).thenReturn(ip);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(1L, false)).thenReturn(new IPv4Network());
        when(_resource._globoNetworkApi.getVlanAPI().getById(999L)).thenReturn(new Vlan());
        try{
            _resource.getVipInfos(123l, "10.0.0.1");
        }catch(InvalidParameterValueException e){
            assertEquals("Vlan " + null + " was not found in GloboNetwork", e.getMessage());
        }
    }

    @Test
    public void testGetVipInfosGivenInvalidNetwork() throws GloboNetworkException {
        Ipv4 ip = new Ipv4();
        ip.setNetworkId(1L);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(123l, null, null, null)).thenReturn(new VipEnvironment());
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp("10.0.0.1", 123l, false)).thenReturn(ip);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(1L, false)).thenReturn(new IPv4Network());
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(new Vlan());
        try{
            _resource.getVipInfos(123l, "10.0.0.1");
        }catch(InvalidParameterValueException e){
            assertEquals("Network " + null + " was not found in GloboNetwork", e.getMessage());
        }
    }

    @Test
    public void testCreateNewVIPFromScratch() throws Exception {
        VipJson vipToBeCreated = buildFakeVipValidatedAndCreated(120L, 546L, 345L);
        vipToBeCreated.setCreated(false);

        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        cmd.setVipId(null);
        cmd.setHost(vipToBeCreated.getHost());
        cmd.setIpv4(vipToBeCreated.getIps().get(0));
        cmd.setVipEnvironmentId(120L);
        cmd.setPorts(Arrays.asList("80:8080"));
        cmd.setBusinessArea(vipToBeCreated.getBusinessArea());
        cmd.setServiceName(vipToBeCreated.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setRuleState(FirewallRule.State.Add);

        HealthCheckHelper build = HealthCheckHelper.build("vip.domain.com", "TCP", "", null);
        cmd.setHealthcheckType(build.getHealthCheckType());
        cmd.setExpectedHealthcheck(build.getExpectedHealthCheck());
        cmd.setHealthcheck(build.getHealthCheck());

        GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
        String ipformatted = "10.0.0.1";
        real.setIp(ipformatted);
        real.setVmName("vm-01");
        real.setPorts(Arrays.asList("80:8080"));
        real.setRevoked(false);
        real.setEnvironmentId(120L);
        cmd.setRealList(Arrays.asList(real));

        Equipment realEquipment = new Equipment();
        realEquipment.setId(1L);
        realEquipment.setName("vm-01");

        Ipv4 equipIpv4 = new Ipv4();
        equipIpv4.setId(1L);
        String[] ipSplitted = ipformatted.split("\\.");
        equipIpv4.setOct1(Integer.valueOf(ipSplitted[0]));
        equipIpv4.setOct2(Integer.valueOf(ipSplitted[1]));
        equipIpv4.setOct3(Integer.valueOf(ipSplitted[2]));
        equipIpv4.setOct4(Integer.valueOf(ipSplitted[3]));

        when(_resource._globoNetworkApi.getVipAPI().getByPk(vipToBeCreated.getId())).thenReturn(vipToBeCreated);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName("vm-01")).thenReturn(realEquipment);
        when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment(equipIpv4.getIpString(), real.getEnvironmentId(), false)).thenReturn(equipIpv4);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(vipIp.getNetworkId(), false)).thenReturn(new IPv4Network());
        Vlan vlan = new Vlan();
        vlan.setEnvironment(120l);
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(vlan);


        PoolV3 pool2 = mockPoolSave(null, 456l, true, 8080, "10.0.0.1", "TCP",
                build.getHealthCheck(), build.getExpectedHealthCheck(), 0, cmd.getServiceDownAction());

        when(_resource._globoNetworkApi.getVipAPI().getById(vipToBeCreated.getId())).thenReturn(fromVipJsonToVipXml(vipToBeCreated));

        when(_resource._globoNetworkApi.getVipAPI().save(
            eq(345L), isNull(Long.class), eq(vipToBeCreated.getFinality()), eq(vipToBeCreated.getClient()), eq(vipToBeCreated.getEnvironment()),
            eq(vipToBeCreated.getCache()), eq("(nenhum)"), eq(vipToBeCreated.getTimeout()), eq(vipToBeCreated.getHost()),
            eq(vipToBeCreated.getBusinessArea()), eq(vipToBeCreated.getServiceName()), isNull(String.class), anyList(), isNull(Long.class), isNull(Long.class))
        ).thenReturn(vipToBeCreated);

        Answer answer = _resource.execute(cmd);

        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);
        verify(_resource._globoNetworkApi.getPoolAPI(), times(1)).save(pool2);
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).save(
            eq(345L), isNull(Long.class), eq(vipToBeCreated.getFinality()), eq(vipToBeCreated.getClient()), eq(vipToBeCreated.getEnvironment()),
            eq(vipToBeCreated.getCache()), eq("(nenhum)"), eq(vipToBeCreated.getTimeout()), eq(vipToBeCreated.getHost()),
            eq(vipToBeCreated.getBusinessArea()), eq(vipToBeCreated.getServiceName()), isNull(String.class), anyList(), isNull(Long.class), isNull(Long.class)
        );
    }

    @Test
    public void testCreateNewVIPWithMultiplePortsAndNoReal() throws Exception {
        VipJson vipToBeCreated = buildFakeVipValidatedAndCreated(120L, 546L, 345L);
        vipToBeCreated.setCreated(false);

        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();

        HealthCheckHelper build = HealthCheckHelper.build("vip.domain.com", "TCP", "", null);
        cmd.setHealthcheckType(build.getHealthCheckType());
        cmd.setExpectedHealthcheck(build.getExpectedHealthCheck());
        cmd.setHealthcheck(build.getHealthCheck());

        cmd.setVipId(null);
        cmd.setHost(vipToBeCreated.getHost());
        cmd.setIpv4(vipToBeCreated.getIps().get(0));
        cmd.setVipEnvironmentId(120L);
        cmd.setPorts(Arrays.asList("80:8080", "443:8443"));
        cmd.setBusinessArea(vipToBeCreated.getBusinessArea());
        cmd.setServiceName(vipToBeCreated.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setRealList(new ArrayList<GloboNetworkVipResponse.Real>());

        when(_resource._globoNetworkApi.getVipAPI().getByPk(vipToBeCreated.getId())).thenReturn(vipToBeCreated);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(vipIp.getNetworkId(), false)).thenReturn(new IPv4Network());
        Vlan vlan = new Vlan();
        vlan.setEnvironment(120l);
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(vlan);

        PoolV3 pool1 = mockPoolSave(null, 123l, false, 8080, null, "TCP",
                                    build.getHealthCheck(), build.getExpectedHealthCheck(), 0, cmd.getServiceDownAction());

        PoolV3 pool2 = mockPoolSave(null, 456l, false, 8443, null, "TCP",
                build.getHealthCheck(), build.getExpectedHealthCheck(), 0, cmd.getServiceDownAction());

        when(_resource._globoNetworkApi.getVipAPI().getById(vipToBeCreated.getId())).thenReturn(fromVipJsonToVipXml(vipToBeCreated));

        when(_resource._globoNetworkApi.getVipAPI().save(
            eq(345L), isNull(Long.class), eq(vipToBeCreated.getFinality()), eq(vipToBeCreated.getClient()), eq(vipToBeCreated.getEnvironment()),
            eq(vipToBeCreated.getCache()), eq("(nenhum)"), eq(vipToBeCreated.getTimeout()), eq(vipToBeCreated.getHost()),
            eq(vipToBeCreated.getBusinessArea()), eq(vipToBeCreated.getServiceName()), isNull(String.class), anyList(), isNull(Long.class), isNull(Long.class))
        ).thenReturn(vipToBeCreated);

        Answer answer = _resource.execute(cmd);

        assertTrue(answer.getResult());
        verify(_resource._globoNetworkApi.getPoolAPI(), times(1)).save(pool1);
        verify(_resource._globoNetworkApi.getPoolAPI(), times(1)).save(pool2);
    }

    @Test
    public void testUpdateCreatedVIPPersistence() throws Exception {
        VipJson createdVip = buildFakeVipValidatedAndCreated(123L, 546L, 345L);
        String persistenceCS = "Cookie";
        String persistenceNetApi = "cookie";

        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        HealthCheckHelper build = HealthCheckHelper.build("vip.domain.com", "TCP", "", null);
        cmd.setHealthcheck(build.getHealthCheck());
        cmd.setExpectedHealthcheck(build.getExpectedHealthCheck());
        cmd.setHealthcheckType(build.getHealthCheckType());

        cmd.setVipId(createdVip.getId());
        cmd.setHost(createdVip.getHost());
        cmd.setIpv4(createdVip.getIps().get(0));
        cmd.setVipEnvironmentId(123L);
        cmd.setPorts(Arrays.asList("80:8080", "443:8443"));
        cmd.setBusinessArea(createdVip.getBusinessArea());
        cmd.setServiceName(createdVip.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setPersistencePolicy(new LbStickinessPolicy(persistenceCS, null));
        cmd.setRealList(new ArrayList<GloboNetworkVipResponse.Real>());

        when(_resource._globoNetworkApi.getVipAPI().getByPk(createdVip.getId())).thenReturn(createdVip);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(vipIp.getNetworkId(), false)).thenReturn(new IPv4Network());
        Vlan vlan = new Vlan();
        vlan.setEnvironment(120l);
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(vlan);

        PoolV3 pool1 = mockPoolSave(80l, 80l, false, 8080, null, "TCP", "", "", 0, "none");
        PoolV3 pool2 = mockPoolSave(null, 443l, false, 8443, null, "TCP", "", "", 0, "none");

        when(_resource._globoNetworkApi.getPoolAPI().getById(80l)).thenReturn(pool1);
        when(_resource._globoNetworkApi.getPoolAPI().getById(443l)).thenReturn(pool2);

        when(_resource._globoNetworkApi.getVipAPI().getById(createdVip.getId())).thenReturn(fromVipJsonToVipXml(createdVip));

        Answer answer = _resource.execute(cmd);

        assertTrue(answer.getResult());
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).alterPersistence(createdVip.getId(), persistenceNetApi);
    }

    @Test
    public void testCreateVipResponseGivenVipWithOnePortMapping() throws GloboNetworkException {
        VipXml vip = createVipXML(Arrays.asList("8080:80"), new RealIP(1L, 80, "192.268.0.10", 8080));

        VipEnvironment vipEnvironment = new VipEnvironment();
        vipEnvironment.setId(1L);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(null, vip.getFinality(), vip.getClient(), vip.getEnvironment())).thenReturn(vipEnvironment);
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp(vip.getIps().get(0), vipEnvironment.getId(), false)).thenReturn(new Ipv4());

        GloboNetworkVipResponse answer = (GloboNetworkVipResponse) _resource.createVipResponse(vip, new ApplyVipInGloboNetworkCommand());

        assertTrue(answer.getResult());
        assertEquals(1, answer.getReals().size());
        assertEquals(1, answer.getReals().get(0).getPorts().size());
        assertEquals(answer.getId(), vip.getId());
        assertEquals(answer.getName(), vip.getHost());
        assertEquals(answer.getIp(), vip.getIps().get(0));
        assertEquals(answer.getLbEnvironmentId(), new Long(vip.getEnvironment()));
        assertEquals(answer.getCache(), vip.getCache());
        assertEquals(answer.getMethod(), vip.getMethod());
        assertEquals(answer.getPersistence(), vip.getPersistence());
        assertEquals(answer.getHealthcheckType(), vip.getHealthcheckType());
        assertEquals(answer.getHealthcheck(), vip.getHealthcheck());
        assertEquals(answer.getMaxConn(), vip.getMaxConn());
    }

    @Test
    public void testCreateVipResponseGivenVipWithMoreThanOnePortMapping() throws GloboNetworkException {
        VipXml vip = createVipXML(Arrays.asList("8080:80", "443: 8443"), new RealIP(1L, 80, "192.268.0.10", 8080));

        VipEnvironment vipEnvironment = new VipEnvironment();
        vipEnvironment.setId(1L);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(null, vip.getFinality(), vip.getClient(), vip.getEnvironment())).thenReturn(vipEnvironment);
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp(vip.getIps().get(0), vipEnvironment.getId(), false)).thenReturn(new Ipv4());

        GloboNetworkVipResponse answer = (GloboNetworkVipResponse) _resource.createVipResponse(vip, new ApplyVipInGloboNetworkCommand());

        assertTrue(answer.getResult());
        assertEquals(1, answer.getReals().size());
        assertEquals(2, answer.getReals().get(0).getPorts().size());
    }

    @Test
    public void testCreateVipResponseGivenVipWithMoreThanOneReal() throws GloboNetworkException {
        VipXml vip = createVipXML(Arrays.asList("8080:80", "443: 8443"), new RealIP(1L, 80, "192.268.0.10", 8080), new RealIP(2L, 80, "192.268.0.11", 8080));

        VipEnvironment vipEnvironment = new VipEnvironment();
        vipEnvironment.setId(1L);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(null, vip.getFinality(), vip.getClient(), vip.getEnvironment())).thenReturn(vipEnvironment);
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp(vip.getIps().get(0), vipEnvironment.getId(), false)).thenReturn(new Ipv4());

        GloboNetworkVipResponse answer = (GloboNetworkVipResponse) _resource.createVipResponse(vip, new ApplyVipInGloboNetworkCommand());

        assertTrue(answer.getResult());
        assertEquals(2, answer.getReals().size());
    }

    @Test
    public void testListPoolOptions() throws IOException, GloboNetworkException {
        List<PoolOption> options = Arrays.asList(new PoolOption(1L, "reset"));
        IPv4Network network = new IPv4Network();
        network.setVlanId(1L);
        Vlan vlan = new Vlan();
        vlan.setEnvironment(1L);
        when(_resource._globoNetworkApi.getNetworkJsonAPI().listVipNetworks(45L, false)).thenReturn(Arrays.<Network>asList(network));
        when(_resource._globoNetworkApi.getVlanAPI().getById(1L)).thenReturn(vlan);
        when(_resource._globoNetworkApi.getPoolAPI().listPoolOptions(1L, "ServiceDownAction")).thenReturn(options);
        GloboNetworkPoolOptionResponse answer = (GloboNetworkPoolOptionResponse) _resource.executeRequest(new ListPoolOptionsCommand(45L, "ServiceDownAction"));
        assertFalse(answer.getPoolOptions().isEmpty());
        assertEquals(new Long(1L), answer.getPoolOptions().get(0).getId());
    }

    @Test
    public void testListPoolOptionsGivenNoPoolsReturned() throws IOException, GloboNetworkException {
        IPv4Network network = new IPv4Network();
        network.setVlanId(1L);
        Vlan vlan = new Vlan();
        vlan.setEnvironment(1L);
        when(_resource._globoNetworkApi.getNetworkJsonAPI().listVipNetworks(45L, false)).thenReturn(Arrays.<Network>asList(network));
        when(_resource._globoNetworkApi.getVlanAPI().getById(1L)).thenReturn(vlan);
        when(_resource._globoNetworkApi.getPoolAPI().listPoolOptions(1L, "ServiceDownAction")).thenReturn(new ArrayList<PoolOption>());
        GloboNetworkPoolOptionResponse answer = (GloboNetworkPoolOptionResponse) _resource.executeRequest(new ListPoolOptionsCommand(45L, "ServiceDownAction"));
        assertTrue(answer.getPoolOptions().isEmpty());
    }

    @Test
    public void testListPoolOptionsGivenGloboNetworkException() throws IOException, GloboNetworkException {
        IPv4Network network = new IPv4Network();
        network.setVlanId(1L);
        Vlan vlan = new Vlan();
        vlan.setEnvironment(1L);
        when(_resource._globoNetworkApi.getNetworkJsonAPI().listVipNetworks(45L, false)).thenReturn(Arrays.<Network>asList(network));
        when(_resource._globoNetworkApi.getVlanAPI().getById(1L)).thenReturn(vlan);
        when(_resource._globoNetworkApi.getPoolAPI().listPoolOptions(1L, "ServiceDownAction")).thenThrow(new GloboNetworkException("Netapi failed"));
        Answer answer = _resource.executeRequest(new ListPoolOptionsCommand(45L, "ServiceDownAction"));
        assertFalse(answer.getResult());
        assertEquals("Netapi failed", answer.getDetails());
    }

    @Test
    public void testListPoolOptionsGivenIOException() throws IOException, GloboNetworkException {
        IPv4Network network = new IPv4Network();
        network.setVlanId(1L);
        Vlan vlan = new Vlan();
        vlan.setEnvironment(1L);
        when(_resource._globoNetworkApi.getNetworkJsonAPI().listVipNetworks(45L, false)).thenReturn(Arrays.<Network>asList(network));
        when(_resource._globoNetworkApi.getVlanAPI().getById(1L)).thenReturn(vlan);
        when(_resource._globoNetworkApi.getPoolAPI().listPoolOptions(1L, "ServiceDownAction")).thenThrow(new IOException());
        Answer answer = _resource.executeRequest(new ListPoolOptionsCommand(45L, "ServiceDownAction"));
        assertFalse(answer.getResult());
    }

    @Test
    public void testListPoolExecute() throws GloboNetworkException {

        List<Pool> poolsNetworkApi = new ArrayList<Pool>();
        Pool pool1 = new Pool();
        pool1.setId(33l);
        pool1.setIdentifier("my_pool");
        pool1.setLbMethod("leastcon");
        pool1.setDefaultPort(80);
        poolsNetworkApi.add(pool1);

        Pool pool2 = new Pool();
        pool2.setId(22l);
        pool2.setIdentifier("my_pool_2");
        pool2.setLbMethod("round");
        pool2.setDefaultPort(8091);
        poolsNetworkApi.add(pool1);

        when(_resource._globoNetworkApi.getPoolAPI().listAllByReqVip(123l)).thenReturn(poolsNetworkApi);

        ListPoolLBCommand cmd = new ListPoolLBCommand(123l);
        Answer answer = _resource.executeRequest((ListPoolLBCommand) cmd);

        List<GloboNetworkPoolResponse.Pool> pools = ((GloboNetworkPoolResponse)answer).getPools();

        assertEquals(2, pools.size());

        GloboNetworkPoolResponse.Pool pool = pools.get(0);
        assertEquals((Long)33l, pool.getId());
        assertEquals("my_pool", pool.getIdentifier());
        assertEquals("leastcon", pool.getLbMethod());
        assertEquals((Integer) 80, pool.getPort());


    }

    @Test
    public void testExecuteUpdatePool() throws GloboNetworkException {
        UpdatePoolCommand cmd = new UpdatePoolCommand(Arrays.asList(12l,13l), "HTTP", "GET /heal HTTP/1.0\\r\\nHost: vip.domain.com\\r\\n\\r\\n", "OK", 5, "vip.domain.com");

        PoolV3 pool1 = mockPool(12l, "ACS_POOL_vip.domain.com_8080", 8080, "least", "http", "/heal.html", "OK", "*:*", 10);
        PoolV3 pool2 = mockPool(13l, "ACS_POOL_vip.domain.com_8443", 8443, "least", "http", "/heal.html", "OK", "*:*", 10);
        List<PoolV3> poolsResponse = new ArrayList<PoolV3>();
        poolsResponse.add(pool1);
        poolsResponse.add(pool2);

        when(_resource._globoNetworkApi.getPoolAPI().getByIdsV3(Arrays.asList(12l, 13l))).thenReturn(poolsResponse);

        mockPoolSave(12l, 12l, true, 8080, "10.0.0.1", "HTTP", "GET /heal HTTP/1.0\\r\\nHost: vip.domain.com\\r\\n\\r\\n", "OK", 5, "none" );
        mockPoolSave(13l, 13l, true, 8443, "10.0.0.1", "HTTP", "GET /heal HTTP/1.0\\r\\nHost: vip.domain.com\\r\\n\\r\\n", "OK", 5, "none" );

        Answer answer = _resource.executeRequest(cmd);

        List<GloboNetworkPoolResponse.Pool> pools = ((GloboNetworkPoolResponse)answer).getPools();

        assertEquals(2, pools.size());

        GloboNetworkPoolResponse.Pool pool = pools.get(0);
        assertEquals((Long)12l, pool.getId());
        assertEquals((Integer)5, pool.getMaxconn());
        assertEquals("HTTP", pool.getHealthcheckType());
        assertEquals("GET /heal HTTP/1.0\\r\\nHost: vip.domain.com\\r\\n\\r\\n", pool.getHealthcheck());
        assertEquals("OK", pool.getExpectedHealthcheck());

        pool = pools.get(1);
        assertEquals((Long) 13l, pool.getId());
        assertEquals((Integer)5, pool.getMaxconn());
        assertEquals("HTTP", pool.getHealthcheckType());
        assertEquals("GET /heal HTTP/1.0\\r\\nHost: vip.domain.com\\r\\n\\r\\n", pool.getHealthcheck());
        assertEquals("OK", pool.getExpectedHealthcheck());
    }

    private PoolV3 mockPool(Long poolId, String identifier, int port, String lbmethod, String healthheckType, String healthcheck, String expectedHealthcheck, String destination, Integer maxconn) {

        PoolV3 pool = new PoolV3();
        pool.setId(poolId);
        pool.setIdentifier(identifier);
        pool.setDefaultPort(port);
        pool.setLbMethod(lbmethod);
        pool.setMaxconn(maxconn);

        PoolV3.Healthcheck healthchecker = pool.getHealthcheck();
        healthchecker.setHealthcheck(healthheckType, healthcheck, expectedHealthcheck);
        healthchecker.setDestination(destination);

        PoolV3.ServiceDownAction action = new PoolV3.ServiceDownAction();
        action.setId(3l);
        action.setName("none");
        pool.setServiceDownAction(action);

        pool.setEnvironment(120l);

        return pool;
    }

    @Test
    public void testBuildPoolMembers() throws GloboNetworkException {
        List<GloboNetworkVipResponse.Real> realList = new ArrayList<GloboNetworkVipResponse.Real>();
        GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
        real.setIp("10.0.0.1");
        real.setEnvironmentId(1212l);
        real.setVmName("vmname-1");
        realList.add(real);

        GloboNetworkVipResponse.Real real2 = new GloboNetworkVipResponse.Real();
        real2.setEnvironmentId(1212l);
        real2.setIp("10.0.0.2");
        real2.setVmName("vmname-2");
        realList.add(real2);

        GloboNetworkVipResponse.Real realRevoked = new GloboNetworkVipResponse.Real();
        realRevoked.setRevoked(true);
        realList.add(realRevoked);

        //real 1
        Ipv4 ipv4 = new Ipv4();
        ipv4.setId(1111l);
        when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment("10.0.0.1", 1212l, false)).thenReturn(ipv4);

        Equipment equipment = new Equipment();
        equipment.setId(111l);
        equipment.setName("equip-1");
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName("vmname-1")).thenReturn(equipment);

        //real 2
        ipv4 = new Ipv4();
        ipv4.setId(2222l);
        when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment("10.0.0.2", 1212l, false)).thenReturn(ipv4);

        equipment = new Equipment();
        equipment.setId(222l);
        equipment.setName("equip-2");
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName("vmname-2")).thenReturn(equipment);



        //execute
        List<PoolV3.PoolMember> poolMembers = _resource.buildPoolMembers(realList);


        //assert
        assertEquals(2, poolMembers.size());

        PoolV3.PoolMember poolMember = poolMembers.get(0);
        assertEquals("10.0.0.1", poolMember.getIp().getIpFormated());
        assertEquals((Long)1111l, poolMember.getIp().getId());
        assertEquals((Long)111l, poolMember.getEquipmentId());
        assertEquals("equip-1", poolMember.getEquipmentName());

        PoolV3.PoolMember poolMember2 = poolMembers.get(1);
        assertEquals("10.0.0.2", poolMember2.getIp().getIpFormated());
        assertEquals((Long)2222l, poolMember2.getIp().getId());
        assertEquals((Long)222l, poolMember2.getEquipmentId());
        assertEquals("equip-2", poolMember2.getEquipmentName());

    }

    @Test
    public void testSavePoolsEmptyPool() throws GloboNetworkException {
        //mock input
        List<String> ports = Arrays.asList("80:8080", "443:8443");
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        cmd.setHost("vip.domain.com");
        cmd.setPorts(ports);
        cmd.setHealthcheckType("HTTP");
        cmd.setHealthcheck("/index.html");
        cmd.setExpectedHealthcheck("WORKING");
        cmd.setHealthCheckDestination(null);
        cmd.setMethodBal("roundrobin");

        GloboNetworkResource.VipInfoHelper vipInfos = new GloboNetworkResource.VipInfoHelper(125l, null, null, null);


        //mock save
        List<PoolV3.PoolMember> poolMembers = new ArrayList<>();

        PoolV3 pool = new PoolV3();
        pool.setIdentifier("ACS_POOL_vip.domain.com_8080");
        pool.setLbMethod("round-robin");
        pool.setMaxconn(0);
        pool.setDefaultPort(8080);
        pool.setEnvironment(125l);

        PoolV3.Healthcheck healthcheck = pool.getHealthcheck();
        healthcheck.setHealthcheck("HTTP", "/index.html", "WORKING");
        healthcheck.setDestination(null);

        when(_resource._globoNetworkApi.getPoolAPI().save(pool)).thenReturn(new PoolV3(123l));

        PoolV3 pool2 = new PoolV3();
        pool2.setIdentifier("ACS_POOL_vip.domain.com_8443");
        pool2.setLbMethod("round-robin");
        pool2.setMaxconn(0);
        pool2.setDefaultPort(8443);
        pool2.setEnvironment(125l);

        PoolV3.Healthcheck healthcheck2 = pool2.getHealthcheck();
        healthcheck2.setHealthcheck("TCP", "", "");
        healthcheck2.setDestination(null);

        when(_resource._globoNetworkApi.getPoolAPI().save(pool2)).thenReturn(new PoolV3(321l));

        //execute
        List<VipPoolMap> vipPoolMaps = _resource.savePools(null, vipInfos, poolMembers, cmd);

        assertEquals(2, vipPoolMaps.size());

        VipPoolMap vipPoolMap = vipPoolMaps.get(0);
        assertEquals((Long)123l, vipPoolMap.getPoolId());
        assertEquals((Integer)80, vipPoolMap.getPort());

        vipPoolMap = vipPoolMaps.get(1);
        assertEquals((Long)321l, vipPoolMap.getPoolId());
        assertEquals((Integer)443, vipPoolMap.getPort());
    }

    @Test
    public void testSavePoolWithOneReal() throws GloboNetworkException {
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        cmd.setRealList(Arrays.asList(new GloboNetworkVipResponse.Real()));
        cmd.setMethodBal("roundrobin");
        cmd.setHost("vip.domain.com");
        cmd.setPorts(Arrays.asList("80:8080"));

        HealthCheckHelper build = HealthCheckHelper.build("vip.domain.com", "TCP", "", null);
        cmd.setHealthcheckType(build.getHealthCheckType());
        cmd.setExpectedHealthcheck(build.getExpectedHealthCheck());
        cmd.setHealthcheck(build.getHealthCheck());

        VipJson vip = null; // VIP NOT CREATED YET
        Ipv4 ip = new Ipv4();
        ip.setNetworkId(1L);

        List<PoolV3.PoolMember> poolMembers = new ArrayList<PoolV3.PoolMember>();
        PoolV3.PoolMember poolMember = new PoolV3.PoolMember();
        poolMember.setPortReal(8080);
        poolMember.setPriority(0);
        poolMember.setMemberStatus(7);
        poolMember.setEquipmentName("vm-01");
        poolMember.setEquipmentId(1l);
        poolMember.setWeight(0);

        PoolV3.Ip ipPm = new PoolV3.Ip();
        ipPm.setId(1l);
        ipPm.setIpFormated("10.0.0.1");

        poolMember.setIp(ipPm);
        poolMembers.add(poolMember);

        GloboNetworkResource.VipInfoHelper vipInfo = new GloboNetworkResource.VipInfoHelper(120l, null, null, null);

        PoolV3 expectedPool = mockPoolSave(null, 123l, true, 8080, "10.0.0.1",
                build.getHealthCheckType(), build.getExpectedHealthCheck(), build.getHealthCheck(), 0,
                cmd.getServiceDownAction());


        List<VipPoolMap> vipPoolMaps = _resource.savePools(vip, vipInfo, poolMembers, cmd);

        VipPoolMap vipPoolMap = vipPoolMaps.get(0);
        assertNotNull(vipPoolMap);
        assertEquals(new Integer(80), vipPoolMap.getPort());
        verify(_resource._globoNetworkApi.getPoolAPI(), times(1)).save(expectedPool);
    }

    @Test
    public void testSavePoolAddRealToExistingPool() throws GloboNetworkException {
        //input 1 - vip
        VipJson vip = new VipJson();
        Pool pool = new Pool();
        pool.setDefaultPort(8080);
        pool.setId(12L);
        pool.setMaxconn(0);
        vip.setPools(Arrays.asList(pool));
        Ipv4 ip = new Ipv4();
        ip.setNetworkId(1L);

        //input 2 - vip info
        GloboNetworkResource.VipInfoHelper vipInfo = new GloboNetworkResource.VipInfoHelper(121l, null, null, null);

        //input 3 - poolMembers
        List<PoolV3.PoolMember> poolMembers = new ArrayList<PoolV3.PoolMember>();
        PoolV3.PoolMember real1 = mockPoolMember(null, 8080, 1l, "10.0.0.1", 1l, "vm-01");
        poolMembers.add(real1);
        PoolV3.PoolMember real2 = mockPoolMember(null, 8080, 2l, "10.0.0.2", 2l, "vm-02");
        poolMembers.add(real2);

        //input 4 - cmd
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        cmd.setMethodBal("roundrobin");
        cmd.setHost("vip.domain.com");
        cmd.setPorts(Arrays.asList("80:8080"));
        HealthCheckHelper build = HealthCheckHelper.build("vip.domain.com", "TCP", "", null);
        cmd.setHealthcheckType(build.getHealthCheckType());
        cmd.setExpectedHealthcheck(build.getExpectedHealthCheck());
        cmd.setHealthcheck(build.getHealthCheck());

        //mock 1 - Pool find by id - v3
        PoolV3 poolv3GetById = mockPool(12l, "ACS_POOL_", 8080, "round-robin", build.getHealthCheckType(), build.getHealthCheck(), build.getExpectedHealthCheck(), "*", 5);
        PoolV3.PoolMember poolM = mockPoolMember(200l, 8080, 1l, "10.0.0.1", 1l, "vm-01");
        poolv3GetById.getPoolMembers().add(poolM);

        when(_resource._globoNetworkApi.getPoolAPI().getById(12l)).thenReturn(poolv3GetById);

        //mock 2 - Pool save pool
        PoolV3 poolToSave = mockPool(12l, "ACS_POOL_", 8080, "round-robin", build.getHealthCheckType(), build.getHealthCheck(), build.getExpectedHealthCheck(), "*", 5);
        PoolV3.PoolMember poolMSaved = mockPoolMember(200l, 8080, 1l, "10.0.0.1", 1l, "vm-01");
        poolToSave.getPoolMembers().add(poolMSaved);
        PoolV3.PoolMember poolM2Saved = mockPoolMember(null, 8080, 2l, "10.0.0.2", 2l, "vm-02");
        poolToSave.getPoolMembers().add(poolM2Saved);

        when(_resource._globoNetworkApi.getPoolAPI().save(poolToSave)).thenReturn(new PoolV3(12l));


        List<VipPoolMap> vipPoolMaps = _resource.savePools(vip, vipInfo, poolMembers, cmd);

        VipPoolMap vipPoolMap = vipPoolMaps.get(0);
        assertNotNull(vipPoolMap);
        assertEquals(new Integer(80), vipPoolMap.getPort());
        verify(_resource._globoNetworkApi.getPoolAPI(), times(1)).save(poolToSave);
    }

    private PoolV3.PoolMember mockPoolMember(Long id,Integer port, Long ipId, String ip, Long equipId, String equipName) {
        PoolV3.PoolMember poolMSaved = new PoolV3.PoolMember();
        if ( id != null ){
            poolMSaved.setId(id);
        }
        poolMSaved.setPortReal(port);
        poolMSaved.setWeight(0);
        poolMSaved.setPriority(0);
        poolMSaved.setEquipmentId(equipId);
        poolMSaved.setEquipmentName(equipName);

        PoolV3.Ip ipppoolMSaved = new PoolV3.Ip();
        ipppoolMSaved.setIpFormated(ip);
        ipppoolMSaved.setId(ipId);
        poolMSaved.setIp(ipppoolMSaved);
        return poolMSaved;
    }

    @Test
    public void testExecuteListAllExpectedHealthcheck() throws GloboNetworkException {
        ListExpectedHealthchecksCommand command = new ListExpectedHealthchecksCommand();

        List<ExpectHealthcheck> result = new ArrayList<>();
        result.add(new ExpectHealthcheck(1l, "OK"));
        result.add(new ExpectHealthcheck(2l, "WORKING"));

        when(_resource._globoNetworkApi.getExpectHealthcheckAPI().listHealthcheck()).thenReturn(result);

        Answer answer = _resource.executeRequest((ListExpectedHealthchecksCommand) command);

        assertNotNull(answer);

        GloboNetworkExpectHealthcheckResponse response = (GloboNetworkExpectHealthcheckResponse)answer;

        List<GloboNetworkExpectHealthcheckResponse.ExpectedHealthcheck> expectedHealthchecks = response.getExpectedHealthchecks();

        assertEquals(2, expectedHealthchecks.size());

        GloboNetworkExpectHealthcheckResponse.ExpectedHealthcheck expectedHealthcheck = expectedHealthchecks.get(0);
        assertEquals((Long)1l, expectedHealthcheck.getId());
        assertEquals("OK", expectedHealthcheck.getExpected());

        expectedHealthcheck = expectedHealthchecks.get(1);
        assertEquals((Long)2l, expectedHealthcheck.getId());
        assertEquals("WORKING", expectedHealthcheck.getExpected());

    }

    private long getNewIpID() {
        return ++s_ipSequence;
    }

    private VipJson buildFakeVipValidatedAndCreated(Long vipEnvironment, Long realEnvironment, Long vipIpId, String... reals) throws GloboNetworkException {
        List<String> ports = Arrays.asList(new String[] { "80:8080" });
        VipJson vip = this.buildFakeVip(vipEnvironment, realEnvironment, vipIpId, ports, reals);
        vip.setCreated(true);
        return vip;
    }

    private VipJson buildFakeVip(Long vipEnvironment, Long realEnvironment, Long vipIpId, List<String> servicePorts, String... reals) throws GloboNetworkException {
        vipIp  = new Ipv4();
        vipIp.setId(vipIpId);
        vipIp.setOct1(192);
        vipIp.setOct2(168);
        vipIp.setOct3(1);
        vipIp.setOct4(15);
        vipIp.setNetworkId(1L);
        when(_resource._globoNetworkApi.getIpAPI().checkVipIp(vipIp.getIpString(), vipEnvironment, false)).thenReturn(vipIp);

        VipEnvironment environmentVip = new VipEnvironment();
        environmentVip.setId(vipEnvironment);
        environmentVip.setFinality("BACKEND");
        environmentVip.setClient("CLIENT");
        environmentVip.setEnvironmentName("TESTAPI");
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(vipEnvironment, null, null, null)).thenReturn(environmentVip);
        when(_resource._globoNetworkApi.getVipEnvironmentAPI().search(null, "BACKEND", "CLIENT", "TESTAPI")).thenReturn(environmentVip);

        VipJson vip = new VipJson();
        vip.setId(987L);
        vip.setIps(Arrays.asList(vipIp.getIpString()));
        vip.setServicePorts(servicePorts);
        vip.setHost("vip.domain.com");
        vip.setBusinessArea("vipbusiness");
        vip.setMethod("least-conn");
        vip.setServiceName("vipservice");
        vip.setFinality("BACKEND");
        vip.setClient("CLIENT");
        vip.setEnvironment("TESTAPI");
        vip.setCache("(nenhum)");
        vip.setCreated(false);
        vip.setMaxConn(0);
        vip.setTimeout(5);
        vip.setValidated(true);
        vip.setPools(new ArrayList<Pool>());

        List<RealIP> realIpList = new ArrayList<>();
        Ip ip = new Ipv4();
        ip.setId(getNewIpID());
        when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment(ip.toString(), realEnvironment, false)).thenReturn(ip);

        RealIP realIp = new RealIP();
        realIp.setIpId(ip.getId());
        realIp.setName("vm-" + ip.getId());
        realIp.setVipPort(Integer.valueOf(servicePorts.get(0).split(":")[0]));
        realIp.setRealPort(Integer.valueOf(servicePorts.get(0).split(":")[1]));
        realIpList.add(realIp);

        for (String servicePort : servicePorts) {

            Pool pool = new Pool();
            pool.setId(Long.valueOf(servicePort.split(":")[0]));
            pool.setLbMethod("least-conn");
            pool.setPoolCreated(true);
            pool.setDefaultPort(new Integer(servicePort.split(":")[1]));
            pool.setIdentifier("POOL_" + servicePort.split(":")[1]);
            pool.setDefaultLimit(0);
            Pool.Environment env = new Pool.Environment();
            env.setId(120l);
            pool.setEnvironment(env);

            vip.getPools().add(pool);
        }
        vip.setRealsIp(realIpList);

        return vip;
    }


    protected VipXml createVipXML(List<String> ports, RealIP...realIPs) {
        VipXml vip = new VipXml();
        vip.setId(1L);
        vip.setHost("vip.domain.com");
        vip.setIps(Arrays.asList("192.168.0.4"));
        vip.setEnvironment("1");
        vip.setCache("cache");
        vip.setMethod("round-robin");
        vip.setPersistence("cookie");
        vip.setHealthcheckType("healthcheckType");
        vip.setHealthcheck("healthcheck");
        vip.setMaxConn(0);
        vip.setServicePorts(ports);
        vip.setRealsIp(Arrays.asList(realIPs));
        return vip;
    }

    private VipXml fromVipJsonToVipXml(VipJson vipJson) {
        VipXml vipXml = new VipXml();
        vipXml.setCache(vipJson.getCache());
        vipXml.setPersistence(vipJson.getPersistence());
        vipXml.setPools(vipJson.getPools());
        vipXml.setRealsIp(vipJson.getRealsIp());
        vipXml.setBusinessArea(vipJson.getBusinessArea());
        vipXml.setClient(vipJson.getClient());
        vipXml.setCreated(vipJson.getCreated());
        vipXml.setEnvironment(vipJson.getEnvironment());
        vipXml.setExpectedHealthcheckId(vipJson.getExpectedHealthcheckId());
        vipXml.setFinality(vipJson.getFinality());
        vipXml.setHealthcheck(vipJson.getHealthcheck());
        vipXml.setHealthcheckType(vipJson.getHealthcheckType());
        vipXml.setHost(vipJson.getHost());
        vipXml.setId(vipJson.getId());
        vipXml.setIps(vipJson.getIps());
        vipXml.setIpv4Description(vipJson.getIpv4Description());
        vipXml.setIpv4Id(vipJson.getIpv4Id());
        vipXml.setIpv6Id(vipJson.getIpv6Id());
        vipXml.setL7Filter(vipJson.getL7Filter());
        vipXml.setMaxConn(vipJson.getMaxConn());
        vipXml.setMethod(vipJson.getMethod());
        vipXml.setRealsPriorities(vipJson.getRealsPriorities());
        vipXml.setRealsWeights(vipJson.getRealsWeights());
        vipXml.setRuleId(vipJson.getRuleId());
        vipXml.setServiceName(vipJson.getServiceName());
        vipXml.setServicePorts(vipJson.getServicePorts());
        vipXml.setTimeout(vipJson.getTimeout());
        vipXml.setValidated(vipJson.getValidated());
        vipXml.setServicePorts(new ArrayList<String>());
        for(Pool pool : vipJson.getPools()){
            vipXml.getServicePorts().add("" + pool.getDefaultPort());
        }

        return vipXml;
    }
}
