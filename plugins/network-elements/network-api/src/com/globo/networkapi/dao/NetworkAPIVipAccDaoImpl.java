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
@DB
public class NetworkAPIVipAccDaoImpl extends
		GenericDaoBase<NetworkAPIVipAccVO, Long> implements
		NetworkAPIVipAccDao {

	final SearchBuilder<NetworkAPIVipAccVO> allParamsSearch;
	
	final SearchBuilder<NetworkAPIVipAccVO> networkSearch;

	final SearchBuilder<NetworkAPIVipAccVO> byNetwork;

	final SearchBuilder<NetworkAPIVipAccVO> byVip;
	
	final SearchBuilder<NetworkAPIVipAccVO> byNetworkAndVip;

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
		
		byVip = createSearchBuilder();
		byVip.and("napi_vip_id", byVip.entity().getNapiVipId(), Op.EQ);
		byVip.done();
		
		byNetworkAndVip = createSearchBuilder();
		byNetworkAndVip.and("network_id", byNetworkAndVip.entity().getNetworkId(), Op.EQ);
		byNetworkAndVip.and("napi_vip_id", byNetworkAndVip.entity().getNapiVipId(), Op.EQ);
		byNetworkAndVip.done();
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

	@Override
	public NetworkAPIVipAccVO findNetworkAPIVip(long napiVipId, long networkId) {
		SearchCriteria<NetworkAPIVipAccVO> sc = byNetworkAndVip.create();
		sc.setParameters("napi_vip_id", napiVipId);
		sc.setParameters("network_id", networkId);
		return findOneBy(sc);
	}

	@Override
	public List<NetworkAPIVipAccVO> findByVipId(long vipId) {
		SearchCriteria<NetworkAPIVipAccVO> sc = byVip.create();
		sc.setParameters("napi_vip_id", vipId);
		return listBy(sc);
	}

}
