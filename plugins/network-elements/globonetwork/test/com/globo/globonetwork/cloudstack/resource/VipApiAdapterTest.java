package com.globo.globonetwork.cloudstack.resource;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.lb.LoadBalancingRule;
import com.globo.globonetwork.client.api.GloboNetworkAPI;
import com.globo.globonetwork.client.api.OptionVipV3API;
import com.globo.globonetwork.client.api.VipAPI;
import com.globo.globonetwork.client.api.VipV3API;
import com.globo.globonetwork.client.exception.GloboNetworkException;
import com.globo.globonetwork.client.model.VipJson;
import com.globo.globonetwork.client.model.VipPoolMap;
import com.globo.globonetwork.client.model.VipV3;
import com.globo.globonetwork.client.model.OptionVipV3;
import com.globo.globonetwork.client.model.Ipv4;
import com.globo.globonetwork.client.model.VipEnvironment;
import com.globo.globonetwork.cloudstack.commands.ApplyVipInGloboNetworkCommand;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.powermock.api.mockito.PowerMockito.doNothing;

public class VipApiAdapterTest {

    private GloboNetworkAPI globoNetworkAPI;
    private VipV3API vipV3API;
    private VipAPI vipV2API;
    private OptionVipV3API optionVipV3API;

    @Before
    public void setUp() throws Exception {
        globoNetworkAPI = mock(GloboNetworkAPI.class);
        vipV3API = mock(VipV3API.class);
        vipV2API = mock(VipAPI.class);
        optionVipV3API = mock(OptionVipV3API.class);
        when(globoNetworkAPI.getVipV3API()).thenReturn(vipV3API);
        when(globoNetworkAPI.getVipAPI()).thenReturn(vipV2API);
        when(globoNetworkAPI.getOptionVipV3API()).thenReturn(optionVipV3API);
    }

    @Test
    public void testCreateVipApiAdapterV2() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, false, "2");
        assertTrue(adapter.hasVip());
    }

    @Test
    public void testCreateVipApiAdapterV3() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, false, "3");
        assertTrue(adapter.hasVip());
    }

    @Test
    public void testCreateVipApiAdapterV2GivenNullId() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(null, false, "2");
        assertFalse(adapter.hasVip());
    }

    @Test
    public void testCreateVipApiAdapterV3GivenNullId() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(null, false, "3");
        assertFalse(adapter.hasVip());
    }

    @Test
    public void testCreateVipV2() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(null, false, "2");
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        VipEnvironment environment = new VipEnvironment();
        Ipv4 ip = new Ipv4();
        List<VipPoolMap> vipPoolMaps = new ArrayList<>();

        when(vipV2API.save(
            anyLong(), isNull(Long.class), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyInt(), anyString(), anyString(), anyString(), isNull(String.class),
            any(List.class), isNull(Long.class), isNull(Long.class))
        ).thenReturn(new VipJson());

        adapter.save(cmd, cmd.getHost(), environment, ip, vipPoolMaps);

        assertTrue(adapter.hasVip());
        verify(vipV2API).save(
            anyLong(), isNull(Long.class), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyInt(), anyString(), anyString(), anyString(), isNull(String.class),
            any(List.class), isNull(Long.class), isNull(Long.class)
        );
    }

    @Test
    public void testCreateVipV3() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(null, false, "3");
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        cmd.setVipEnvironmentId(1L);
        cmd.setHealthcheckType("TCP");
        VipEnvironment environment = new VipEnvironment();
        environment.setId(1L);
        Ipv4 ip = new Ipv4();
        List<VipPoolMap> vipPoolMaps = new ArrayList<>();
        VipPoolMap vipPoolMap = new VipPoolMap(1L, 1L, 1L, 80);
        vipPoolMaps.add(vipPoolMap);

        mockVipOptions(environment);

        adapter.save(cmd, cmd.getHost(), environment, ip, vipPoolMaps);
        assertTrue(adapter.hasVip());
        verify(vipV3API).save(any(VipV3.class));
    }

    @Test
    public void testUpdateDeployedVipV2() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, true, "2");
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        Ipv4 ip = new Ipv4();
        List<VipPoolMap> vipPoolMaps = new ArrayList<>();

        adapter.update(cmd, ip, vipPoolMaps);

        assertTrue(adapter.hasVip());
        verify(vipV2API).alterPersistence(eq(1L), anyString());
    }

    @Test
    public void testUpdateNotDeployedVipV2() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, false, "2");
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        Ipv4 ip = new Ipv4();
        List<VipPoolMap> vipPoolMaps = new ArrayList<>();

        when(vipV2API.save(
            anyLong(), isNull(Long.class), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyInt(), anyString(), anyString(), anyString(), isNull(String.class),
            any(List.class), isNull(Long.class), isNull(Long.class))
        ).thenReturn(new VipJson());

        adapter.update(cmd, ip, vipPoolMaps);

        assertTrue(adapter.hasVip());
        verify(vipV2API).save(
            anyLong(), isNull(Long.class), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyInt(), anyString(), anyString(), anyString(), isNull(String.class),
            any(List.class), isNull(Long.class), isNull(Long.class)
        );
    }

    @Test
    public void testUpdateNotDeployedVipV3() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, false, "3");
        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        cmd.setVipEnvironmentId(1L);
        cmd.setHealthcheckType("TCP");
        VipEnvironment environment = new VipEnvironment();
        environment.setId(1L);
        Ipv4 ip = new Ipv4();
        List<VipPoolMap> vipPoolMaps = new ArrayList<>();
        VipPoolMap vipPoolMap = new VipPoolMap(1L, 1L, 1L, 80);
        vipPoolMaps.add(vipPoolMap);

        mockVipOptions(environment);
        when(vipV3API.save(any(VipV3.class))).thenReturn(new VipV3());

        adapter.update(cmd, ip, vipPoolMaps);
        assertTrue(adapter.hasVip());
        verify(vipV3API).save(any(VipV3.class));
    }

    @Test
    public void testUpdateVipV3Created() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(21L, true, "3");
        adapter.setVipV3(buildFakeVipV3(21L, true));

        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();
        LoadBalancingRule.LbStickinessPolicy stickinessPolicy = new LoadBalancingRule.LbStickinessPolicy("Source-ip", null, false);
        cmd.setPersistencePolicy(stickinessPolicy);


        VipJson vipJson = buildFakeVipV2(21L, true);
        vipJson.setPersistence("Cookie");
        when(vipV2API.getByPk(21L)).thenReturn(vipJson);


        String networkAPIStickiness = GloboNetworkResource.PersistenceMethod.fromPersistencePolicy(stickinessPolicy);
        doNothing().when(vipV2API).alterPersistence(21L, networkAPIStickiness);

        //execute
        adapter.update(cmd, null, null);


        //test
        assertTrue(adapter.hasVip());
        verify(vipV2API).getByPk(21L);
        verify(vipV2API).alterPersistence(21L, networkAPIStickiness);
    }

    @Test
    public void testUpdateVipV3NotCreated() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(21L, false, "3");


        ApplyVipInGloboNetworkCommand cmd = new ApplyVipInGloboNetworkCommand();

        cmd.setVipEnvironmentId(1L);
        cmd.setHealthcheckType("TCP");
        VipEnvironment environment = new VipEnvironment();
        environment.setId(1L);

        mockVipOptions(environment);

        when(vipV3API.save(adapter.getVipV3())).thenReturn(adapter.getVipV3());

        //execute
        adapter.update(cmd, null, null);


        //test
        verify(vipV3API).save(adapter.getVipV3());
    }




    @Test
    public void testDeployVipV2() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, false, "2");
        adapter.deploy();
        verify(vipV2API, times(1)).create(1L);
    }

    @Test
    public void testDeployVipV3() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, false, "3");
        adapter.deploy();
        verify(vipV3API, times(1)).deploy(1L);
    }

    @Test
    public void testUndeployVipV2() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, true, "2");
        adapter.undeploy();
        verify(vipV2API, times(1)).removeScriptVip(1L);
    }

    @Test
    public void testUndeployVipV3() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, true, "3");
        adapter.undeploy();
        verify(vipV3API, times(1)).undeploy(1L);
    }

    @Test
    public void testDeleteVipV2KeepingIP() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, true, "2");
        adapter.delete(true);
        verify(vipV2API, times(1)).removeVip(1L, true);
    }

    @Test
    public void testDeleteVipV3KeepingIP() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, true, "3");
        adapter.delete(true);
        verify(vipV3API, times(1)).delete(1L, true);
    }

    @Test
    public void testDeleteVipV2NotKeepingIP() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, true, "2");
        adapter.delete(false);
        verify(vipV2API, times(1)).removeVip(1L, false);
    }

    @Test
    public void testDeleteVipV3NotKeepingIP() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, true, "3");
        adapter.delete(false);
        verify(vipV3API, times(1)).delete(1L, false);
    }

    @Test
    public void testValidateVipV2() throws GloboNetworkException {
        VipApiAdapter adapter = createTestVipAdapter(1L, true, "2");
        adapter.validate(new Ipv4());
        verify(vipV2API, times(2)).getByPk(1L);
        verify(vipV2API).validate(1L);
    }

    @Test
    public void testGetPersistenceMethodNone(){
        assertEquals("(nenhum)", VipApiAdapter.getPersistenceMethod(new LoadBalancingRule.LbStickinessPolicy("None", null)));
        assertEquals("(nenhum)", VipApiAdapter.getPersistenceMethod(null));
    }

    @Test
    public void testGetPersistenceMethodGivenCookie(){
        assertEquals("cookie", VipApiAdapter.getPersistenceMethod(new LoadBalancingRule.LbStickinessPolicy("Cookie", null)));
    }

    @Test
    public void testGetPersistenceMethodGivenSourceIp(){
        assertEquals("source-ip", VipApiAdapter.getPersistenceMethod(new LoadBalancingRule.LbStickinessPolicy("Source-ip", null)));
    }

    @Test
    public void testGetPersistenceMethodGivenSourceIpWithPersistence(){
        assertEquals("source-ip com persist. entre portas", VipApiAdapter.getPersistenceMethod(new LoadBalancingRule.LbStickinessPolicy("Source-ip with persistence between ports", null)));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetPersistenceMethodGivenInvalidPersistence(){
        VipApiAdapter.getPersistenceMethod(new LoadBalancingRule.LbStickinessPolicy("jsession", null));
    }

    private VipV3 buildFakeVipV3(Long vipId, Boolean created) throws GloboNetworkException {
        VipV3 vip = new VipV3();
        vip.setId(vipId);
        vip.setCreated(created);
        return vip;
    }

    private VipJson buildFakeVipV2(Long vipId, Boolean created) throws GloboNetworkException {
        VipJson vip = new VipJson();
        vip.setId(vipId);
        vip.setCreated(created);
        return vip;
    }

    private VipApiAdapter createTestVipAdapter(Long vipId, Boolean created, String version) throws GloboNetworkException {
        when(vipV3API.getById(vipId)).thenReturn(buildFakeVipV3(vipId, created));
        when(vipV2API.getByPk(vipId)).thenReturn(buildFakeVipV2(vipId, created));
        return new VipApiAdapter(vipId, globoNetworkAPI, version);
    }

    private void mockVipOptions(VipEnvironment environment) throws GloboNetworkException {
        OptionVipV3 cache = new OptionVipV3(1L, "cache", "(nenhum)");
        OptionVipV3 trafficReturn = new OptionVipV3(2L, "Retorno de trafego", "Normal");
        OptionVipV3 timeout = new OptionVipV3(3L, "timeout", "5");
        OptionVipV3 persistence = new OptionVipV3(4L, "Persistencia", "(nenhum)");
        OptionVipV3 l7Rule = new OptionVipV3(5L, "l7_rule", "default_vip");
        OptionVipV3 l4Protocol = new OptionVipV3(5L, "l4_protocol", "TCP");
        OptionVipV3 l7Protocol = new OptionVipV3(5L, "l7_protocol", "Outros");

        when(optionVipV3API.findOptionsByTypeAndName(environment.getId(), "cache", "(nenhum)")).thenReturn(Collections.singletonList(cache));
        when(optionVipV3API.findOptionsByTypeAndName(environment.getId(), "Retorno de trafego", "Normal")).thenReturn(Collections.singletonList(trafficReturn));
        when(optionVipV3API.findOptionsByTypeAndName(environment.getId(), "timeout", "5")).thenReturn(Collections.singletonList(timeout));
        when(optionVipV3API.findOptionsByTypeAndName(environment.getId(), "Persistencia", "(nenhum)")).thenReturn(Collections.singletonList(persistence));
        when(optionVipV3API.findOptionsByTypeAndName(environment.getId(), "l7_rule", "default_vip")).thenReturn(Collections.singletonList(l7Rule));
        when(optionVipV3API.findOptionsByTypeAndName(environment.getId(), "l4_protocol", "TCP")).thenReturn(Collections.singletonList(l4Protocol));
        when(optionVipV3API.findOptionsByTypeAndName(environment.getId(), "l7_protocol", "Outros")).thenReturn(Collections.singletonList(l7Protocol));
    }
}