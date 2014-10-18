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

package org.apache.cloudstack.api.response;

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class LoadBalancerCapabilitiesResponse extends BaseResponse {
    
    @SerializedName(ApiConstants.ALGORITHM) @Param(description = "the load balancer algorithms (source, roundrobin, leastconn, ...)")
    private List<String> algorithms;
    
    @SerializedName(ApiConstants.ISOLATION_METHODS) @Param(description="Load Balancer isolations")
    private List<String> isolations;
    
    @SerializedName(ApiConstants.STICKY_METHOD) @Param(description="Load Balancer sticky methods")
    private List<String> stickies;
    
    @SerializedName(ApiConstants.SCHEME) @Param(description="Load Balancer schemes")
    private List<String> schemes;

    public List<String> getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(List<String> algorithms) {
        this.algorithms = algorithms;
    }

    public List<String> getIsolations() {
        return isolations;
    }

    public void setIsolations(List<String> isolations) {
        this.isolations = isolations;
    }

    public List<String> getSchemes() {
        return schemes;
    }

    public void setSchemes(List<String> schemes) {
        this.schemes = schemes;
    }

    public List<String> getStickies() {
        return stickies;
    }

    public void setStickies(List<String> stickies) {
        this.stickies = stickies;
    }

}
