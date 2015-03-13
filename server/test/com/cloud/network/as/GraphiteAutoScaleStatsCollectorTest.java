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
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphiteAutoScaleStatsCollectorTest extends AutoScaleStatsCollectorTest{

    @Before
    public void setUp() {
        autoScaleStatsCollector = new GraphiteAutoScaleStatsCollector();
        asGroup = new AutoScaleVmGroupVO(1L,1l, 1L, 1L, 1, 3, 80, 30, new Date(), 1, "enabled");
        VMInstanceVO vm = new VMInstanceVO(1, 1, "vm-01", "vm-01", VirtualMachine.Type.Instance, 1L, Hypervisor.HypervisorType.Simulator, 1, 1, 1, false, true, 1L);
        vm.setHostId(1L);
        vmList.add(vm);
    }

    @Test
    public void testReadVmStatsWithCpuCounter(){
        mockGraphiteClient("cpu", 10);
        super.testReadVmStatsWithCpuCounter();
    }

    @Test
    public void testReadVmStatsWithMemoryCounter(){
        mockGraphiteClient("memory", 25);
        super.testReadVmStatsWithMemoryCounter();
    }

    @Test
    public void testCalculateAverage(){
        mockAutoScaleGroupPolicyMapDao();
        mockAutoScalePolicyDao();
        mockAutoScalePolicyConditionMapDao();
        mockConditionDao();
        mockCounterDao("cpu");

        List<List<Number>> dataPoints = new ArrayList<>();
        dataPoints.add(Arrays.asList(new Number[]{ 10, 1426173040}));
        dataPoints.add(Arrays.asList(new Number[]{ 50, 1426173040}));
        GraphiteResult graphiteResult = new GraphiteResult("stats.vm-1.gauges.cpu", dataPoints);

        Map<String, GraphiteResult[]> result = new HashMap<>();
        result.put("cpu", new GraphiteResult[]{ graphiteResult });
        GraphiteClient graphiteClient = mock(GraphiteClient.class);
        when(graphiteClient.fetchData(anyMap())).thenReturn(result);
        ((GraphiteAutoScaleStatsCollector)autoScaleStatsCollector).graphiteClient = graphiteClient;

        Map<String, Double> countersSummary = autoScaleStatsCollector.retrieveMetrics(asGroup, vmList);

        assert countersSummary.get("cpu") == 0.3D;
    }

    @Test
    public void testCalculateAverageWithMoreThanOneTarget(){
        mockAutoScaleGroupPolicyMapDao();
        mockAutoScalePolicyDao();
        mockAutoScalePolicyConditionMapDao();
        mockConditionDao();
        mockCounterDao("cpu");

        List<List<Number>> dataPoints = new ArrayList<>();
        dataPoints.add(Arrays.asList(new Number[]{ 10, 1426173040}));
        dataPoints.add(Arrays.asList(new Number[]{ 50, 1426173040}));
        GraphiteResult graphiteResult = new GraphiteResult("stats.vm-1.gauges.cpu", dataPoints);

        List<List<Number>> dataPoints2 = new ArrayList<>();
        dataPoints2.add(Arrays.asList(new Number[]{ 20, 1426173040}));
        dataPoints2.add(Arrays.asList(new Number[]{ 20, 1426173040}));
        GraphiteResult graphiteResult2 = new GraphiteResult("stats.vm-2.gauges.cpu", dataPoints2);

        Map<String, GraphiteResult[]> result = new HashMap<>();
        result.put("cpu", new GraphiteResult[]{ graphiteResult, graphiteResult2 });
        GraphiteClient graphiteClient = mock(GraphiteClient.class);
        when(graphiteClient.fetchData(anyMap())).thenReturn(result);
        ((GraphiteAutoScaleStatsCollector)autoScaleStatsCollector).graphiteClient = graphiteClient;


        Map<String, Double> countersSummary = autoScaleStatsCollector.retrieveMetrics(asGroup, vmList);

        assert countersSummary.get("cpu") == 0.25D;
    }

    @Test
    public void testGetTargetName(){
        String pattern = "stats.#{vm.hostName}.#{counterName}";
        String targetName = ((GraphiteAutoScaleStatsCollector)autoScaleStatsCollector).getTargetName(pattern, "cpu", vmList.get(0));

        assert ("stats." + vmList.get(0).getHostName() + ".cpu").equals(targetName);
    }

    private void mockGraphiteClient(String counter, Integer avgPercent){
        List<List<Number>> dataPoints = new ArrayList<>();
        dataPoints.add(Arrays.asList(new Number[]{ avgPercent, 1426173040}));
        GraphiteResult graphiteResult = new GraphiteResult("stats.vm-1.gauges.cpu", dataPoints);

        Map<String, GraphiteResult[]> result = new HashMap<>();
        result.put(counter, new GraphiteResult[]{ graphiteResult });
        GraphiteClient graphiteClient = mock(GraphiteClient.class);
        when(graphiteClient.fetchData(anyMap())).thenReturn(result);
        ((GraphiteAutoScaleStatsCollector)autoScaleStatsCollector).graphiteClient = graphiteClient;
    }
}
