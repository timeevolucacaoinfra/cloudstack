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
package com.globo.globonetwork.cloudstack.commands;

import java.util.List;

import com.cloud.agent.api.Command;

public class AddVipInGloboNetworkCommand extends Command {
	
	private String ipv4;
	
	private String methodBal;
	
	private Long vipEnvironmentId;
	
	private String businessArea;
	
	private String host;
	
	private String serviceName;
	
	private String healthcheckType;
	
	private List<String> ports;
	
	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public String getIpv4() {
		return ipv4;
	}
	
	public void setIpv4(String ipv4) {
		this.ipv4 = ipv4;
	}

    public String getMethodBal() {
        return methodBal;
    }

    public void setMethodBal(String methodBal) {
        this.methodBal = methodBal;
    }
    
    public Long getVipEnvironmentId() {
        return vipEnvironmentId;
    }
    
    public void setVipEnvironmentId(Long vipEnvironmentId) {
        this.vipEnvironmentId = vipEnvironmentId;
    }

    public String getBusinessArea() {
        return businessArea;
    }

    public void setBusinessArea(String businessArea) {
        this.businessArea = businessArea;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getHealthcheckType() {
        return healthcheckType;
    }

    public void setHealthcheckType(String healthcheckType) {
        this.healthcheckType = healthcheckType;
    }

    public List<String> getPorts() {
        return ports;
    }
    
    public void setPorts(List<String> ports) {
        this.ports = ports;
    }
}
