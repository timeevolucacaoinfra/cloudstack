package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.globonetwork.cloudstack.GloboNetworkVipAccVO;

public interface GloboNetworkVipAccDao extends GenericDao<GloboNetworkVipAccVO, Long> {

	GloboNetworkVipAccVO findNetworkAPIVipAcct(long napiVipId, long accountId, long networkId);
	
	List<GloboNetworkVipAccVO> listByNetworks(List<Long> networkIdList);

	GloboNetworkVipAccVO findNetworkAPIVip(long napiVipId, long networkId);

	List<GloboNetworkVipAccVO> findByNetwork(long networkId);
	
	List<GloboNetworkVipAccVO> findByVipId(long vipId);
	
}
