package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.globonetwork.cloudstack.GloboNetworkVipAccVO;

public interface GloboNetworkVipAccDao extends GenericDao<GloboNetworkVipAccVO, Long> {

	GloboNetworkVipAccVO findGloboNetworkVipAcc(long vipId, long accountId, long networkId);
	
	List<GloboNetworkVipAccVO> listByNetworks(List<Long> networkIdList);

	GloboNetworkVipAccVO findGloboNetworkVip(long vipId, long networkId);

	List<GloboNetworkVipAccVO> findByNetwork(long networkId);
	
	List<GloboNetworkVipAccVO> findByVipId(long vipId);
	
}
