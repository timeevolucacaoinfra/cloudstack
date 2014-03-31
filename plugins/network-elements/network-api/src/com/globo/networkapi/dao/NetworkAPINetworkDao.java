package com.globo.networkapi.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.networkapi.NetworkAPINetworkVO;

public interface NetworkAPINetworkDao extends GenericDao<NetworkAPINetworkVO, Long> {

	NetworkAPINetworkVO findByNetworkId(long networkId);

	List<NetworkAPINetworkVO> listByEnvironmentId(long napiEnvironmentId);
}
