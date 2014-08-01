package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.globonetwork.cloudstack.GloboNetworkEnvironmentVO;

public interface GloboNetworkEnvironmentDao extends GenericDao<GloboNetworkEnvironmentVO, Long> {

	List<GloboNetworkEnvironmentVO> listByPhysicalNetworkId(long physicalNetworkId);

	GloboNetworkEnvironmentVO findByPhysicalNetworkIdAndEnvironmentId(long physicalNetworkId, long environmentId);

}
