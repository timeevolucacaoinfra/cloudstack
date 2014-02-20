//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package com.globo.networkapi.response;

import org.apache.cloudstack.api.ApiConstants;

import com.cloud.serializer.Param;
import com.globo.networkapi.NetworkAPIEnvironmentVO;
import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = NetworkAPIEnvironmentVO.class)
public class NetworkAPIEnvironmentResponse extends BaseResponse {

	@SerializedName("id") @Param(description = "id of the NetworkAPI Environment ref")
	private Long id;

	@SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description = "the physicalNetworkId of networkapi environment belongs to")
	private String physicalNetworkId;

	@SerializedName(ApiConstants.NAME) @Param(description = "name of the provider")
	private String name;

	@SerializedName("napienvironmentid") @Param(description = "id of environment in networkapi")
	private Long napiEnvironmentId;

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPhysicalNetworkId(String physicalNetworkId) {
		this.physicalNetworkId = physicalNetworkId;
	}

	public void setNapiEnvironmentId(Long napiEnvironmentId) {
		this.napiEnvironmentId = napiEnvironmentId;
	}

}
