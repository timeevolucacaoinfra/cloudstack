package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.globonetwork.cloudstack.GloboNetworkNetworkVO;

public interface GloboNetworkNetworkDao extends GenericDao<GloboNetworkNetworkVO, Long> {

	GloboNetworkNetworkVO findByNetworkId(long networkId);

	List<GloboNetworkNetworkVO> listByEnvironmentId(long napiEnvironmentId);
}
