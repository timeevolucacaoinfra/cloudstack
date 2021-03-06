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
package com.globo.globoaclapi.cloudstack.commands;

import com.cloud.agent.api.Command;

public class RemoveACLRuleCommand extends Command {

    private Long ruleId;

    private Long environmentId;

    private Long vlanNumber;

    private String aclOwner;

    public RemoveACLRuleCommand(Long ruleId, Long environmentId, Long vlanNumber, String aclOwner) {
        this.ruleId = ruleId;
        this.environmentId = environmentId;
        this.vlanNumber = vlanNumber;
        this.aclOwner = aclOwner;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public Long getVlanNumber() {
        return vlanNumber;
    }

    public String getAclOwner() {
        return aclOwner;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}