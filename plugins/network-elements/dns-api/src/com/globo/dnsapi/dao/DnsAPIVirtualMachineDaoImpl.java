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
