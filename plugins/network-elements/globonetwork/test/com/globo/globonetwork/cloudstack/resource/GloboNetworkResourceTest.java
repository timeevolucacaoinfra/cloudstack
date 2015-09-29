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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cloud.exception.InvalidParameterValueException;
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

import com.globo.globonetwork.cloudstack.commands.ListPoolLBCommand;
import com.globo.globonetwork.cloudstack.commands.UpdatePoolCommand;
import com.globo.globonetwork.cloudstack.response.GloboNetworkPoolResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.globo.globonetwork.cloudstack.commands.AddOrRemoveVipInGloboNetworkCommand;
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
        when(_resource._globoNetworkApi.getVipAPI()).thenReturn(mock(VipAPI.class));
    }

    static long s_ipSequence = 100;

    @Test
    public void testTryToRemoveNullVIP(){
        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setIpv4("192.168.1.2");
        Answer answer = _resource.removeVIP(cmd, null);
        assertTrue(answer.getResult());
        assertEquals("VIP 192.168.1.2 already removed from GloboNetwork", answer.getDetails());
    }

    @Test
    public void testTryToRemoveAlreadyRemovedVIP() throws GloboNetworkException {
        Vip vip = new VipXml();
        vip.setId(1L);
        when(_resource._globoNetworkApi.getVipAPI().getById(vip.getId())).thenReturn(null);

        Answer answer = _resource.removeVIP(new AddOrRemoveVipInGloboNetworkCommand(), vip);
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

        Answer answer = _resource.removeVIP(new AddOrRemoveVipInGloboNetworkCommand(), vip);
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

        Answer answer = _resource.removeVIP(new AddOrRemoveVipInGloboNetworkCommand(), vip);
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

        Answer answer = _resource.removeVIP(new AddOrRemoveVipInGloboNetworkCommand(), vip);
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
        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
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

        assertNotNull(_resource.findPoolByPort(80, vip));
        assertNull(_resource.findPoolByPort(81, vip));
    }

    @Test
    public void testFindPoolByPorGivenNullVip(){
        assertNull(_resource.findPoolByPort(80, null));
    }

    @Test
    public void testBuildHealthCheckStringGivenPathAndHostNull(){
        GloboNetworkResource.HealthCheck healthCheck = new GloboNetworkResource.HealthCheck(null, null);
        assertEquals("", healthCheck.buildHealthCheckString(null, null));
    }

    @Test
    public void testBuildHealthCheckStringGivenPathNullAndHostFilled(){
        GloboNetworkResource.HealthCheck healthCheck = new GloboNetworkResource.HealthCheck(null, null);
        assertEquals("", healthCheck.buildHealthCheckString(null, "host"));
    }

    @Test
    public void testBuildHealthCheckStringGiveFullHTTPPath(){
        GloboNetworkResource.HealthCheck healthCheck = new GloboNetworkResource.HealthCheck(null, null);
        assertEquals("GET /healtcheck.html", healthCheck.buildHealthCheckString("GET /healtcheck.html", "host"));
    }

    @Test
    public void testBuildHealthCheckStringGivenURIandHost(){
        GloboNetworkResource.HealthCheck healthCheck = new GloboNetworkResource.HealthCheck(null, null);
        assertEquals("GET /healtcheck.html HTTP/1.0\\r\\nHost: host\\r\\n\\r\\n", healthCheck.buildHealthCheckString("/healtcheck.html", "host"));
    }

    @Test
    public void testCreateEmptyPool() throws GloboNetworkException {
        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setRealList(Arrays.asList(new GloboNetworkVipResponse.Real()));
        cmd.setMethodBal("roundrobin");

        VipJson vip = null; // VIP NOT CREATED YET
        Ipv4 ip = new Ipv4();
        ip.setNetworkId(1L);

        Map<String, List<RealIP>> realIps = new HashMap<>();
        List<Integer> realPriorities = new ArrayList<>();
        List<String> equipNames = new ArrayList<>();
        List<Long> equipIds = new ArrayList<>();
        List<Long> idPoolMembers = new ArrayList<>();
        List<Long> realWeights = new ArrayList<>();

        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(1L, false)).thenReturn(new IPv4Network());
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(new Vlan());
        when(_resource._globoNetworkApi.getPoolAPI().save(
            isNull(Long.class), anyString(), eq(8080),
            isNull(Long.class), eq("round-robin"), eq("TCP"), isNull(String.class),
            eq(""), eq(0), anyList(), anyList(), anyList(), anyList(), anyList(),
            anyList(), anyList(), isNull(String.class), isNull(String.class))
        ).thenReturn(new Pool());

        VipPoolMap vipPoolMap = _resource.createPool(cmd, vip, "host", ip, realIps, realPriorities, equipNames, equipIds, idPoolMembers, realWeights, "80:8080");

        assertNotNull(vipPoolMap);
        assertEquals(new Integer(80), vipPoolMap.getPort());
    }

    @Test
    public void testCreatePoolWithOneReal() throws GloboNetworkException {
        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setRealList(Arrays.asList(new GloboNetworkVipResponse.Real()));
        cmd.setMethodBal("roundrobin");

        VipJson vip = null; // VIP NOT CREATED YET
        Ipv4 ip = new Ipv4();
        ip.setNetworkId(1L);

        Map<String, List<RealIP>> realIps = new HashMap<>();
        realIps.put("", Arrays.asList(new RealIP(1L, 80, "192.268.0.4", 8080)));
        List<Integer> realPriorities = Arrays.asList(0);
        List<String> equipNames = Arrays.asList("vm-01");
        List<Long> equipIds = Arrays.asList(1L);
        List<Long> idPoolMembers = Arrays.asList(0L);
        List<Long> realWeights = Arrays.asList(0L);

        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(1L, false)).thenReturn(new IPv4Network());
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(new Vlan());
        when(_resource._globoNetworkApi.getPoolAPI().save(
            isNull(Long.class), anyString(), eq(8080),
            isNull(Long.class), eq("round-robin"), eq("TCP"), isNull(String.class),
            eq(""), eq(0), anyList(), anyList(), anyList(), anyList(), anyList(),
            anyList(), anyList(), isNull(String.class), isNull(String.class))
        ).thenReturn(new Pool());

        VipPoolMap vipPoolMap = _resource.createPool(cmd, vip, "host", ip, realIps, realPriorities, equipNames, equipIds, idPoolMembers, realWeights, "80:8080");

        assertNotNull(vipPoolMap);
        assertEquals(new Integer(80), vipPoolMap.getPort());
        verify(_resource._globoNetworkApi.getPoolAPI(), times(1)).save(
                isNull(Long.class), anyString(), eq(8080),
                isNull(Long.class), eq("round-robin"), eq("TCP"), isNull(String.class),
                eq(""), eq(0), anyList(), anyList(), anyList(), anyList(), anyList(),
                anyList(), anyList(), isNull(String.class), isNull(String.class)
        );
    }

    @Test
    public void testCreatePoolGivenInvalidNetwork() throws GloboNetworkException {
        Ipv4 ip = new Ipv4();
        ip.setNetworkId(1L);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(1L, false)).thenReturn(new IPv4Network());
        when(_resource._globoNetworkApi.getVlanAPI().getById(999L)).thenReturn(new Vlan());
        try{
            _resource.createPool(new AddOrRemoveVipInGloboNetworkCommand(), null, "host", ip, null, null, null, null, null, null, "80:8080");
        }catch(InvalidParameterValueException e){
            assertEquals("Vlan " + null + " was not found in GloboNetwork", e.getMessage());
        }
    }

    @Test
    public void testCreatePoolGivenInvalidVlan() throws GloboNetworkException {
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(1L, false)).thenReturn(new IPv4Network());
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(new Vlan());
        try{
            _resource.createPool(new AddOrRemoveVipInGloboNetworkCommand(), null, "host", new Ipv4(), null, null, null, null, null, null, "80:8080");
        }catch(InvalidParameterValueException e){
            assertEquals("Network " + null + " was not found in GloboNetwork", e.getMessage());
        }
    }

    @Test
    public void testCreateNewVIPFromScratch() throws Exception {
        VipJson vipToBeCreated = buildFakeVipValidatedAndCreated(123L, 546L, 345L);
        vipToBeCreated.setCreated(false);

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setVipId(null);
        cmd.setHost(vipToBeCreated.getHost());
        cmd.setIpv4(vipToBeCreated.getIps().get(0));
        cmd.setVipEnvironmentId(123L);
        cmd.setPorts(Arrays.asList("80:8080"));
        cmd.setBusinessArea(vipToBeCreated.getBusinessArea());
        cmd.setServiceName(vipToBeCreated.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setRuleState(FirewallRule.State.Add);

        GloboNetworkVipResponse.Real real = new GloboNetworkVipResponse.Real();
        real.setIp("1.2.3.4");
        real.setVmName("mynewreal");
        real.setPorts(Arrays.asList("80:8080"));
        real.setRevoked(false);
        real.setEnvironmentId(546L);
        cmd.setRealList(Arrays.asList(real));

        Equipment realEquipment = new Equipment();
        realEquipment.setId(999L);

        Ipv4 equipIpv4 = new Ipv4();
        equipIpv4.setId(1212L);
        equipIpv4.setOct1(Integer.valueOf("1.2.3.4".split("\\.")[0]));
        equipIpv4.setOct2(Integer.valueOf("1.2.3.4".split("\\.")[1]));
        equipIpv4.setOct3(Integer.valueOf("1.2.3.4".split("\\.")[2]));
        equipIpv4.setOct4(Integer.valueOf("1.2.3.4".split("\\.")[3]));

        when(_resource._globoNetworkApi.getVipAPI().getByPk(vipToBeCreated.getId())).thenReturn(vipToBeCreated);
        when(_resource._globoNetworkApi.getEquipmentAPI().listByName("mynewreal")).thenReturn(realEquipment);
        when(_resource._globoNetworkApi.getIpAPI().findByIpAndEnvironment(equipIpv4.getIpString(), real.getEnvironmentId(), false)).thenReturn(equipIpv4);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(vipIp.getNetworkId(), false)).thenReturn(new IPv4Network());
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(new Vlan());

        when(_resource._globoNetworkApi.getPoolAPI().save(
            isNull(Long.class), anyString(), eq(8080),
            isNull(Long.class), eq("round-robin"), eq("TCP"), isNull(String.class),
            eq(""), eq(0), anyList(), anyList(), anyList(), anyList(), anyList(),
            anyList(), anyList(), isNull(String.class), isNull(String.class))
        ).thenReturn(new Pool());

        when(_resource._globoNetworkApi.getVipAPI().getById(vipToBeCreated.getId())).thenReturn(fromVipJsonToVipXml(vipToBeCreated));

        when(_resource._globoNetworkApi.getVipAPI().save(
            eq(345L), isNull(Long.class), eq(vipToBeCreated.getFinality()), eq(vipToBeCreated.getClient()), eq(vipToBeCreated.getEnvironment()),
            eq(vipToBeCreated.getCache()), eq("(nenhum)"), eq(vipToBeCreated.getTimeout()), eq(vipToBeCreated.getHost()),
            eq(vipToBeCreated.getBusinessArea()), eq(vipToBeCreated.getServiceName()), isNull(String.class), anyList(), isNull(Long.class), isNull(Long.class))
        ).thenReturn(vipToBeCreated);

        Answer answer = _resource.execute(cmd);

        assertTrue(answer.getResult());
        assertTrue(answer instanceof GloboNetworkVipResponse);
        verify(_resource._globoNetworkApi.getPoolAPI(), times(1)).save(isNull(Long.class), anyString(), eq(8080),
            isNull(Long.class), eq("round-robin"), eq("TCP"), isNull(String.class),
            eq(""), eq(0), anyList(), anyList(), anyList(), anyList(), anyList(),
            anyList(), anyList(), isNull(String.class), isNull(String.class));
        verify(_resource._globoNetworkApi.getVipAPI(), times(1)).save(
            eq(345L), isNull(Long.class), eq(vipToBeCreated.getFinality()), eq(vipToBeCreated.getClient()), eq(vipToBeCreated.getEnvironment()),
            eq(vipToBeCreated.getCache()), eq("(nenhum)"), eq(vipToBeCreated.getTimeout()), eq(vipToBeCreated.getHost()),
            eq(vipToBeCreated.getBusinessArea()), eq(vipToBeCreated.getServiceName()), isNull(String.class), anyList(), isNull(Long.class), isNull(Long.class)
        );
    }

    @Test
    public void testCreateNewVIPWithMultiplePortsAndNoReal() throws Exception {
        VipJson vipToBeCreated = buildFakeVipValidatedAndCreated(123L, 546L, 345L);
        vipToBeCreated.setCreated(false);

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
        cmd.setVipId(null);
        cmd.setHost(vipToBeCreated.getHost());
        cmd.setIpv4(vipToBeCreated.getIps().get(0));
        cmd.setVipEnvironmentId(123L);
        cmd.setPorts(Arrays.asList("80:8080", "443:8443"));
        cmd.setBusinessArea(vipToBeCreated.getBusinessArea());
        cmd.setServiceName(vipToBeCreated.getServiceName());
        cmd.setMethodBal("roundrobin");
        cmd.setRealList(new ArrayList<GloboNetworkVipResponse.Real>());

        when(_resource._globoNetworkApi.getVipAPI().getByPk(vipToBeCreated.getId())).thenReturn(vipToBeCreated);
        when(_resource._globoNetworkApi.getNetworkAPI().getNetwork(vipIp.getNetworkId(), false)).thenReturn(new IPv4Network());
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(new Vlan());

        when(_resource._globoNetworkApi.getPoolAPI().save(
                        isNull(Long.class), anyString(), anyInt(),
                        isNull(Long.class), eq("round-robin"), eq("TCP"), isNull(String.class),
                        eq(""), eq(0), anyList(), anyList(), anyList(), anyList(), anyList(),
                        anyList(), anyList(), isNull(String.class), isNull(String.class))
        ).thenReturn(new Pool());

        when(_resource._globoNetworkApi.getVipAPI().getById(vipToBeCreated.getId())).thenReturn(fromVipJsonToVipXml(vipToBeCreated));

        when(_resource._globoNetworkApi.getVipAPI().save(
            eq(345L), isNull(Long.class), eq(vipToBeCreated.getFinality()), eq(vipToBeCreated.getClient()), eq(vipToBeCreated.getEnvironment()),
            eq(vipToBeCreated.getCache()), eq("(nenhum)"), eq(vipToBeCreated.getTimeout()), eq(vipToBeCreated.getHost()),
            eq(vipToBeCreated.getBusinessArea()), eq(vipToBeCreated.getServiceName()), isNull(String.class), anyList(), isNull(Long.class), isNull(Long.class))
        ).thenReturn(vipToBeCreated);

        Answer answer = _resource.execute(cmd);

        assertTrue(answer.getResult());
        verify(_resource._globoNetworkApi.getPoolAPI(), times(2)).save(isNull(Long.class), anyString(), anyInt(),
                isNull(Long.class), eq("round-robin"), eq("TCP"), isNull(String.class),
                eq(""), eq(0), anyList(), anyList(), anyList(), anyList(), anyList(),
                anyList(), anyList(), isNull(String.class), isNull(String.class)
        );
    }

    @Test
    public void testUpdateCreatedVIPPersistence() throws Exception {
        VipJson createdVip = buildFakeVipValidatedAndCreated(123L, 546L, 345L);
        String persistenceCS = "Cookie";
        String persistenceNetApi = "cookie";

        AddOrRemoveVipInGloboNetworkCommand cmd = new AddOrRemoveVipInGloboNetworkCommand();
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
        when(_resource._globoNetworkApi.getVlanAPI().getById(anyLong())).thenReturn(new Vlan());

        when(_resource._globoNetworkApi.getPoolAPI().save(
                        anyLong(), anyString(), anyInt(),
                        isNull(Long.class), eq("round-robin"), eq("TCP"), isNull(String.class),
                        eq(""), eq(0), anyList(), anyList(), anyList(), anyList(), anyList(),
                        anyList(), anyList(), isNull(String.class), isNull(String.class))
        ).thenReturn(new Pool());

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

        GloboNetworkVipResponse answer = (GloboNetworkVipResponse) _resource.createVipResponse(vip, new AddOrRemoveVipInGloboNetworkCommand());

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

        GloboNetworkVipResponse answer = (GloboNetworkVipResponse) _resource.createVipResponse(vip, new AddOrRemoveVipInGloboNetworkCommand());

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

        GloboNetworkVipResponse answer = (GloboNetworkVipResponse) _resource.createVipResponse(vip, new AddOrRemoveVipInGloboNetworkCommand());

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
        UpdatePoolCommand cmd = new UpdatePoolCommand(Arrays.asList(12l,13l), "HTTP", "/heal", "OK", 5);


        Pool.PoolResponse poolResponse = mockPoolResponse(12l, "MY_POOL", 80, "least", "http", "/heal.html", "OK", "*:*", 10, "EQUIP_NAME_2", 112l, "10.1.1.2", 10112l, 92, 52, 8080);
        when(_resource._globoNetworkApi.getPoolAPI().getByPk(12l)).thenReturn(poolResponse);

        poolResponse = mockPoolResponse(13l, "MY_POOL_3", 8080, "leastcon", "http", "/heal.html", "OK", "*:*", 11, "EQUIP_NAME_2", 113l, "10.1.1.3", 10113l, 93, 53, 8080);
        when(_resource._globoNetworkApi.getPoolAPI().getByPk(13l)).thenReturn(poolResponse);

        ArrayList<RealIP> realIPs = new ArrayList<>();
        RealIP realIp = new RealIP();
        realIp.setIpId(10112l);
        realIp.setRealIp("10.1.1.2");
        realIPs.add(realIp);

        when(_resource._globoNetworkApi.getPoolAPI().save(12l,
                "MY_POOL",
                80,
                4000l,
                "least",
                "HTTP",
                "OK",
                "/heal",
                5,
                realIPs,
                Arrays.asList("EQUIP_NAME_2"), //equipNames
                Arrays.asList(112l), //equipIds
                Arrays.asList(52), //priorities,
                Arrays.asList(92l), //weights
                Arrays.asList(8080), // realPorts
                Arrays.asList(333l), // idPoolMembers,
                "none",
                "*:*")).thenReturn(new Pool(12l));

        ArrayList<RealIP> realIPsNew = new ArrayList<>();
        RealIP realIpNew = new RealIP();
        realIpNew.setIpId(10113l);
        realIpNew.setRealIp("10.1.1.3");
        realIPsNew.add(realIpNew);

        when(_resource._globoNetworkApi.getPoolAPI().save(13l,
                "MY_POOL_3",
                8080,
                4000l,
                "leastcon",
                "HTTP",
                "OK",
                "/heal",
                5,
                realIPsNew,
                Arrays.asList("EQUIP_NAME_2"), //equipNames
                Arrays.asList(113l), //equipIds
                Arrays.asList(53), //priorities,
                Arrays.asList(93l), //weights
                Arrays.asList(8080), // realPorts
                Arrays.asList(333l), // idPoolMembers,
                "none",
                "*:*")).thenReturn(new Pool(13l));

        Answer answer = _resource.executeRequest(cmd);

        List<GloboNetworkPoolResponse.Pool> pools = ((GloboNetworkPoolResponse)answer).getPools();

        assertEquals(2, pools.size());

        GloboNetworkPoolResponse.Pool pool = pools.get(0);
        assertEquals((Long)12l, pool.getId());

        pool = pools.get(1);
        assertEquals((Long) 13l, pool.getId());
    }


    @Test
    public void testSavePool() throws GloboNetworkException {

        Pool.PoolResponse poolResponse = mockPoolResponse(12l, "MY_POOL", 80, "least", "http", "/heal.html", "OK", "**:*", 911, "EQUIP_NAME_2", 112l, "10.1.1.2", 10112l, 92, 52, 8080);

        ArrayList<RealIP> realIPs = new ArrayList<>();
        RealIP realIp = new RealIP();
        realIp.setIpId(10112l);
        realIp.setRealIp("10.1.1.2");
        realIPs.add(realIp);

        PoolAPI poolAPI = _resource._globoNetworkApi.getPoolAPI();
        when(poolAPI.save(12l,
                "MY_POOL",
                80,
                4000l,
                "least",
                "http",
                "OK",
                "/heal.html",
                911,
                realIPs,
                Arrays.asList("EQUIP_NAME_2"), //equipNames
                Arrays.asList(112l), //equipIds
                Arrays.asList(52), //priorities,
                Arrays.asList(92l), //weights
                Arrays.asList(8080), // realPorts
                Arrays.asList(333l), // idPoolMembers,
                "none",
                "**:*")).thenReturn(new Pool(12l));

        Pool pool = _resource.savePool(poolResponse.getPool(), poolResponse.getPoolMembers(), poolAPI);

        assertNotNull(pool);
        assertEquals((Long) 12l, pool.getId());

    }

    private Pool mockPool(Long poolId, String identifier, int port, String lbmethod, String healthheckType, String healthcheck, String expectedHealthcheck, String destination, Integer maxconn) {

        Pool pool = new Pool();
        pool.setId(poolId);
        pool.setIdentifier(identifier);
        pool.setDefaultPort(port);
        pool.setLbMethod(lbmethod);
        pool.setMaxconn(maxconn);
        pool.setVipPort(80);

        Pool.Healthcheck healthchecker = new Pool.Healthcheck();
        healthchecker.setHealthcheckType(healthheckType);
        healthchecker.setHealthcheckRequest(healthcheck);
        healthchecker.setExpectedHealthcheck(expectedHealthcheck);
        healthchecker.setDestination(destination);
        pool.setHealthcheck(healthchecker);

        Pool.ServiceDownAction action = new Pool.ServiceDownAction();
        action.setId(3333l);
        action.setName("none");
        action.setType("ServiceDownAction");
        pool.setServiceDownAction(action);

        Pool.Environment env = new Pool.Environment();
        env.setId(4000l);
        pool.setEnvironment(env);

        return pool;
    }


    private Pool.PoolResponse mockPoolResponse(Long poolId, String identifier, int port, String lbmethod,
                                               String healthheckType, String healthcheck, String expectedHealthcheck, String destination, Integer maxconn,
                                               String realEquName, Long realEquipId, String realIp, Long realIpId, Integer realWeight, Integer realPriority, Integer realPort) {
        Pool.PoolResponse response = new Pool.PoolResponse();

        Pool pool = mockPool(poolId, identifier, port, lbmethod, healthheckType, healthcheck, expectedHealthcheck, destination, maxconn);
        response.setPool(pool);

        Pool.PoolMember member = new Pool.PoolMember();
        member.setId(333l);
        member.setEquipmentName(realEquName);
        member.setWeight(realWeight);
        member.setPriority(realPriority);
        member.setEquipmentId(realEquipId);
        member.setPortReal(realPort);

        Pool.Ip ip = new Pool.Ip();
        ip.setId(realIpId);
        ip.setIpFormated(realIp);
        member.setIp(ip);

        response.setPoolMembers(Arrays.asList(member));

        return response;
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
            pool.setId(getNewIpID());
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
        vip.setHost("host");
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
