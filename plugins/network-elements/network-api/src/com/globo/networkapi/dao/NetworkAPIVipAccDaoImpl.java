package com.globo.networkapi.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.networkapi.NetworkAPIVipAccVO;

@Component
@Local(value = NetworkAPIVipAccDao.class)
@DB(txn = false)
public class NetworkAPIVipAccDaoImpl extends
		GenericDaoBase<NetworkAPIVipAccVO, Long> implements
		NetworkAPIVipAccDao {

	final SearchBuilder<NetworkAPIVipAccVO> allParamsSearch;

	protected NetworkAPIVipAccDaoImpl() {
		super();

		allParamsSearch = createSearchBuilder();
		allParamsSearch.and("napi_vip_id", allParamsSearch.entity().getNapiVipId(), Op.EQ);
		allParamsSearch.and("account_id", allParamsSearch.entity().getAccountId(), Op.EQ);
		allParamsSearch.and("network_id", allParamsSearch.entity().getNetworkId(), Op.EQ);
		allParamsSearch.done();
	}

	@Override
	public NetworkAPIVipAccVO findNetworkAPIVipAcct(long napiVipId,
			long accountId, long networkId) {
		SearchCriteria<NetworkAPIVipAccVO> sc = allParamsSearch.create();
		sc.setParameters("napi_vip_id", napiVipId);
		sc.setParameters("account_id", accountId);
		sc.setParameters("network_id", networkId);
		return findOneBy(sc);
	}

}
