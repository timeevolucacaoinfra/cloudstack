package com.globo.networkapi.dao;

import java.util.List;

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
	
	final SearchBuilder<NetworkAPIVipAccVO> networkSearch;

	final SearchBuilder<NetworkAPIVipAccVO> byNetwork;

	protected NetworkAPIVipAccDaoImpl() {
		super();

		allParamsSearch = createSearchBuilder();
		allParamsSearch.and("napi_vip_id", allParamsSearch.entity().getNapiVipId(), Op.EQ);
		allParamsSearch.and("account_id", allParamsSearch.entity().getAccountId(), Op.EQ);
		allParamsSearch.and("network_id", allParamsSearch.entity().getNetworkId(), Op.EQ);
		allParamsSearch.done();
		
		networkSearch = createSearchBuilder();
		networkSearch.and("network_id", networkSearch.entity().getNetworkId(), Op.IN);
		networkSearch.done();

		byNetwork = createSearchBuilder();
		byNetwork.and("network_id", byNetwork.entity().getNetworkId(), Op.EQ);
		byNetwork.done();
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
	
	public List<NetworkAPIVipAccVO> listByNetworks(List<Long> networkIdList) {
		SearchCriteria<NetworkAPIVipAccVO> sc = networkSearch.create();
		sc.setParameters("network_id", networkIdList.toArray(new Object[networkIdList.size()]));
		return listBy(sc);
	}

	@Override
	public List<NetworkAPIVipAccVO> findByNetwork(long networkId) {
		SearchCriteria<NetworkAPIVipAccVO> sc = byNetwork.create();
		sc.setParameters("network_id", networkId);
		return listBy(sc);
	}

}
