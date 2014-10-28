/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.globo.globonetwork.cloudstack.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.globo.globonetwork.cloudstack.GloboNetworkLBNetworkVO;

@Component
@Local(value = GloboNetworkLBNetworkDao.class)
@DB
public class GloboNetworkLBNetworkDaoImpl extends GenericDaoBase<GloboNetworkLBNetworkVO, Long> implements GloboNetworkLBNetworkDao {

    final SearchBuilder<GloboNetworkLBNetworkVO> byNetworkEnvironmentRefId;
    
    final SearchBuilder<GloboNetworkLBNetworkVO> byNetworkEnvironmentRefIdAndLBNetworkId;

    final SearchBuilder<GloboNetworkLBNetworkVO> byNetworkEnvironmentRefIdAndPortableIpRangeId;

    protected GloboNetworkLBNetworkDaoImpl() {
        byNetworkEnvironmentRefId = createSearchBuilder();
        byNetworkEnvironmentRefId.and("globonetwork_environment_ref_id", byNetworkEnvironmentRefId.entity().getGloboNetworkEnvironmentRefId(), Op.EQ);
        byNetworkEnvironmentRefId.done();
        
        byNetworkEnvironmentRefIdAndLBNetworkId = createSearchBuilder();
        byNetworkEnvironmentRefIdAndLBNetworkId.and("globonetwork_environment_ref_id", byNetworkEnvironmentRefIdAndLBNetworkId.entity().getGloboNetworkEnvironmentRefId(), Op.EQ);
        byNetworkEnvironmentRefIdAndLBNetworkId.and("globonetwork_lb_network_id", byNetworkEnvironmentRefIdAndLBNetworkId.entity().getGloboNetworkLBNetworkId(), Op.EQ);
        byNetworkEnvironmentRefIdAndLBNetworkId.done();        

        byNetworkEnvironmentRefIdAndPortableIpRangeId = createSearchBuilder();
        byNetworkEnvironmentRefIdAndPortableIpRangeId.and("globonetwork_environment_ref_id", byNetworkEnvironmentRefIdAndPortableIpRangeId.entity().getGloboNetworkEnvironmentRefId(), Op.EQ);
        byNetworkEnvironmentRefIdAndPortableIpRangeId.and("portable_ip_range_id", byNetworkEnvironmentRefIdAndPortableIpRangeId.entity().getPortableIpRangeId(), Op.EQ);
        byNetworkEnvironmentRefIdAndPortableIpRangeId.done();        

    }

    @Override
    public List<GloboNetworkLBNetworkVO> listByEnvironmentRefId(long globoNetworkEnvironmentRefId) {
        SearchCriteria<GloboNetworkLBNetworkVO> sc = byNetworkEnvironmentRefId.create();
        sc.setParameters("globonetwork_environment_ref_id", globoNetworkEnvironmentRefId);
        return listBy(sc);
    }

    @Override
    public GloboNetworkLBNetworkVO findByEnvironmentRefAndLBNetwork(long globoNetworkEnvironmentRefId, long globoNetworkLBNetworkId) {
        SearchCriteria<GloboNetworkLBNetworkVO> sc = byNetworkEnvironmentRefIdAndLBNetworkId.create();
        sc.setParameters("globonetwork_environment_ref_id", globoNetworkEnvironmentRefId);
        sc.setParameters("globonetwork_lb_network_id", globoNetworkLBNetworkId);
        return findOneBy(sc);
    }

    @Override
    public GloboNetworkLBNetworkVO findByEnvironmentRefAndPortableIpRangeId(long globoNetworkEnvironmentRefId, long vlanId) {
        SearchCriteria<GloboNetworkLBNetworkVO> sc = byNetworkEnvironmentRefIdAndLBNetworkId.create();
        sc.setParameters("globonetwork_environment_ref_id", globoNetworkEnvironmentRefId);
        sc.setParameters("portable_ip_range_id", vlanId);
        return findOneBy(sc);
    }

}
