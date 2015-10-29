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
import com.globo.globoaclapi.cloudstack.commands.ACLRuleCommand;
import com.globo.globoaclapi.cloudstack.commands.CreateACLRuleCommand;
import com.globo.globoaclapi.cloudstack.commands.RemoveACLRuleCommand;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

public class GloboAclApiResource extends ManagerBase implements ServerResource {

    private static final Logger s_logger = Logger.getLogger(GloboAclApiResource.class);

    private String _zoneId;

    private String _guid;

    private String _name;

    protected ClientAclAPI _aclApiClient;

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

        _aclApiClient = createAclApiClient(params);

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
        }
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    private Answer execute(CreateACLRuleCommand cmd) {
        try{
            Rule rule = _aclApiClient.getAclAPI().saveSync(cmd.getEnvironmentId(), cmd.getVlanNumber(), createRule(cmd), cmd.getAclOwner());
            return new Answer(cmd, true, "ACL Rule " + rule.getId() + " successfully created.");
        }catch(AclAPIException e){
            s_logger.error("Error while creating ACL Rule: " + cmd.getAclRuleDescription(), e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private Answer execute(RemoveACLRuleCommand cmd) {
        try {
            Long ruleId = getRuleId(cmd);
            if(ruleId != null){
                _aclApiClient.getAclAPI().removeSync(cmd.getEnvironmentId(), cmd.getVlanNumber(), ruleId, cmd.getAclOwner());
            }
            return new Answer(cmd, true, "ACL Rule " + ruleId + " successfully removed.");
        }catch(AclAPIException e){
            s_logger.error("Error while removing ACL Rule: " + cmd.getAclRuleDescription(), e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    protected Long getRuleId(RemoveACLRuleCommand cmd) {
        Rule ruleToBeRemoved = createRule(cmd);
        List<Rule> rules = _aclApiClient.getAclAPI().listByEnvAndNumVlan(cmd.getEnvironmentId(), cmd.getVlanNumber());
        Long ruleId = null;
        for(Rule rule : rules){
            if(rule.equals(ruleToBeRemoved)){
                ruleId = new Long(rule.getId());
                break;
            }
        }
        return ruleId;
    }

    protected Rule createRule(ACLRuleCommand cmd) {
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
            if(cmd.getEndPort() != null){
                l4Option.setDestPortOperation("range");
                l4Option.setDestPortEnd(cmd.getEndPort());
            }else{
                l4Option.setDestPortOperation("eq");
            }
            rule.setL4Options(l4Option);
        }

        return rule;
    }

    private ClientAclAPI createAclApiClient(Map<String, Object> params) {
        if(_aclApiClient == null){
            return ClientAclAPI.buildHttpAPI((String) params.get("url"), (String) params.get("username"), (String) params.get("password"));
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
