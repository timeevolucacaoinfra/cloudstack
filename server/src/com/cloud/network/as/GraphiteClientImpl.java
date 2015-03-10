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


import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class GraphiteClientImpl implements GraphiteClient {

    @Inject
    private HttpClient httpClient;

    public GraphiteClientImpl() {
        //TODO: config httpclient bean on spring context xml

        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager();
        cm.setDefaultMaxPerRoute(200);
        cm.setMaxTotal(200);

        DefaultHttpClient httpClient = new DefaultHttpClient(cm);
        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 1000);
        HttpConnectionParams.setSoTimeout(params, 1000);
    }

    public List<String> fetchData(List<String> ids){
        HttpGet get = new HttpGet("");
        try {
            HttpResponse response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
