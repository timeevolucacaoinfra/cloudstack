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

import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceVO;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticSearchAutoScaleStatsCollector extends AutoScaleStatsCollector implements Configurable{

    protected TransportClient elasticSearchClient;

    private static final ConfigKey<String> ElasticSearchHost = new ConfigKey<>("Advanced", String.class, "autoscale.elasticsearch.host", null,
            "Elastic search server host name", true, ConfigKey.Scope.Global);

    private static final ConfigKey<Integer> ElasticSearchPort = new ConfigKey<>("Advanced", Integer.class, "autoscale.elasticsearch.port", null,
            "Elastic search server transport module port", true, ConfigKey.Scope.Global);

    private static final ConfigKey<String> ElasticSearchClusterName = new ConfigKey<>("Advanced", String.class, "autoscale.elasticsearch.cluster", null,
            "Elastic search server cluster name", true, ConfigKey.Scope.Global);

    private static final ConfigKey<String> ElasticSearchIndexName = new ConfigKey<>("Advanced", String.class, "autoscale.elasticsearch.index", null,
            "Elastic search index name", true, ConfigKey.Scope.Global);

    public static final Logger s_logger = Logger.getLogger(ElasticSearchAutoScaleStatsCollector.class.getName());

    public ElasticSearchAutoScaleStatsCollector(){
        buildConnection();
    }

    @Override
    public Map<String, Double> retrieveMetrics(AutoScaleVmGroup asGroup, List<VMInstanceVO> vmList) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("[AutoScale] Collecting ElasticSearch's data.");
        }

        if(vmList == null || vmList.size() == 0 || !connectionIsSet()) {
            return null;
        }

        List<String> hostNames = new ArrayList<>();
        for(VMInstanceVO vm : vmList){
            hostNames.add(vm.getHostName());
        }

        Map<String, Double> avgSummary = new HashMap<>();
        List<Pair<String, Integer>> counterNameAndDuration = this.getPairOfCounterNameAndDuration(asGroup);

        try {
            for (Pair<String, Integer> counter : counterNameAndDuration) {
                String counterName = counter.first().split(",")[0];
                Integer duration = counter.second();

                SearchResponse response = elasticSearchClient.prepareSearch(ElasticSearchIndexName.value())
                .setTypes(counterName)
                .setFrom(0).setSize(0)
                .setQuery(QueryBuilders.filteredQuery(
                        QueryBuilders.matchAllQuery(),
                        FilterBuilders.andFilter(
                            FilterBuilders.rangeFilter("@timestamp").from("now-" + duration + "s/s").to("now"),
                            FilterBuilders.termsFilter("hostname.raw", hostNames)
                        )
                    )
                )
                .addAggregation(AggregationBuilders.avg("counter_average").field("value"))
                .execute()
                .actionGet();

                s_logger.debug("[AutoScale] Elasticsearch response: " + response.toString());

                Double avg = ((InternalAvg) response.getAggregations().asMap().get("counter_average")).value();
                avgSummary.put(counterName, (avg >= 0.0 ? avg : null));
            }
        }catch (RuntimeException ex){
            s_logger.error("[AutoScale] Error while reading AutoScale group " + asGroup.getId() + " Stats", ex);
        }
        return avgSummary;
    }

    private boolean connectionIsSet() {
        if(elasticSearchClient == null){
            buildConnection();
        }
        return elasticSearchClient != null;
    }

    private void buildConnection(){
        if(ElasticSearchHost.value() != null && ElasticSearchPort.value() != null && ElasticSearchClusterName.value() != null){
            Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", ElasticSearchClusterName.value()).build();
            this.elasticSearchClient = new TransportClient(settings);
            elasticSearchClient.addTransportAddress(new InetSocketTransportAddress(ElasticSearchHost.value(), ElasticSearchPort.value()));
        }
    }

    @Override
    public String getConfigComponentName() {
        return ElasticSearchAutoScaleStatsCollector.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ ElasticSearchPort, ElasticSearchHost, ElasticSearchClusterName, ElasticSearchIndexName };
    }
}
