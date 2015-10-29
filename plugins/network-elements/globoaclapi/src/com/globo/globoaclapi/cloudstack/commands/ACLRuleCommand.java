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

public abstract class ACLRuleCommand extends Command {

    private String destinationCidr;

    private String sourceCidr;

    private Integer startPort;

    private Integer endPort;

    private String protocol;

    private Integer icmpCode;

    private Integer icmpType;

    private Long vlanNumber;

    private Long environmentId;

    private String aclOwner;

    public String getDestinationCidr() {
        return destinationCidr;
    }

    public void setDestinationCidr(String destinationCidr) {
        this.destinationCidr = destinationCidr;
    }

    public String getSourceCidr() {
        return sourceCidr;
    }

    public void setSourceCidr(String sourceCidr) {
        this.sourceCidr = sourceCidr;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public void setStartPort(Integer startPort) {
        this.startPort = startPort;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public void setEndPort(Integer endPort) {
        this.endPort = endPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public void setIcmpCode(Integer icmpCode) {
        this.icmpCode = icmpCode;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public void setIcmpType(Integer icmpType) {
        this.icmpType = icmpType;
    }

    public void setVlanNumber(Long vlanNumber) {
        this.vlanNumber = vlanNumber;
    }

    public Long getVlanNumber() {
        return vlanNumber;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }


    public Long getEnvironmentId() {
        return environmentId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getAclRuleDescription() {
        return "CreateACLRuleCommand{" +
                "destinationCidr='" + destinationCidr + '\'' +
                ", sourceCidr='" + sourceCidr + '\'' +
                ", startPort=" + startPort +
                ", endPort=" + endPort +
                ", protocol='" + protocol + '\'' +
                ", icmpCode=" + icmpCode +
                ", icmpType=" + icmpType +
                ", vlanNumber=" + vlanNumber +
                ", environmentId=" + environmentId +
                '}';
    }

    public void setAclOwner(String aclOwner) {
        this.aclOwner = aclOwner;
    }

    public String getAclOwner() {
        return aclOwner;
    }
}
