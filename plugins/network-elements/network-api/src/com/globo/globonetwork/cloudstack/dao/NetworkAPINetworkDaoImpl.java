package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.globonetwork.cloudstack.NetworkAPINetworkVO;

@Component
@Local(value=NetworkAPINetworkDao.class) @DB
public class NetworkAPINetworkDaoImpl extends GenericDaoBase<NetworkAPINetworkVO, Long> implements NetworkAPINetworkDao {

 final SearchBuilder<NetworkAPINetworkVO> networkIdSearch;
 final SearchBuilder<NetworkAPINetworkVO> environmentIdSearch;

 protected NetworkAPINetworkDaoImpl() {
     super();

     networkIdSearch = createSearchBuilder();
     networkIdSearch.and("network_id", networkIdSearch.entity().getNetworkId(), Op.EQ);
     networkIdSearch.done();
     
     environmentIdSearch = createSearchBuilder();
     environmentIdSearch.and("napi_environment_id", environmentIdSearch.entity().getNapiEnvironmentId(), Op.EQ);
     environmentIdSearch.done();
 }

 @Override
 public NetworkAPINetworkVO findByNetworkId(long networkId) {
     SearchCriteria<NetworkAPINetworkVO> sc = networkIdSearch.create();
     sc.setParameters("network_id", networkId);
     return findOneBy(sc);
 }
 
 @Override
 public List<NetworkAPINetworkVO> listByEnvironmentId(long napiEnvironmentId) {
	 SearchCriteria<NetworkAPINetworkVO> sc = environmentIdSearch.create();
	 sc.setParameters("napi_environment_id", napiEnvironmentId);
	 return listBy(sc);
 }

}
