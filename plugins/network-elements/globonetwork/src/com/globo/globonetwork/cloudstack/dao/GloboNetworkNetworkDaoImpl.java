package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.globonetwork.cloudstack.GloboNetworkNetworkVO;

@Component
@Local(value=GloboNetworkNetworkDao.class) @DB
public class GloboNetworkNetworkDaoImpl extends GenericDaoBase<GloboNetworkNetworkVO, Long> implements GloboNetworkNetworkDao {

 final SearchBuilder<GloboNetworkNetworkVO> networkIdSearch;
 final SearchBuilder<GloboNetworkNetworkVO> environmentIdSearch;

 protected GloboNetworkNetworkDaoImpl() {
     super();

     networkIdSearch = createSearchBuilder();
     networkIdSearch.and("network_id", networkIdSearch.entity().getNetworkId(), Op.EQ);
     networkIdSearch.done();
     
     environmentIdSearch = createSearchBuilder();
     environmentIdSearch.and("napi_environment_id", environmentIdSearch.entity().getNapiEnvironmentId(), Op.EQ);
     environmentIdSearch.done();
 }

 @Override
 public GloboNetworkNetworkVO findByNetworkId(long networkId) {
     SearchCriteria<GloboNetworkNetworkVO> sc = networkIdSearch.create();
     sc.setParameters("network_id", networkId);
     return findOneBy(sc);
 }
 
 @Override
 public List<GloboNetworkNetworkVO> listByEnvironmentId(long napiEnvironmentId) {
	 SearchCriteria<GloboNetworkNetworkVO> sc = environmentIdSearch.create();
	 sc.setParameters("napi_environment_id", napiEnvironmentId);
	 return listBy(sc);
 }

}
