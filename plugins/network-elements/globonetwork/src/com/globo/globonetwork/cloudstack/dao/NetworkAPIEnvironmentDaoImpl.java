package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.globonetwork.cloudstack.NetworkAPIEnvironmentVO;

@Component
@Local(value = NetworkAPINetworkDao.class)
@DB
public class NetworkAPIEnvironmentDaoImpl extends
		GenericDaoBase<NetworkAPIEnvironmentVO, Long> implements
		NetworkAPIEnvironmentDao {

	final SearchBuilder<NetworkAPIEnvironmentVO> physicalNetworkIdSearch;
	final SearchBuilder<NetworkAPIEnvironmentVO> physicalNetworkIdAndEnvironmentIdSearch;

	protected NetworkAPIEnvironmentDaoImpl() {
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
	public List<NetworkAPIEnvironmentVO> listByPhysicalNetworkId(long physicalNetworkId) {
		SearchCriteria<NetworkAPIEnvironmentVO> sc = physicalNetworkIdSearch.create();
		sc.setParameters("physical_network_id", physicalNetworkId);
		return listBy(sc);
	}

	@Override
	public NetworkAPIEnvironmentVO findByPhysicalNetworkIdAndEnvironmentId(
			long physicalNetworkId, long environmentId) {
		SearchCriteria<NetworkAPIEnvironmentVO> sc = physicalNetworkIdAndEnvironmentIdSearch.create();
		sc.setParameters("physical_network_id", physicalNetworkId);
		sc.setParameters("napi_environment_id", environmentId);
		return findOneBy(sc);
	}

}
