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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PerformanceMonitorAnswer;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.as.dao.AutoScalePolicyConditionMapDao;
import com.cloud.network.as.dao.AutoScalePolicyDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.as.dao.AutoScaleVmProfileDao;
import com.cloud.network.as.dao.ConditionDao;
import com.cloud.network.as.dao.CounterDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RRDAutoScaleStatsCollectorTest {

    RRDAutoScaleStatsCollector rrdAutoScaleStatsCollector = new RRDAutoScaleStatsCollector();
    AutoScaleVmGroup asGroup;
    List<VMInstanceVO> vmList = new ArrayList<>();

    @Before
    public void setUp() {
        asGroup = new AutoScaleVmGroupVO(1L,1l, 1L, 1L, 1, 3, 80, 30, new Date(), 1, "enabled");
        VMInstanceVO vm = new VMInstanceVO(1, 1, "vm-01", "vm-01", VirtualMachine.Type.Instance, 1L, Hypervisor.HypervisorType.Simulator, 1, 1, 1, false, true, 1L);
        vm.setHostId(1L);
        vmList.add(vm);
    }

    @Test
    public void testReadVmStatsWithCpuCounter(){
        mockAutoScaleVmGroupVmMapDao();
        mockAutoScaleGroupPolicyMapDao();
        mockAutoScalePolicyDao();
        mockAutoScalePolicyConditionMapDao();
        mockConditionDao();
        mockCounterDao("cpu");
        mockAgenManager("0.1");

        Map<String, Double> countersSummary = rrdAutoScaleStatsCollector.retrieveMetrics(asGroup, vmList);

        assert countersSummary.get("cpu") == 0.1D;
    }

    @Test
    public void testReadVmStatsWithMemoryCounter(){
        mockAutoScaleVmGroupVmMapDao();
        mockAutoScaleGroupPolicyMapDao();
        mockAutoScalePolicyDao();
        mockAutoScalePolicyConditionMapDao();
        mockConditionDao();
        mockCounterDao("memory");
        mockAgenManager("128");
        mockAutoScaleProfileDao();
        mockServiceOfferingDao(512);

        Map<String, Double> countersSummary = rrdAutoScaleStatsCollector.retrieveMetrics(asGroup, vmList);

        assert countersSummary.get("memory") == 0.25D; //fifty percent of 256 RAM
    }

    @Test
    public void testReadVmStatsGivenVmListEmpty(){
        vmList = new ArrayList<>();
        Map<String, Double> countersSummary = rrdAutoScaleStatsCollector.retrieveMetrics(asGroup, vmList);

        assert countersSummary == null;
    }

    @Test
    public void testReadVmStatsGivenVmListIsNull(){
        vmList = null;
        Map<String, Double> countersSummary = rrdAutoScaleStatsCollector.retrieveMetrics(asGroup, vmList);

        assert countersSummary == null;
    }

    @Test
    public void testReadVmStatsWithError(){
        AutoScaleVmGroupVmMapDao _asGroupVmDao = mock(AutoScaleVmGroupVmMapDao.class);
        when(_asGroupVmDao.listByGroup(anyLong())).thenThrow(new RuntimeException());
        rrdAutoScaleStatsCollector._asGroupVmDao = _asGroupVmDao;

        Map<String, Double> countersSummary = rrdAutoScaleStatsCollector.retrieveMetrics(asGroup, vmList);

        assert countersSummary == null;
    }

    private void mockAutoScaleVmGroupVmMapDao(){
        AutoScaleVmGroupVmMapDao _asGroupVmDao = mock(AutoScaleVmGroupVmMapDao.class);
        List<AutoScaleVmGroupVmMapVO> asGroupVmVOs = new ArrayList<>();
        asGroupVmVOs.add(new AutoScaleVmGroupVmMapVO(1L, 1L));
        when(_asGroupVmDao.listByGroup(anyLong())).thenReturn(asGroupVmVOs);
        rrdAutoScaleStatsCollector._asGroupVmDao = _asGroupVmDao;
    }
    private void mockAgenManager(String counterValue) {
        AgentManager agentManager = mock(AgentManager.class);
        Answer answer = new PerformanceMonitorAnswer(null, true, "1.1:" + counterValue);
        try {
            when(agentManager.send(eq(1L), any(Command.class))).thenReturn(answer);
        }catch(Exception e){}
        rrdAutoScaleStatsCollector._agentMgr = agentManager;
    }

    private void mockCounterDao(String counterName) {
        CounterDao _asCounterDao = mock(CounterDao.class);
        CounterVO counter = new CounterVO(Counter.Source.valueOf(counterName), counterName, "");
        when(_asCounterDao.findById(1L)).thenReturn(counter);
        rrdAutoScaleStatsCollector._asCounterDao = _asCounterDao;
    }

    private void mockConditionDao() {
        ConditionDao _asConditionDao = mock(ConditionDao.class);
        ConditionVO conditionUp = new ConditionVO(1L, 90L, 1L, 1L, Condition.Operator.GT);
        ConditionVO conditionDown = new ConditionVO(1L, 10L, 1L, 1L, Condition.Operator.LT);
        when(_asConditionDao.findById(1L)).thenReturn(conditionUp);
        when(_asConditionDao.findById(2L)).thenReturn(conditionDown);
        rrdAutoScaleStatsCollector._asConditionDao = _asConditionDao;
    }

    private void mockAutoScalePolicyConditionMapDao() {
        AutoScalePolicyConditionMapDao _asConditionMapDao = mock(AutoScalePolicyConditionMapDao.class);
        List<AutoScalePolicyConditionMapVO> policiesMap = new ArrayList<>();
        policiesMap.add(new AutoScalePolicyConditionMapVO(1L, 1L));
        when(_asConditionMapDao.findByPolicyId(anyLong())).thenReturn(policiesMap);
        rrdAutoScaleStatsCollector._asConditionMapDao = _asConditionMapDao;
    }

    private void mockAutoScalePolicyDao() {
        AutoScalePolicyDao _asPolicyDao = mock(AutoScalePolicyDao.class);
        AutoScalePolicyVO policyUp = new AutoScalePolicyVO(1L, 1L, 60, 120, new Date(), "scaleup");
        AutoScalePolicyVO policyDown = new AutoScalePolicyVO(1L, 1L, 60, 120, new Date(), "scaledown");
        when(_asPolicyDao.findById(1L)).thenReturn(policyUp);
        when(_asPolicyDao.findById(2L)).thenReturn(policyDown);
        rrdAutoScaleStatsCollector._asPolicyDao = _asPolicyDao;
    }

    private void mockAutoScaleGroupPolicyMapDao() {
        AutoScaleVmGroupPolicyMapDao _asGroupPolicyDao = mock(AutoScaleVmGroupPolicyMapDao.class);
        List<AutoScaleVmGroupPolicyMapVO> groupPolicymap = new ArrayList<>();
        groupPolicymap.add(new AutoScaleVmGroupPolicyMapVO(1L, 1L));
        groupPolicymap.add(new AutoScaleVmGroupPolicyMapVO(1L, 2L));
        when(_asGroupPolicyDao.listByVmGroupId(anyLong())).thenReturn(groupPolicymap);
        rrdAutoScaleStatsCollector._asGroupPolicyDao = _asGroupPolicyDao;
    }

    private void mockServiceOfferingDao(Integer memorySize) {
        ServiceOfferingDao _serviceOfferingDao = mock(ServiceOfferingDao.class);
        when(_serviceOfferingDao.findById(anyLong())).thenReturn(new ServiceOfferingVO("offering", 100, memorySize, 1, 1000, 1000, false, "offering", false, false, null, false, VirtualMachine.Type.Instance, false));
        rrdAutoScaleStatsCollector._serviceOfferingDao = _serviceOfferingDao;
    }

    private void mockAutoScaleProfileDao() {
        AutoScaleVmProfileDao _asProfileDao = mock(AutoScaleVmProfileDao.class);
        when(_asProfileDao.findById(anyLong())).thenReturn(new AutoScaleVmProfileVO(1L, 1L, 1L, 1L, 1L, null, null, 30, 1L));
        rrdAutoScaleStatsCollector._asProfileDao = _asProfileDao;
    }
}
