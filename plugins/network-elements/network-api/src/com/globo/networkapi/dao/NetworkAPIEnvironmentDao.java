package com.globo.networkapi.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.networkapi.NetworkAPIEnvironmentVO;

public interface NetworkAPIEnvironmentDao extends GenericDao<NetworkAPIEnvironmentVO, Long> {

	List<NetworkAPIEnvironmentVO> findByPhysicalNetworkId(long physicalNetworkId);

}
