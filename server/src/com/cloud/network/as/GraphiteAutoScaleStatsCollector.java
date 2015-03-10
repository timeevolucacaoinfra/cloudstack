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
package com.cloud.network.as;

import com.cloud.vm.VMInstanceVO;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphiteAutoScaleStatsCollector extends AutoScaleStatsCollector {

    //@Inject
    private GraphiteClient graphiteClient;

    public static final Logger s_logger = Logger.getLogger(GraphiteAutoScaleStatsCollector.class.getName());


    @Override
    public Map<String, Double> retrieveMetrics(AutoScaleVmGroup asGroup, List<VMInstanceVO> vmList) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("[AutoScale] Collecting Graphite's data.");
        }
        try {
            Map<String, Double> params = new HashMap<>();


            return params;
        }catch(Exception e){
            s_logger.error("[AutoScale] Error while reading AutoScale group " + asGroup.getId() + " Stats", e);
        }
        return null;
    }
}
