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
package com.globo.globoaclapi.cloudstack.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.globo.aclapi.client.AclAPIException;
import com.globo.aclapi.client.ClientAclAPI;
import com.globo.aclapi.client.model.ICMPOption;
import com.globo.aclapi.client.model.L4Option;
import com.globo.aclapi.client.model.Rule;
import com.globo.globoaclapi.cloudstack.commands.CreateACLRuleCommand;
import com.globo.globoaclapi.cloudstack.commands.ListACLRulesCommand;
import com.globo.globoaclapi.cloudstack.commands.RemoveACLRuleCommand;
import com.globo.globoaclapi.cloudstack.response.GloboACLRulesResponse;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GloboAclApiResource extends ManagerBase implements ServerResource {

    private String _zoneId;

    private String _guid;

    private String _name;

    protected ClientAclAPI _aclApiClient;

    private static final Logger s_logger = Logger.getLogger(GloboAclApiResource.class);

    @Override
    public StartupCommand[] initialize() {
        s_logger.trace("initialize called");
        StartupCommand cmd = new StartupCommand(getType());
        cmd.setName(_name);
        cmd.setGuid(_guid);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress("");
        cmd.setStorageIpAddress("");
        cmd.setVersion(GloboAclApiResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {cmd};
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _zoneId = (String)params.get("zoneId");
        if (_zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find guid");
        }

        _name = (String)params.get("name");
        if (_name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        if (params.get("url") == null) {
            throw new ConfigurationException("Unable to find url");
        }

        if (params.get("username") == null) {
            throw new ConfigurationException("Unable to find username");
        }

        if (params.get("password") == null) {
            throw new ConfigurationException("Unable to find password");
        }

        _aclApiClient = createACLApiClient(params);

        return true;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return new MaintainAnswer((MaintainCommand)cmd);
        }else if(cmd instanceof CreateACLRuleCommand){
            return execute((CreateACLRuleCommand) cmd);
        }else if(cmd instanceof RemoveACLRuleCommand){
            return execute((RemoveACLRuleCommand) cmd);
        }else if(cmd instanceof ListACLRulesCommand){
            return execute((ListACLRulesCommand) cmd);
        }
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    private Answer execute(ListACLRulesCommand cmd) {
        try{
            List<Rule> rules = _aclApiClient.getAclAPI().listByEnvAndNumVlan(cmd.getEnvironmentId(), cmd.getVlanNumber());
            List<GloboACLRulesResponse.ACLRule> aclRules = new ArrayList<>();
            for(Rule r : rules) {
                aclRules.add(createACLRuleResponse(r));
            }
            return new GloboACLRulesResponse(aclRules);
        }catch(AclAPIException e){
            s_logger.error("Error while listing ACL Rules.", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private Answer execute(CreateACLRuleCommand cmd) {
        try{
            Rule rule = _aclApiClient.getAclAPI().saveSync(cmd.getEnvironmentId(), cmd.getVlanNumber(), createRule(cmd), cmd.getAclOwner());
            return new Answer(cmd, true, "ACL Rule " + rule.getId() + " successfully created.");
        }catch(AclAPIException e){
            s_logger.error("Error while creating ACL Rule: " + cmd.getAclRuleDescription(), e);
            if(e.getHttpStatus() != null && e.getHttpStatus() == 403){
                return new Answer(cmd, false, "The user " + cmd.getAclOwner() + " don't have the required permission to perform this action.");
            }else if(e.getCause() instanceof ConnectTimeoutException){
                return new Answer(cmd, false, "ACL creation timed out. Your request may still being processed by ACL API.");
            } else {
                return new Answer(cmd, false, e.getMessage());
            }
        }
    }

    private Answer execute(RemoveACLRuleCommand cmd) {
        try {
            _aclApiClient.getAclAPI().removeSync(cmd.getEnvironmentId(), cmd.getVlanNumber(), cmd.getRuleId(), cmd.getAclOwner());
            return new Answer(cmd, true, "ACL Rule " + cmd.getRuleId() + " successfully removed.");
        }catch(AclAPIException e){
            s_logger.error("Error while removing ACL Rule: " + cmd.getRuleId(), e);
            if(e.getHttpStatus() == 403){
                return new Answer(cmd, false, "The user " + cmd.getAclOwner() + " doesn't have the required permission to perform this action.");
            }else if(e.getCause() instanceof ConnectTimeoutException){
                return new Answer(cmd, false, "ACL removal timed out. Your request may still being processed by ACL API.");
            } else {
                return new Answer(cmd, false, e.getMessage());
            }
        }
    }

    protected Rule createRule(CreateACLRuleCommand cmd) {
        Rule rule = new Rule();
        rule.setAction(Rule.Action.PERMIT);
        rule.setProtocol(cmd.getProtocol());
        rule.setDestination(cmd.getDestinationCidr());
        rule.setSource(cmd.getSourceCidr());

        if(cmd.getProtocol().equals("icmp")){
            rule.setIcmpOptions(new ICMPOption(cmd.getIcmpType(), cmd.getIcmpCode()));
        }else{
            L4Option l4Option = new L4Option();
            l4Option.setDestPortStart(cmd.getStartPort());
            if(cmd.getEndPort() == null || cmd.getEndPort().equals(cmd.getStartPort())){
                l4Option.setDestPortOperation("eq");
            }else{
                l4Option.setDestPortOperation("range");
                l4Option.setDestPortEnd(cmd.getEndPort());
            }
            rule.setL4Options(l4Option);
        }
        return rule;
    }

    private GloboACLRulesResponse.ACLRule createACLRuleResponse(Rule r) {
        GloboACLRulesResponse.ACLRule rule = new GloboACLRulesResponse.ACLRule();

        Integer destPortStart = r.getL4Options() != null ? r.getL4Options().getDestPortStart() : null;
        Integer destPortEnd = r.getL4Options() != null ? r.getL4Options().getDestPortEnd() : null;
        Integer code = r.getIcmpOptions() != null ? r.getIcmpOptions().getCode() : null;
        Integer type =  r.getIcmpOptions() != null ? r.getIcmpOptions().getType() : null;

        rule.setId(r.getId());
        rule.setProtocol(r.getProtocol().name().toLowerCase());
        rule.setDestination(r.getDestination());
        rule.setIcmpType(type);
        rule.setIcmpCode(code);
        rule.setPortStart(destPortStart);
        rule.setPortEnd(destPortEnd);

        return rule;
    }

    private ClientAclAPI createACLApiClient(Map<String, Object> params) {
        if(_aclApiClient == null){
            String url = (String) params.get("url");
            String username = (String) params.get("username");
            Integer timeout = new Integer((String) params.get("timeout"));
            String password = (String) params.get("password");
            boolean verifySSL = params.get("trustssl").equals("true");
            return ClientAclAPI.buildHttpAPI(url, username, password, timeout, verifySSL);
        }
        return _aclApiClient;
    }

    @Override
    public Host.Type getType() {
        return Host.Type.L2Networking;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(getType(), id);
    }

    @Override
    public void disconnected() { }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) { }
}
