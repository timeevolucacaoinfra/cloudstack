// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.query.dao;

import com.cloud.api.query.vo.GloboVmVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import java.util.List;
import javax.ejb.Local;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Local(value = {GloboVmDao.class})
public class GloboVmDaoImpl extends GenericDaoBase<GloboVmVO, Long> implements GloboVmDao {
    public static final Logger s_logger = Logger.getLogger(GloboVmDaoImpl.class);

    protected GloboVmDaoImpl() {


    }

    @Override
    public Pair<List<GloboVmVO>, Integer> list(long projectId) {
        Filter searchFilter = new Filter(GloboVmVO.class, "displayName", Boolean.TRUE, 0l, 1000l);

        SearchBuilder<GloboVmVO> searchBuilder = createSearchBuilder();
        searchBuilder.and("projectId", searchBuilder.entity().getProjectId(), SearchCriteria.Op.EQ);

        searchBuilder.done();

        SearchCriteria<GloboVmVO> globoVmVOSearchCriteria = searchBuilder.create();

        globoVmVOSearchCriteria.setParameters("projectId", projectId);

        Pair<List<GloboVmVO>, Integer> listIntegerPair = searchAndCount(globoVmVOSearchCriteria, searchFilter);

        return listIntegerPair;
    }
}
