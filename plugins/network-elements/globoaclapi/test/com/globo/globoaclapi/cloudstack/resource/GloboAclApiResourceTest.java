package com.globo.globoaclapi.cloudstack.resource;

import com.cloud.agent.api.Answer;
import com.globo.aclapi.client.AclAPIException;
import com.globo.aclapi.client.ClientAclAPI;
import com.globo.aclapi.client.api.RuleAPI;
import com.globo.aclapi.client.model.L4Option;
import com.globo.aclapi.client.model.Rule;
import com.globo.globoaclapi.cloudstack.commands.CreateACLRuleCommand;
import com.globo.globoaclapi.cloudstack.commands.RemoveACLRuleCommand;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GloboAclApiResourceTest {

    private GloboAclApiResource globoAclApiResource;

    @Before
    public void setUp() throws Exception {
        globoAclApiResource = spy(new GloboAclApiResource());
    }

    @Test
    public void testCreateTcpACLRuleAndNoEndPort(){
        CreateACLRuleCommand cmd = new CreateACLRuleCommand();
        cmd.setProtocol("tcp");
        cmd.setDestinationCidr("10.1.1.0/24");
        cmd.setSourceCidr("192.168.1.0/24");
        cmd.setStartPort(80);

        Rule rule = globoAclApiResource.createRule(cmd);

        assertEquals("TCP", rule.getProtocol().name());
        assertEquals("10.1.1.0/24", rule.getDestination());
        assertEquals("192.168.1.0/24", rule.getSource());
        assertEquals(new Integer(80), rule.getL4Options().getDestPortStart());
        assertEquals("eq", rule.getL4Options().getDestPortOperation());
    }

    @Test
    public void testCreateTcpACLRuleWithEqualStartAndEndPorts(){
        CreateACLRuleCommand cmd = new CreateACLRuleCommand();
        cmd.setProtocol("tcp");
        cmd.setDestinationCidr("10.1.1.0/24");
        cmd.setSourceCidr("192.168.1.0/24");
        cmd.setStartPort(80);
        cmd.setEndPort(80);

        Rule rule = globoAclApiResource.createRule(cmd);

        assertEquals("TCP", rule.getProtocol().name());
        assertEquals("10.1.1.0/24", rule.getDestination());
        assertEquals("192.168.1.0/24", rule.getSource());
        assertEquals(new Integer(80), rule.getL4Options().getDestPortStart());
        assertEquals("eq", rule.getL4Options().getDestPortOperation());
    }

    @Test
    public void testCreateTcpACLRule(){
        CreateACLRuleCommand cmd = new CreateACLRuleCommand();
        cmd.setProtocol("tcp");
        cmd.setDestinationCidr("10.1.1.0/24");
        cmd.setSourceCidr("192.168.1.0/24");
        cmd.setStartPort(80);
        cmd.setEndPort(88);

        Rule rule = globoAclApiResource.createRule(cmd);

        assertEquals("TCP", rule.getProtocol().name());
        assertEquals("10.1.1.0/24", rule.getDestination());
        assertEquals("192.168.1.0/24", rule.getSource());
        assertEquals(new Integer(80), rule.getL4Options().getDestPortStart());
        assertEquals(new Integer(88), rule.getL4Options().getDestPortEnd());
        assertEquals("range", rule.getL4Options().getDestPortOperation());
    }

    @Test
    public void testCreateIcmpACLRule(){
        CreateACLRuleCommand cmd = new CreateACLRuleCommand();
        cmd.setProtocol("icmp");
        cmd.setDestinationCidr("10.1.1.0/24");
        cmd.setSourceCidr("192.168.1.0/24");
        cmd.setIcmpCode(1);
        cmd.setIcmpType(3);

        Rule rule = globoAclApiResource.createRule(cmd);

        assertEquals("ICMP", rule.getProtocol().name());
        assertEquals("10.1.1.0/24", rule.getDestination());
        assertEquals("192.168.1.0/24", rule.getSource());
        assertEquals(new Integer(1), rule.getIcmpOptions().getCode());
        assertEquals(new Integer(3), rule.getIcmpOptions().getType());
    }

    @Test
    public void testGetRuleIdGivenNoRulesFound(){
        RuleAPI ruleAPI = mockAclApiClientListByEnvAndVlan(new ArrayList<Rule>());
        RemoveACLRuleCommand cmd = new RemoveACLRuleCommand();
        cmd.setProtocol("tcp");
        cmd.setVlanNumber(1L);
        cmd.setEnvironmentId(1L);

        assertNull(globoAclApiResource.getRuleId(cmd));
        verify(ruleAPI, times(1)).listByEnvAndNumVlan(cmd.getEnvironmentId(), cmd.getVlanNumber());
    }

    @Test
    public void testGetRuleIdGivenMatchingRuleFound(){
        Rule rule = new Rule();
        rule.setId("1");
        rule.setAction(Rule.Action.PERMIT);
        rule.setProtocol("tcp");
        rule.setDestination("10.1.1.0/24");
        rule.setSource("192.168.1.0/24");
        L4Option l4Options = new L4Option();
        l4Options.setDestPortStart(80);
        l4Options.setDestPortEnd(88);
        l4Options.setDestPortOperation("range");
        rule.setL4Options(l4Options);

        RuleAPI ruleAPI = mockAclApiClientListByEnvAndVlan(Arrays.asList(rule));
        RemoveACLRuleCommand cmd = createRemoveCmd();

        assertEquals(new Long(rule.getId()), globoAclApiResource.getRuleId(cmd));
        verify(ruleAPI, times(1)).listByEnvAndNumVlan(cmd.getEnvironmentId(), cmd.getVlanNumber());
    }

    @Test
    public void testCreateACLGivenApiError(){
        RuleAPI ruleAPI = mockACLClientApiSaveSync(new AclAPIException("error"));

        Answer answer = globoAclApiResource.executeRequest(createCreateCmd());
        assertFalse(answer.getResult());
        assertEquals("error", answer.getDetails());
        verify(ruleAPI, times(1)).saveSync(eq(1L), eq(1L), any(Rule.class), anyString());
    }

    @Test
    public void testCreateACLGivenRuleSuccessfullyCreated(){
        RuleAPI ruleAPI = mockACLClientApiSaveSync(new Rule());

        Answer answer = globoAclApiResource.executeRequest(createCreateCmd());
        assertTrue(answer.getResult());
        verify(ruleAPI, times(1)).saveSync(eq(1L), eq(1L), any(Rule.class), anyString());
    }

    @Test
    public void testRemoveACLGivenApiError(){
        RemoveACLRuleCommand cmd = createRemoveCmd();
        RuleAPI ruleAPI = mockACLClientApiRemoveSync(new AclAPIException("error"), cmd);

        Answer answer = globoAclApiResource.executeRequest(cmd);
        assertFalse(answer.getResult());
        assertEquals("error", answer.getDetails());
        verify(ruleAPI, times(1)).removeSync(eq(1L), eq(1L), any(Long.class), anyString());
    }

    @Test
    public void testRemoveACLGivenRuleSuccessfullyRemoved(){
        RemoveACLRuleCommand cmd = createRemoveCmd();
        RuleAPI ruleAPI = mockACLClientApiRemoveSync(null, cmd);

        Answer answer = globoAclApiResource.executeRequest(cmd);
        assertTrue(answer.getResult());
        verify(ruleAPI, times(1)).listByEnvAndNumVlan(cmd.getEnvironmentId(), cmd.getVlanNumber());
        verify(ruleAPI, times(1)).removeSync(eq(1L), eq(1L), eq(1L), anyString());
    }

    private RuleAPI mockACLClientApiRemoveSync(Object response, RemoveACLRuleCommand cmd) {
        ClientAclAPI aclAPI = mock(ClientAclAPI.class);
        RuleAPI ruleAPI = mock(RuleAPI.class);
        when(aclAPI.getAclAPI()).thenReturn(ruleAPI);
        if(response instanceof AclAPIException) {
            doThrow((AclAPIException)response).when(ruleAPI).removeSync(anyLong(), anyLong(), any(Long.class), anyString());
        }
        if(cmd != null) {
            Rule rule = globoAclApiResource.createRule(cmd);
            rule.setId("1");
            when(ruleAPI.listByEnvAndNumVlan(anyLong(), anyLong())).thenReturn(Arrays.asList(rule));
        }
        globoAclApiResource._aclApiClient = aclAPI;
        return ruleAPI;
    }

    private RuleAPI mockACLClientApiSaveSync(Object response) {
        ClientAclAPI aclAPI = mock(ClientAclAPI.class);
        RuleAPI ruleAPI = mock(RuleAPI.class);
        when(aclAPI.getAclAPI()).thenReturn(ruleAPI);
        if(response instanceof Rule) {
            when(ruleAPI.saveSync(anyLong(), anyLong(), any(Rule.class), anyString())).thenReturn((Rule) response);
        }else{
            when(ruleAPI.saveSync(anyLong(), anyLong(), any(Rule.class), anyString())).thenThrow((AclAPIException) response);
        }
        globoAclApiResource._aclApiClient = aclAPI;
        return ruleAPI;
    }

    private RuleAPI mockAclApiClientListByEnvAndVlan(List<Rule> rules) {
        ClientAclAPI aclAPI = mock(ClientAclAPI.class);
        RuleAPI ruleAPI = mock(RuleAPI.class);
        when(aclAPI.getAclAPI()).thenReturn(ruleAPI);
        when(ruleAPI.listByEnvAndNumVlan(anyLong(), anyLong())).thenReturn(rules);
        globoAclApiResource._aclApiClient = aclAPI;
        return ruleAPI;
    }

    private CreateACLRuleCommand createCreateCmd() {
        CreateACLRuleCommand cmd = new CreateACLRuleCommand();
        cmd.setProtocol("tcp");
        cmd.setDestinationCidr("10.1.1.0/24");
        cmd.setSourceCidr("192.168.1.0/24");
        cmd.setStartPort(80);
        cmd.setEndPort(88);
        cmd.setVlanNumber(1L);
        cmd.setEnvironmentId(1L);
        return cmd;
    }

    private RemoveACLRuleCommand createRemoveCmd() {
        RemoveACLRuleCommand cmd = new RemoveACLRuleCommand();
        cmd.setProtocol("tcp");
        cmd.setDestinationCidr("10.1.1.0/24");
        cmd.setSourceCidr("192.168.1.0/24");
        cmd.setStartPort(80);
        cmd.setEndPort(88);
        cmd.setVlanNumber(1L);
        cmd.setEnvironmentId(1L);
        return cmd;
    }
}