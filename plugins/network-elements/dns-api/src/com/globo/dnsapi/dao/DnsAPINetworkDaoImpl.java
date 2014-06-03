package com.globo.dnsapi.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.dnsapi.DnsAPINetworkVO;

@Component
@Local(value=DnsAPINetworkDao.class) @DB(txn=false)
public class DnsAPINetworkDaoImpl extends GenericDaoBase<DnsAPINetworkVO, Long> implements DnsAPINetworkDao {

	final SearchBuilder<DnsAPINetworkVO> networkIdSearch;

	protected DnsAPINetworkDaoImpl() {
		super();

		networkIdSearch = createSearchBuilder();
		networkIdSearch.and("network_id", networkIdSearch.entity().getNetworkId(), Op.EQ);
		networkIdSearch.done();
     }

	@Override
	public DnsAPINetworkVO findByNetworkId(long networkId) {
		SearchCriteria<DnsAPINetworkVO> sc = networkIdSearch.create();
		sc.setParameters("network_id", networkId);
		return findOneBy(sc);
	}
 
}
