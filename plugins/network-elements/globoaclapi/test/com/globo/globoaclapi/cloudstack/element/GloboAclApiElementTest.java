package com.globo.globoaclapi.cloudstack.element;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.globo.globoaclapi.cloudstack.commands.ACLRuleCommand;
import com.globo.globoaclapi.cloudstack.commands.CreateACLRuleCommand;
import com.globo.globoaclapi.cloudstack.commands.RemoveACLRuleCommand;
import com.globo.globonetwork.client.model.Vlan;
import com.globo.globonetwork.cloudstack.GloboNetworkNetworkVO;
import com.globo.globonetwork.cloudstack.dao.GloboNetworkNetworkDao;
import com.globo.globonetwork.cloudstack.manager.GloboNetworkManager;
import org.apache.cloudstack.acl.ControlledEntity;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GloboAclApiElementTest {

    private GloboAclApiElement element;

    @Before
    public void setUp() throws Exception {
        element = spy(new GloboAclApiElement());
        doReturn("user").when(element).getCallingUser();
        mockFindZone();
        mockCheckACLProvider();
    }

    @Test
    public void testApplyFWRulesGivenRuleWithoutDestinationCIDR() throws Exception {
        try{
            element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createTCPFirewallRuleVO(" ", 80, 81, FirewallRule.State.Add)));
            fail();
        }catch(InvalidParameterValueException e){
            assertEquals("Invalid source CIDR, value should not be empty.", e.getMessage());
        }
    }

    @Test
    public void testApplyFWRulesGivenNotPortsSupplied() throws Exception {
        try{
            element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createTCPFirewallRuleVO("10.0.0.5/24", null, null, FirewallRule.State.Add)));
            fail();
        }catch(InvalidParameterValueException e){
            assertEquals("Port start should not be empty.", e.getMessage());
        }
    }

    @Test
    public void testApplyFWRulesGivenEndPortGreaterThanStartPortSupplied() throws Exception {
        try{
            element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createTCPFirewallRuleVO("10.0.0.5/24", 80, 70, FirewallRule.State.Add)));
            fail();
        }catch(InvalidParameterValueException e){
            assertEquals("Port end should not be greater than port start.", e.getMessage());
        }
    }

    @Test
    public void testApplyFWRulesGivenNullIcmpCode() throws Exception {
        try{
            element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createICMPFirewallRuleVO("10.0.0.5/24", null, 1, FirewallRule.State.Add)));
            fail();
        }catch(InvalidParameterValueException e){
            assertEquals("ICMP type and code should not be empty", e.getMessage());
        }
    }

    @Test
    public void testApplyFWRulesGivenNullIcmpType() throws Exception {
        try{
            element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createICMPFirewallRuleVO("10.0.0.5/24", 1, null, FirewallRule.State.Add)));
            fail();
        }catch(InvalidParameterValueException e){
            assertEquals("ICMP type and code should not be empty", e.getMessage());
        }
    }

    @Test
    public void testApplyFWRulesGivenInvalidCommandResponse() throws Exception {
        mockGetGloboNetworkVlanNumber(1L);
        mockGetGloboNetworkEnvironmentID(1L);
        mockGetACLapiHost();
        AgentManager agentManager = mockAgentManagerEasySend(false, "Invalid ACL rule");

        try {
            element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createTCPFirewallRuleVO("10.0.0.5/24", 80, null, FirewallRule.State.Add)));
            fail();
        }catch(CloudRuntimeException e){
            assertEquals("Invalid ACL rule", e.getMessage());
            verify(agentManager, times(1)).easySend(anyLong(), any(CreateACLRuleCommand.class));
        }
    }

    @Test
    public void testApplyRevokedFWRulesGivenInvalidCommandResponse() throws Exception {
        mockGetGloboNetworkVlanNumber(1L);
        mockGetGloboNetworkEnvironmentID(1L);
        mockGetACLapiHost();
        AgentManager agentManager = mockAgentManagerEasySend(false, "Invalid ACL rule");

        try {
            element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createTCPFirewallRuleVO("10.0.0.5/24", 80, null, FirewallRule.State.Revoke)));
            fail();
        }catch(CloudRuntimeException e){
            assertEquals("Invalid ACL rule", e.getMessage());
            verify(agentManager, times(1)).easySend(anyLong(), any(RemoveACLRuleCommand.class));
        }
    }

    @Test
    public void testApplyFWRulesGivenSuccessCommandResponse() throws Exception {
        mockGetGloboNetworkVlanNumber(1L);
        mockGetGloboNetworkEnvironmentID(1L);
        mockGetACLapiHost();
        AgentManager agentManager = mockAgentManagerEasySend(true, "ACL rule created");

        element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createTCPFirewallRuleVO("10.0.0.5/24", 80, null, FirewallRule.State.Add)));
        verify(agentManager, times(1)).easySend(anyLong(), any(CreateACLRuleCommand.class));
    }

    @Test
    public void testApplyRevokedFWRulesGivenSuccessCommandResponse() throws Exception {
        mockGetGloboNetworkVlanNumber(1L);
        mockGetGloboNetworkEnvironmentID(1L);
        mockGetACLapiHost();
        AgentManager agentManager = mockAgentManagerEasySend(true, "ACL rule created");

        element.applyFWRules(createNetwork(), Arrays.asList((FirewallRule) createTCPFirewallRuleVO("10.0.0.5/24", 80, null, FirewallRule.State.Revoke)));
        verify(agentManager, times(1)).easySend(anyLong(), any(RemoveACLRuleCommand.class));
    }

    @Test
    public void testCreateACLRuleCommandGivenTCPRule() throws Exception {
        mockGetGloboNetworkVlanNumber(1L);
        mockGetGloboNetworkEnvironmentID(1L);

        CreateACLRuleCommand cmd = element.createACLRuleCommand(createNetwork(), createTCPFirewallRuleVO("10.0.0.5/24", 80, 85, FirewallRule.State.Add), CreateACLRuleCommand.class);

        assertEquals("tcp", cmd.getProtocol());
        assertEquals("10.0.0.5/24", cmd.getDestinationCidr());
        assertEquals(new Integer(80), cmd.getStartPort());
        assertEquals(new Integer(85), cmd.getEndPort());
        assertEquals(new Long(1), cmd.getVlanNumber());
        assertEquals(new Long(1), cmd.getEnvironmentId());
    }

    @Test
    public void testRemoveACLRuleCommandGivenTCPRule() throws Exception {
        mockGetGloboNetworkVlanNumber(1L);
        mockGetGloboNetworkEnvironmentID(1L);

        RemoveACLRuleCommand cmd = element.createACLRuleCommand(createNetwork(), createTCPFirewallRuleVO("10.0.0.5/24", 80, 85, FirewallRule.State.Add), RemoveACLRuleCommand.class);

        assertEquals("tcp", cmd.getProtocol());
        assertEquals("10.0.0.5/24", cmd.getDestinationCidr());
        assertEquals(new Integer(80), cmd.getStartPort());
        assertEquals(new Integer(85), cmd.getEndPort());
        assertEquals(new Long(1), cmd.getVlanNumber());
        assertEquals(new Long(1), cmd.getEnvironmentId());
    }

    @Test
    public void testCreateACLRuleCommandGivenICMPRule() throws Exception {
        mockGetGloboNetworkVlanNumber(1L);
        mockGetGloboNetworkEnvironmentID(1L);

        CreateACLRuleCommand cmd = element.createACLRuleCommand(createNetwork(), createICMPFirewallRuleVO("10.0.0.5/24", 1, 1, FirewallRule.State.Add), CreateACLRuleCommand.class);

        assertEquals("icmp", cmd.getProtocol());
        assertEquals("10.0.0.5/24", cmd.getDestinationCidr());
        assertEquals(new Integer(1), cmd.getIcmpCode());
        assertEquals(new Integer(1), cmd.getIcmpType());
        assertEquals(new Long(1), cmd.getVlanNumber());
        assertEquals(new Long(1), cmd.getEnvironmentId());
    }

    @Test
    public void testRemoveACLRuleCommandGivenICMPRule() throws Exception {
        mockGetGloboNetworkVlanNumber(1L);
        mockGetGloboNetworkEnvironmentID(1L);

        RemoveACLRuleCommand cmd = element.createACLRuleCommand(createNetwork(), createICMPFirewallRuleVO("10.0.0.5/24", 1, 1, FirewallRule.State.Add), RemoveACLRuleCommand.class);

        assertEquals("icmp", cmd.getProtocol());
        assertEquals("10.0.0.5/24", cmd.getDestinationCidr());
        assertEquals(new Integer(1), cmd.getIcmpCode());
        assertEquals(new Integer(1), cmd.getIcmpType());
        assertEquals(new Long(1), cmd.getVlanNumber());
        assertEquals(new Long(1), cmd.getEnvironmentId());
    }

    private FirewallRuleVO createTCPFirewallRuleVO(String destination, Integer portStart, Integer portEnd, FirewallRule.State state) {
        FirewallRuleVO rule = new FirewallRuleVO(
            "1", 1L, portStart, portEnd, "tcp", 1, 1, 1,
            FirewallRule.Purpose.Firewall, null, null, null, null,
            FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User
        );
        rule.setState(state);
        rule.setSourceCidrList(Arrays.asList(destination));
        return rule;
    }

    private FirewallRuleVO createICMPFirewallRuleVO(String destination, Integer icmpCode, Integer icmpType, FirewallRule.State state) {
        FirewallRuleVO rule = new FirewallRuleVO(
                "1", 1L, null, null, "icmp", 1, 1, 1,
                FirewallRule.Purpose.Firewall, null, icmpCode, icmpType, null,
                FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User
        );
        rule.setState(state);
        rule.setSourceCidrList(Arrays.asList(destination));
        return rule;
    }

    private NetworkVO createNetwork() {
        NetworkVO network = new NetworkVO(
            1L, Networks.TrafficType.Guest, Networks.Mode.None,
            Networks.BroadcastDomainType.Vlan, 1L, 1L, 1L, 1L, "bla", "fake", "eet.net",
            Network.GuestType.Shared, 1L, 1L,
            ControlledEntity.ACLType.Account, false, 1L
        );
        network.setNetworkCidr("10.10.10.0/24");
        return network;
    }

    private HostVO createMockHost() {
        return new HostVO(10L, "Host-1", Host.Type.Routing, null,
                "10.0.0.0", null, null, null, null, null, null, null, null,
                Status.Up, null, null, null, 10L, 10L, 30L, 10233, null, null,
                null, 0, null);
    }

    private void mockCheckACLProvider() {
        NetworkModel networkModel = mock(NetworkModel.class);
        when(networkModel.isProviderSupportServiceInNetwork(anyLong(), eq(Network.Service.Firewall), eq(Network.Provider.GloboAclApi))).thenReturn(true);
        element._networkManager = networkModel;
    }

    private void mockFindZone() {
        EntityManager entityManager = mock(EntityManager.class);
        DataCenter zone = new DataCenterVO(
                UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.0/24", null, null,
                DataCenter.NetworkType.Advanced, null,
                null, true, true, null, null
        );
        when(entityManager.findById(eq(DataCenter.class), anyLong())).thenReturn(zone);
        element._entityMgr = entityManager;
    }

    private void mockGetGloboNetworkVlanNumber(long vlanNum) {
        GloboNetworkManager globoNetworkService = mock(GloboNetworkManager.class);
        Vlan vlan = new Vlan();
        vlan.setVlanNum(vlanNum);
        when(globoNetworkService.getVlanInfoFromGloboNetwork(any(Network.class))).thenReturn(vlan);
        element._globoNetworkService = globoNetworkService;
    }

    private void mockGetGloboNetworkEnvironmentID(long environmentId) {
        GloboNetworkNetworkDao globoNetworkDao = mock(GloboNetworkNetworkDao.class);
        GloboNetworkNetworkVO globonetwork = new GloboNetworkNetworkVO(1L, 1L, environmentId);
        when(globoNetworkDao.findByNetworkId(any(Long.class))).thenReturn(globonetwork);
        element._globoNetworkDao = globoNetworkDao;
    }

    private AgentManager mockAgentManagerEasySend(boolean success, String details) {
        AgentManager agentManager = mock(AgentManager.class);
        when(agentManager.easySend(anyLong(), any(ACLRuleCommand.class))).thenReturn(new Answer(null, success, details));
        element._agentMgr = agentManager;
        return agentManager;
    }

    private void mockGetACLapiHost() {
        HostDao hostDao = mock(HostDao.class);
        when(hostDao.findByTypeNameAndZoneId(anyLong(), eq(Network.Provider.GloboAclApi.getName()), eq(Host.Type.L2Networking))).thenReturn(createMockHost());
        element._hostDao = hostDao;
    }
}