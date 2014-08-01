package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.globonetwork.cloudstack.NetworkAPIVipAccVO;

public interface NetworkAPIVipAccDao extends GenericDao<NetworkAPIVipAccVO, Long> {

	NetworkAPIVipAccVO findNetworkAPIVipAcct(long napiVipId, long accountId, long networkId);
	
	List<NetworkAPIVipAccVO> listByNetworks(List<Long> networkIdList);

	NetworkAPIVipAccVO findNetworkAPIVip(long napiVipId, long networkId);

	List<NetworkAPIVipAccVO> findByNetwork(long networkId);
	
	List<NetworkAPIVipAccVO> findByVipId(long vipId);
	
}
