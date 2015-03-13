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

import com.cloud.hypervisor.Hypervisor;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import org.junit.Before;

import java.util.Date;

public class RRDAutoScaleStatsCollectorTest extends AutoScaleStatsCollectorTest{

    @Before
    public void setUp() {
        autoScaleStatsCollector = new RRDAutoScaleStatsCollector();
        asGroup = new AutoScaleVmGroupVO(1L,1l, 1L, 1L, 1, 3, 80, 30, new Date(), 1, "enabled");
        VMInstanceVO vm = new VMInstanceVO(1, 1, "vm-01", "vm-01", VirtualMachine.Type.Instance, 1L, Hypervisor.HypervisorType.Simulator, 1, 1, 1, false, true, 1L);
        vm.setHostId(1L);
        vmList.add(vm);
    }
}
