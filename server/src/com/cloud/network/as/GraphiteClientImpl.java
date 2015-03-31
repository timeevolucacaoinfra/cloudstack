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
import com.google.gson.Gson;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphiteClientImpl implements GraphiteClient, Configurable {

    protected HttpClient httpClient;

    public static final Logger s_logger = Logger.getLogger(GraphiteClientImpl.class.getName());

    private static final ConfigKey<String> GraphiteEndpoint = new ConfigKey<>("Advanced", String.class, "autoscale.graphite.endpoint", "", "Graphite's endpoint to be used on auto scale metrics gathering", true, ConfigKey.Scope.Global);
    private static final ConfigKey<Integer> GraphiteTimeout = new ConfigKey<>("Advanced", Integer.class, "autoscale.graphite.timeout", "2000", "Graphite's connection timeout", true, ConfigKey.Scope.Global);
    private static final ConfigKey<Integer> GraphiteSoTimeout = new ConfigKey<>("Advanced", Integer.class, "autoscale.graphite.sotimeout", "2000", "Graphite's socket timeout", true, ConfigKey.Scope.Global);

    public GraphiteClientImpl(){}

    public GraphiteClientImpl(HttpClient httpClient) {
        this.httpClient = httpClient;
        HttpParams params = httpClient.getParams();
        Integer connectionTimeout = GraphiteTimeout.value();
        Integer connectionSocketTimeout = GraphiteSoTimeout.value();
        if(connectionTimeout != null && !"".equals(connectionTimeout)) {
            HttpConnectionParams.setConnectionTimeout(params, GraphiteTimeout.value());
        }
        if(connectionSocketTimeout != null && !"".equals(connectionSocketTimeout)) {
            HttpConnectionParams.setSoTimeout(params, GraphiteSoTimeout.value());
        }
    }

    public Map<String, GraphiteResult[]> fetchData(Map<String, Pair<List<String>, Integer>> countersAndTargets){
        Map<String, GraphiteResult[]> result = new HashMap<>();
        try {
            for(String counterName : countersAndTargets.keySet()){
                Pair<List<String>, Integer> targets = countersAndTargets.get(counterName);
                List<String> targetKeys = targets.first();
                Integer duration = targets.second();

                String url = GraphiteEndpoint.value() + "?format=json&from=-" + duration +"s";
                for(String targetKey : targetKeys){
                    url += "&target=" + targetKey;
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("[Graphite] Collecting Graphite's data: " + url);
                }
                HttpGet get = new HttpGet(url);
                HttpResponse response = httpClient.execute(get);

                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK || response.getEntity() == null) {
                    throw new Exception("[Graphite] Request to Graphite API return invalid status or empty content. Status code: " + response.getStatusLine().getStatusCode());
                }
                String jsonResponse = EntityUtils.toString(response.getEntity());

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("[Graphite] Data returned from graphite: \n" + jsonResponse);
                }

                GraphiteResult[] graphiteResult = new Gson().fromJson(jsonResponse, GraphiteResult[].class);
                result.put(counterName, graphiteResult);
            }
        } catch (Exception e) {
            s_logger.error("[Graphite] Error while reading from Graphite API", e);
        }
        return result;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { GraphiteEndpoint, GraphiteSoTimeout, GraphiteTimeout };
    }

    @Override
    public String getConfigComponentName() {
        return GraphiteClientImpl.class.getSimpleName();
    }
}

class GraphiteResult{

    private String target;
    private List<List<Number>> datapoints;

    public GraphiteResult(String target, List<List<Number>> datapoints) {
        this.target = target;
        this.datapoints = datapoints;
    }

    public String getTargetName(){
        return this.target;
    }

    public List<List<Number>> getDataPoints(){
        return this.datapoints;
    }

    public Double getAverage(){
        Double counterSum = 0.0;
        Integer points = 0;
        for(List<Number> dataPoint : this.getDataPoints()){
            if(dataPoint.get(0) != null) {
                points++;
                counterSum += dataPoint.get(0).doubleValue();
            }
        }
        return counterSum / points;
    }
}