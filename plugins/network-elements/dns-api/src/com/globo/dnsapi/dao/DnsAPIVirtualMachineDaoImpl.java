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
package com.globo.dnsapi.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.dnsapi.DnsAPIVirtualMachineVO;

@Component
@Local(value=DnsAPIVirtualMachineDao.class) @DB
public class DnsAPIVirtualMachineDaoImpl extends GenericDaoBase<DnsAPIVirtualMachineVO, Long> implements DnsAPIVirtualMachineDao {
	
	protected final SearchBuilder<DnsAPIVirtualMachineVO> VmAndDomainSearch;
	
	public DnsAPIVirtualMachineDaoImpl() {
		VmAndDomainSearch = createSearchBuilder();
		VmAndDomainSearch.and("vmId", VmAndDomainSearch.entity().getVirtualMachineId(), Op.EQ);
		VmAndDomainSearch.and("domainId", VmAndDomainSearch.entity().getDnsapiDomainId(), Op.EQ);
		VmAndDomainSearch.done();
	}
	
	@Override
	public DnsAPIVirtualMachineVO findByVirtualMachineIdAndDomainId(Long vmId, Long dnsapiDomainId) {
		SearchCriteria<DnsAPIVirtualMachineVO> sc = VmAndDomainSearch.create();
		sc.addAnd("vmId", Op.EQ, vmId);
//		sc.addAnd("domainId", Op.EQ, dnsapiDomainId);
//		sc.setParameters("vmId", vmId);
		sc.setParameters("domainId", dnsapiDomainId);
		return findOneBy(sc);
	}
}
