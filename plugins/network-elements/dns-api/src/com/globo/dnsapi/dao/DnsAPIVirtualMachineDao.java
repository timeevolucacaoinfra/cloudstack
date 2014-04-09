package com.globo.dnsapi.dao;

import com.cloud.utils.db.GenericDao;
import com.globo.dnsapi.DnsAPIVirtualMachineVO;

public interface DnsAPIVirtualMachineDao extends GenericDao<DnsAPIVirtualMachineVO, Long> {
	
	public DnsAPIVirtualMachineVO findByVirtualMachineIdAndDomainId(Long vmId, Long dnsapiDomainId);

}
