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

public class GloboNetworkVipEnvironmentResponse extends Answer {

	private Long vipEnvironmentid;
	private String finality;
	private String client;
	private String environmentName;

	public GloboNetworkVipEnvironmentResponse(Command command, Long vipEnvironmentId, String finality, String client, String environmentName) {
		super(command, true, null);
		this.vipEnvironmentid = vipEnvironmentId;
		this.finality = finality;
		this.client = client;
		this.environmentName = environmentName;
	}

    public Long getVipEnvironmentid() {
        return vipEnvironmentid;
    }

    public void setVipEnvironmentid(Long vipEnvironmentid) {
        this.vipEnvironmentid = vipEnvironmentid;
    }

    public String getFinality() {
        return finality;
    }

    public void setFinality(String finality) {
        this.finality = finality;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

}
