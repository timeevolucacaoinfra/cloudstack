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

package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;
import java.util.List;

public class UpdatePoolCommand extends Command{

    private String expectedHealthcheck;
    private String healthcheck;
    private String healthcheckType;
    private List<Long> poolIds;
    private Integer maxConn;

    public UpdatePoolCommand(List<Long> poolIds) {
        this.poolIds = poolIds;
    }

    public UpdatePoolCommand(List<Long> poolIds, String healthcheckType, String healthcheck, String expectedHealthcheck, Integer maxConn) {
        this.poolIds = poolIds;
        this.healthcheckType = healthcheckType;
        this.healthcheck = healthcheck;
        this.expectedHealthcheck = expectedHealthcheck;
        this.maxConn = maxConn;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getExpectedHealthcheck() {
        return expectedHealthcheck;
    }

    public void setExpectedHealthcheck(String expectedHealthcheck) {
        this.expectedHealthcheck = expectedHealthcheck;
    }

    public String getHealthcheck() {
        return healthcheck;
    }

    public void setHealthcheck(String healthcheck) {
        this.healthcheck = healthcheck;
    }

    public String getHealthcheckType() {
        return healthcheckType;
    }

    public void setHealthcheckType(String healthcheckType) {
        this.healthcheckType = healthcheckType;
    }

    public List<Long> getPoolIds() {
        return poolIds;
    }

    public void setPoolIds(List<Long> poolIds) {
        this.poolIds = poolIds;
    }

    public List<Long> getPoolId() {
        return poolIds;
    }

    public void setPoolId(List<Long> poolIds) {
        this.poolIds = poolIds;
    }

    public Integer getMaxConn() { return maxConn; }

    public void setMaxConn(Integer maxConn) { this.maxConn = maxConn; }
}
