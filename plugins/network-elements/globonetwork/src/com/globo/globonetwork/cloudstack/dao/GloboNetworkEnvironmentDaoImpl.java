package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.globonetwork.cloudstack.GloboNetworkEnvironmentVO;

@Component
@Local(value = GloboNetworkNetworkDao.class)
@DB
public class GloboNetworkEnvironmentDaoImpl extends
		GenericDaoBase<GloboNetworkEnvironmentVO, Long> implements
		GloboNetworkEnvironmentDao {

	final SearchBuilder<GloboNetworkEnvironmentVO> physicalNetworkIdSearch;
	final SearchBuilder<GloboNetworkEnvironmentVO> physicalNetworkIdAndEnvironmentIdSearch;

	protected GloboNetworkEnvironmentDaoImpl() {
		super();

		physicalNetworkIdSearch = createSearchBuilder();
		physicalNetworkIdSearch.and("physical_network_id", physicalNetworkIdSearch.entity().getPhysicalNetworkId(), Op.EQ);
		physicalNetworkIdSearch.done();

		physicalNetworkIdAndEnvironmentIdSearch = createSearchBuilder();
		physicalNetworkIdAndEnvironmentIdSearch.and("physical_network_id", physicalNetworkIdAndEnvironmentIdSearch.entity().getPhysicalNetworkId(), Op.EQ);
		physicalNetworkIdAndEnvironmentIdSearch.and("napi_environment_id", physicalNetworkIdAndEnvironmentIdSearch.entity().getNapiEnvironmentId(), Op.EQ);
		physicalNetworkIdAndEnvironmentIdSearch.done();

	}

	@Override
	public List<GloboNetworkEnvironmentVO> listByPhysicalNetworkId(long physicalNetworkId) {
		SearchCriteria<GloboNetworkEnvironmentVO> sc = physicalNetworkIdSearch.create();
		sc.setParameters("physical_network_id", physicalNetworkId);
		return listBy(sc);
	}

	@Override
	public GloboNetworkEnvironmentVO findByPhysicalNetworkIdAndEnvironmentId(
			long physicalNetworkId, long environmentId) {
		SearchCriteria<GloboNetworkEnvironmentVO> sc = physicalNetworkIdAndEnvironmentIdSearch.create();
		sc.setParameters("physical_network_id", physicalNetworkId);
		sc.setParameters("napi_environment_id", environmentId);
		return findOneBy(sc);
	}

}
