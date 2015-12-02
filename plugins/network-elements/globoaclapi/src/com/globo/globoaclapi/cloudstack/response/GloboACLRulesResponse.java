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

import java.util.List;

public class GloboACLRulesResponse extends Answer {

    protected List<ACLRule> rules;

    public GloboACLRulesResponse(){
        super();
    }

    public GloboACLRulesResponse(List<ACLRule> rules) {
        this.result = true;
        this.setRules(rules);
    }

    public List<ACLRule> getRules() {
        return rules;
    }

    public void setRules(List<ACLRule> rules) {
        this.rules = rules;
    }

    public static class ACLRule {

        private String id;

        private String protocol;

        private String destination;

        private Integer icmpType;

        private Integer icmpCode;

        private Integer destPortStart;

        private Integer destPortEnd;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public Integer getIcmpType() {
            return icmpType;
        }

        public void setIcmpType(Integer icmpType) {
            this.icmpType = icmpType;
        }

        public Integer getIcmpCode() {
            return icmpCode;
        }

        public void setIcmpCode(Integer icmpCode) {
            this.icmpCode = icmpCode;
        }

        public Integer getPortStart() {
            return destPortStart;
        }

        public void setPortStart(Integer destPortStart) {
            this.destPortStart = destPortStart;
        }

        public Integer getPortEnd() {
            return destPortEnd;
        }

        public void setPortEnd(Integer destPortEnd) {
            this.destPortEnd = destPortEnd;
        }
    }
}