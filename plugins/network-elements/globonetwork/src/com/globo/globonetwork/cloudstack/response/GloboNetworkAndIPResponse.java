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
import com.cloud.utils.net.Ip;

public class GloboNetworkAndIPResponse extends Answer {

    // Vlan information
    private Long vlanId;
    
    private String vlanName;
    
    private String vlanDescription;

    private Integer vlanNum;

    // Network information
    private Long networkId;
    
    private String networkAddress;
    
    private String networkMask;

    private String networkBroadcast;
    
    private String networkGateway;
    
    private String networkCidr;

    private boolean isActive;
    
    // Ip information (optional)
    private Long ipId;
    
    private Ip ip;
    
    public GloboNetworkAndIPResponse(Command command) {
        super(command, true, null);
    }
    
    public GloboNetworkAndIPResponse(Command command, Long vlanId, String vlanName, String vlanDescription, Integer vlanNum, boolean isActive,
            Long networkId, String networkAddress, String networkMask, String networkBroadcast, String networkGateway, String networkCidr,
            Long ipId, Ip ip) {
    	this(command);
    	this.vlanId = vlanId;
    	this.vlanName = vlanName;
    	this.vlanDescription = vlanDescription;
    	this.vlanNum = vlanNum;
    	this.networkAddress = networkAddress;
    	this.networkMask = networkMask;
    	this.networkBroadcast = networkBroadcast;
    	this.networkGateway = networkGateway;
    	this.networkCidr = networkCidr;
    	this.networkId = networkId;
    	this.isActive = isActive;
    	this.ipId = ipId;
    	this.ip = ip;
    }

    public Long getVlanId() {
        return vlanId;
    }

    public void setVlanId(Long vlanId) {
        this.vlanId = vlanId;
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

    public Integer getVlanNum() {
        return vlanNum;
    }

    public void setVlanNum(Integer vlanNum) {
        this.vlanNum = vlanNum;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getNetworkAddress() {
        return networkAddress;
    }

    public void setNetworkAddress(String networkAddress) {
        this.networkAddress = networkAddress;
    }

    public String getNetworkBroadcast() {
        return networkBroadcast;
    }

    public void setNetworkBroadcast(String networkBroadcast) {
        this.networkBroadcast = networkBroadcast;
    }

    public Long getIpId() {
        return ipId;
    }

    public void setIpId(Long ipId) {
        this.ipId = ipId;
    }

    public Ip getIp() {
        return ip;
    }

    public void setIp(Ip ip) {
        this.ip = ip;
    }

    public String getNetworkGateway() {
        return networkGateway;
    }

    public void setNetworkGateway(String networkGateway) {
        this.networkGateway = networkGateway;
    }

    public String getNetworkMask() {
        return networkMask;
    }

    public void setNetworkMask(String networkMask) {
        this.networkMask = networkMask;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }
    
}
