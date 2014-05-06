package com.globo.networkapi.dao;

import com.cloud.utils.db.GenericDao;
import com.globo.networkapi.NetworkAPIVipAccVO;

public interface NetworkAPIVipAccDao extends GenericDao<NetworkAPIVipAccVO, Long> {

	NetworkAPIVipAccVO findNetworkAPIVipAcct(long napiVipId, long accountId, long networkId);

}
