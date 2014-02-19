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

	final SearchBuilder<NetworkAPIEnvironmentVO> zoneIdSearch;

	protected NetworkAPIEnvironmentDaoImpl() {
		super();

		zoneIdSearch = createSearchBuilder();
		zoneIdSearch.and("zone_id", zoneIdSearch.entity().getZoneId(), Op.EQ);
		zoneIdSearch.done();
	}

	@Override
	public List<NetworkAPIEnvironmentVO> findByZoneId(long zoneId) {
		SearchCriteria<NetworkAPIEnvironmentVO> sc = zoneIdSearch.create();
		sc.setParameters("zone_id", zoneId);
		return listBy(sc);
	}

}
