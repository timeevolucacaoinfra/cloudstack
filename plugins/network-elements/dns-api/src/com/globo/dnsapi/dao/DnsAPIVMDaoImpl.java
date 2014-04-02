package com.globo.dnsapi.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.dnsapi.DnsAPIVMVO;

@Component
@Local(value=DnsAPIVMDao.class) @DB(txn=false)
public class DnsAPIVMDaoImpl extends GenericDaoBase<DnsAPIVMVO, Long> implements DnsAPIVMDao {

	final SearchBuilder<DnsAPIVMVO> vmIdSearch;

	protected DnsAPIVMDaoImpl() {
		super();

		vmIdSearch = createSearchBuilder();
		vmIdSearch.and("vm_id", vmIdSearch.entity().getVMId(), Op.EQ);
		vmIdSearch.done();
     }

	@Override
	public DnsAPIVMVO findByVMId(long vmId) {
		SearchCriteria<DnsAPIVMVO> sc = vmIdSearch.create();
		sc.setParameters("vm_id", vmId);
		return findOneBy(sc);
	}
 
}
