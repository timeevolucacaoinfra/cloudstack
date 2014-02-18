package com.globo.networkapi.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.networkapi.NetworkAPINetworkVO;

@Component
@Local(value=NetworkAPINetworkDao.class) @DB(txn=false)
public class NetworkAPINetworkDaoImpl extends GenericDaoBase<NetworkAPINetworkVO, Long> implements NetworkAPINetworkDao {

 final SearchBuilder<NetworkAPINetworkVO> networkIdSearch;

 protected NetworkAPINetworkDaoImpl() {
     super();

     networkIdSearch = createSearchBuilder();
     networkIdSearch.and("network_id", networkIdSearch.entity().getNetworkId(), Op.EQ);
     networkIdSearch.done();
 }

 @Override
 public NetworkAPINetworkVO findByNetworkId(long networkId) {
     SearchCriteria<NetworkAPINetworkVO> sc = networkIdSearch.create();
     sc.setParameters("network_id", networkId);
     return findOneBy(sc);
 }

}
