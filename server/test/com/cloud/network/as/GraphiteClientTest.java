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
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphiteClientTest {

    private GraphiteClientImpl graphiteClient;

    @Before
    public void setUp(){
        graphiteClient = new GraphiteClientImpl();
    }

    @Test
    public void testFetchData(){
        mockHttpClient("[{\"target\": \"stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu\", \"datapoints\": [[2.0, 1426185120], [1.0, 1426185130]]}]", 200);

        Map<String, Pair<List<String>, Integer>> countersAndTargets = new HashMap<>();

        countersAndTargets.put("cpu", new Pair<>(Arrays.asList(new String[]{ "stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu" }), 60));
        Map<String, GraphiteResult[]> result  = graphiteClient.fetchData(countersAndTargets);

        assert result.keySet().size() == 1;
        assert result.get("cpu").length == 1;
        assert result.get("cpu")[0].getDataPoints().size() == 2;
        assert result.get("cpu")[0].getAverage() == 1.5;
    }


    @Test
    public void testFetchDataWithMoreThanOneTarget(){
        mockHttpClient("[{\"target\": \"stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu\", \"datapoints\": [[2.0, 1426185120], [1.0, 1426185130]]}, " +
                "{\"target\": \"stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu\", \"datapoints\": [[2.0, 1426185120], [1.0, 1426185130]]}]", 200);

        Map<String, Pair<List<String>, Integer>> countersAndTargets = new HashMap<>();

        countersAndTargets.put("cpu", new Pair<>(Arrays.asList(new String[]{ "stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu" }), 60));
        Map<String, GraphiteResult[]> result  = graphiteClient.fetchData(countersAndTargets);

        assert result.keySet().size() == 1;
        assert result.get("cpu").length == 2;
    }

    @Test
    public void testFetchDataWithMoreThanOneCounter(){
        mockHttpClient("[{\"target\": \"stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu\", \"datapoints\": [[2.0, 1426185120], [1.0, 1426185130]]}]", 200);

        Map<String, Pair<List<String>, Integer>> countersAndTargets = new HashMap<>();

        countersAndTargets.put("cpu", new Pair<>(Arrays.asList(new String[]{ "stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu" }), 60));
        countersAndTargets.put("memory", new Pair<>(Arrays.asList(new String[]{ "stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.memory" }), 60));
        Map<String, GraphiteResult[]> result  = graphiteClient.fetchData(countersAndTargets);

        assert result.keySet().size() == 2;
    }

    @Test
    public void testFetchDataWithStatusCode500(){
        mockHttpClient("", 500);

        Map<String, Pair<List<String>, Integer>> countersAndTargets = new HashMap<>();

        countersAndTargets.put("cpu", new Pair<>(Arrays.asList(new String[]{ "stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu" }), 60));
        countersAndTargets.put("memory", new Pair<>(Arrays.asList(new String[]{ "stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.memory" }), 60));
        Map<String, GraphiteResult[]> result  = graphiteClient.fetchData(countersAndTargets);

        assert  result.keySet().size() == 0;
    }

    @Test
    public void testFetchDataWithEmptyResponse(){
        mockHttpClient(null, 200);

        Map<String, Pair<List<String>, Integer>> countersAndTargets = new HashMap<>();

        countersAndTargets.put("cpu", new Pair<>(Arrays.asList(new String[]{ "stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.cpu" }), 60));
        countersAndTargets.put("memory", new Pair<>(Arrays.asList(new String[]{ "stats.gauges.vm-a47dbd63-ca98-4f1d-9f0c-4b1ea4f77f32.memory" }), 60));
        Map<String, GraphiteResult[]> result  = graphiteClient.fetchData(countersAndTargets);

        assert result.keySet().size() == 0;
    }

    @Test
    public void testFetchDataWithError(){
        graphiteClient.httpClient = null;

        Map<String, GraphiteResult[]> result  = graphiteClient.fetchData(null);

        assert result.keySet().size() == 0;
    }

    private void mockHttpClient(String response, Integer statusCode){
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        BasicHttpEntity entity = new BasicHttpEntity();
        if(response != null) {
            entity.setContent(new ByteArrayInputStream(response.getBytes()));
        }
        try {
            when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("https", 1, 1), statusCode, "OK"));
            when(httpResponse.getEntity()).thenReturn(entity);
            when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);

            graphiteClient.httpClient = httpClient;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
