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

import com.cloud.agent.api.Command;

public class GetVipEnvironmentFromGloboNetworkCommand extends Command {
	
	private Long vipEnvironmentId;
	
	private String finality;
	
	private String client;
	
	private String environmentName;
	
	public GetVipEnvironmentFromGloboNetworkCommand(Long vipEnvironmentId, String finality, String client, String environmentName) {
	    this.vipEnvironmentId = vipEnvironmentId;
	    this.finality = finality;
	    this.client = client;
	    this.environmentName = environmentName;
	}

	@Override
	public boolean executeInSequence() {
		return false;
	}
    
    public Long getVipEnvironmentId() {
        return this.vipEnvironmentId;
    }
    
    public String getFinality() {
        return this.finality;
    }
    
    public String getClient() {
        return this.client;
    }
    
    public String getEnvironmentName() {
        return this.environmentName;
    }
}
