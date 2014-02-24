package com.globo.networkapi.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.networkapi.NetworkAPIEnvironmentVO;

public interface NetworkAPIEnvironmentDao extends GenericDao<NetworkAPIEnvironmentVO, Long> {

	List<NetworkAPIEnvironmentVO> listByPhysicalNetworkId(long physicalNetworkId);

	NetworkAPIEnvironmentVO findByPhysicalNetworkIdAndEnvironmentId(long physicalNetworkId, long environmentId);

}
