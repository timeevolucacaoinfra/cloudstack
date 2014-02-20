package com.globo.networkapi.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.networkapi.NetworkAPIEnvironmentVO;

@Component
@Local(value = NetworkAPINetworkDao.class)
@DB(txn = false)
public class NetworkAPIEnvironmentDaoImpl extends
		GenericDaoBase<NetworkAPIEnvironmentVO, Long> implements
		NetworkAPIEnvironmentDao {

	final SearchBuilder<NetworkAPIEnvironmentVO> physicalNetworkIdSearch;

	protected NetworkAPIEnvironmentDaoImpl() {
		super();

		physicalNetworkIdSearch = createSearchBuilder();
		physicalNetworkIdSearch.and("physical_network_id", physicalNetworkIdSearch.entity().getPhysicalNetworkId(), Op.EQ);
		physicalNetworkIdSearch.done();
	}

	@Override
	public List<NetworkAPIEnvironmentVO> findByPhysicalNetworkId(long physicalNetworkId) {
		SearchCriteria<NetworkAPIEnvironmentVO> sc = physicalNetworkIdSearch.create();
		sc.setParameters("physical_network_id", physicalNetworkId);
		return listBy(sc);
	}

}
