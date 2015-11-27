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
package com.globo.globoaclapi.cloudstack.response;

import com.cloud.agent.api.Answer;
import com.cloud.network.rules.FirewallRuleVO;

import java.util.ArrayList;
import java.util.List;

public class GloboACLRulesResponse extends Answer {

    protected List<FirewallRuleVO> rules;

    public GloboACLRulesResponse(){
        super();
    }

    public GloboACLRulesResponse(List<FirewallRuleVO> rules) {
        this.result = true;
        this.setRules(rules);
        if(!rules.isEmpty()) {
            this.rules = rules;
        }
    }

    public void setRules(List<FirewallRuleVO> rules) {
        List<FirewallRuleVO> r = new ArrayList<>();
        for(FirewallRuleVO rule: rules){
            if(rule != null){
                r.add(rule);
            }
        }
        this.rules = r;
    }

    public List<FirewallRuleVO> getRules() {
        return rules;
    }
}
