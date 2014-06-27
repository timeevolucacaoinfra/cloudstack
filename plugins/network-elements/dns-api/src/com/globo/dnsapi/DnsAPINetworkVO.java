package com.globo.dnsapi;
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


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "dnsapi_network_ref")
public class DnsAPINetworkVO implements InternalIdentity {

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "network_id")
	private long networkId;
	
	@Column(name = "dnsapi_domain_id")
	private long dnsapiDomainId;
	
	@Column(name = "dnsapi_reverse_domain_id")
	private long dnsapiReverseDomainId;

	public DnsAPINetworkVO() {
	}

	public DnsAPINetworkVO(long networkId, long dnsapiDomainId, long dnsapiReverseDomainId) {
		this.networkId = networkId;
		this.dnsapiDomainId = dnsapiDomainId;
		this.dnsapiReverseDomainId = dnsapiReverseDomainId;
	}

	public long getId() {
		return id;
	}

	public long getNetworkId() {
		return networkId;
	}
	
	public long getDnsapiDomainId() {
		return dnsapiDomainId;
	}
	
	public long getDnsapiReverseDomainId() {
		return dnsapiReverseDomainId;
	}
}
