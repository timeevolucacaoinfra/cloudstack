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
@Local(value=DnsAPIVirtualMachineDao.class) @DB(txn=false)
public class DnsAPIVirtualMachineDaoImpl extends GenericDaoBase<DnsAPIVirtualMachineVO, Long> implements DnsAPIVirtualMachineDao {
	
	protected final SearchBuilder<DnsAPIVirtualMachineVO> VmSearch;
	
	public DnsAPIVirtualMachineDaoImpl() {
		VmSearch = createSearchBuilder();
		VmSearch.and("vmId", VmSearch.entity().getVirtualMachineId(), Op.EQ);
		VmSearch.done();
	}
	
	@Override
	public DnsAPIVirtualMachineVO findByVirtualMachineId(Long vmId) {
		SearchCriteria<DnsAPIVirtualMachineVO> sc = VmSearch.create();
		sc.setParameters("vmId", vmId);
		return findOneBy(sc);
	}
}
