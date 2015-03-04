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
package com.cloud.control;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TemporalType;
import javax.persistence.Temporal;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "execution_control")
public class ExecutionControlVO implements ExecutionControl{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "process_alias", updatable = true, nullable = false, length = 255)
    private String processAlias;

    @Column(name = "last_execution", updatable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastExecution;

    @Column(name = "mgmt_server_id", updatable = true)
    private Long managementServerId;

    public ExecutionControlVO(){}

    public ExecutionControlVO(String processAlias) {
        super();
        this.processAlias = processAlias;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setProcessAlias(String processAlias) {
        this.processAlias = processAlias;
    }

    @Override
    public String getProcessAlias() {
        return this.processAlias;
    }

    public void setLastExecution(Date lastExecution) {
        this.lastExecution = lastExecution;
    }

    @Override
    public Date getLastExecution() {
        return this.lastExecution;
    }

    public void setManagementServerId(Long managementServerId) {
        this.managementServerId = managementServerId;
    }

    @Override
    public Long getManagementServerId() {
        return this.managementServerId;
    }
}
