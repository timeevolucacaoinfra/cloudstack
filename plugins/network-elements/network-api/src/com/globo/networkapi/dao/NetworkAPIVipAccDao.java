package com.globo.networkapi.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.globo.networkapi.NetworkAPIVipAccVO;

public interface NetworkAPIVipAccDao extends GenericDao<NetworkAPIVipAccVO, Long> {

	NetworkAPIVipAccVO findNetworkAPIVipAcct(long napiVipId, long accountId, long networkId);

	NetworkAPIVipAccVO findNetworkAPIVip(long napiVipId, long networkId);

	List<NetworkAPIVipAccVO> findByNetwork(long networkId);
	
	List<NetworkAPIVipAccVO> findByVipId(long vipId);
	
}
