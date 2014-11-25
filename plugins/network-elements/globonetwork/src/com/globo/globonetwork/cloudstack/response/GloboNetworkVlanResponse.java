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
package com.globo.globonetwork.cloudstack.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.net.Ip4Address;

public class GloboNetworkVlanResponse extends Answer {

    private Long vlanId;

    private String vlanName;

    private String vlanDescription;

    private Long vlanNum;

    private Ip4Address mask;

    private Ip4Address networkAddress;

    private Long networkId;

    private boolean isActive;

    public GloboNetworkVlanResponse(Command command, Long vlanId, String vlanName, String vlanDescription, Long vlanNum, Ip4Address networkAddress, Ip4Address mask,
            Long networkId, Boolean isActive) {
        super(command, true, null);
        this.vlanId = vlanId;
        this.vlanName = vlanName;
        this.vlanDescription = vlanDescription;
        this.vlanNum = vlanNum;
        this.mask = mask;
        this.networkAddress = networkAddress;
        this.setNetworkId(networkId);
        this.setIsActive(isActive == null ? false : isActive);
    }

    public Long getVlanNum() {
        return vlanNum;
    }

    public void setVlanNum(Long vlanNum) {
        this.vlanNum = vlanNum;
    }

    public Long getVlanId() {
        return vlanId;
    }

    public void setVlanId(Long vlanId) {
        this.vlanId = vlanId;
    }

    public Ip4Address getMask() {
        return mask;
    }

    public void setMask(Ip4Address mask) {
        this.mask = mask;
    }

    public String getVlanName() {
        return vlanName;
    }

    public void setVlanName(String vlanName) {
        this.vlanName = vlanName;
    }

    public String getVlanDescription() {
        return vlanDescription;
    }

    public void setVlanDescription(String vlanDescription) {
        this.vlanDescription = vlanDescription;
    }

    public Ip4Address getNetworkAddress() {
        return networkAddress;
    }

    public void setNetworkAddress(Ip4Address networkAddress) {
        this.networkAddress = networkAddress;
    }

    public Boolean isActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

}
