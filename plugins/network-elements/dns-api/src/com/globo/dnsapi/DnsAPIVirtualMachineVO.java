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
@Table(name = "dnsapi_vm_ref")
public class DnsAPIVirtualMachineVO implements InternalIdentity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private long id;

	@Column(name="vm_id")
	private Long vmId;
	
	@Column(name="dnsapi_record_id")
	private Long dnsapiRecordId;
	
	@Column(name="dnsapi_reverse_record_id")
	private Long dnsapiReverseRecordId;

	public DnsAPIVirtualMachineVO() {
	}

	public DnsAPIVirtualMachineVO(Long vmId, Long dnsapiRecordId, Long dnsapiReverseRecordId) {
		this.vmId = vmId;
		this.dnsapiRecordId = dnsapiRecordId;
		this.dnsapiReverseRecordId = dnsapiReverseRecordId;
	}

	@Override
	public long getId() {
		return id;
	}

	public Long getVirtualMachineId() {
		return vmId;
	}
	
	public void setVirtualMachineId(Long vmId) {
		this.vmId = vmId;
	}
	
	public Long getDnsapiRecordId() {
		return dnsapiRecordId;
	}
	
	public void setDnsapiRecordId(Long dnsapiRecordId) {
		this.dnsapiRecordId = dnsapiRecordId;
	}
	
	public Long getDnsapiReverseRecordId() {
		return dnsapiReverseRecordId;
	}
	
	public void setDnsapiReverseRecordId(Long dnsapiReverseRecordId) {
		this.dnsapiReverseRecordId = dnsapiReverseRecordId;
	}
}
