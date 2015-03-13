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
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GraphiteAutoScaleStatsCollector extends AutoScaleStatsCollector implements Configurable {

    @Inject
    protected GraphiteClient graphiteClient;

    public static final ConfigKey<String> GraphiteTargetPattern = new ConfigKey<>("Advanced", String.class, "autosacale.graphite.targetpattern", "", "Graphite's target pattern.", true, ConfigKey.Scope.Global);

    public static final Logger s_logger = Logger.getLogger(GraphiteAutoScaleStatsCollector.class.getName());

    @Override
    public Map<String, Double> retrieveMetrics(AutoScaleVmGroup asGroup, List<VMInstanceVO> vmList) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("[AutoScale] Collecting Graphite's data.");
        }

        if(vmList == null || vmList.size() == 0){
            return null;
        }

        try {
            Map<String, Pair<List<String>, Integer>> countersAndTargets = new HashMap<>();
            for(Pair<String, Integer> counterNameAndDuration : this.getPairOfCounterNameAndDuration(asGroup)){
                String counterName = counterNameAndDuration.first().split(",")[0];
                Integer duration = counterNameAndDuration.second();
                Pair<List<String>, Integer> targets = new Pair(new ArrayList<>(), duration);
                countersAndTargets.put(counterName, targets);
                for(VMInstanceVO vm : vmList){
                    targets.first().add(this.getTargetName(GraphiteTargetPattern.value(), counterName, vm));
                }
            }

            Map<String, GraphiteResult[]> graphiteResult = graphiteClient.fetchData(countersAndTargets);

            Map<String, Double> counterSummary = new HashMap<>();
            for(String counterName : graphiteResult.keySet()){
                GraphiteResult[] targetResults = graphiteResult.get(counterName);
                Double targetsAverageSum = 0.0;
                for(GraphiteResult targetResult : targetResults){
                    targetsAverageSum += targetResult.getAverage();
                }
                Double counterAveragePercent = (targetsAverageSum / targetResults.length) / 100;
                counterSummary.put(counterName, counterAveragePercent);
            }
            return counterSummary;
        }catch(Exception e){
            s_logger.error("[AutoScale] Error while reading AutoScale group " + asGroup.getId() + " Stats", e);
        }
        return null;
    }

    protected String getTargetName(String pattern, String counterName, VMInstanceVO vm){
        Map<String, Object> context = new HashMap<>();
        context.put("counterName", counterName);
        context.put("vm", vm);
        return formatter(pattern, context);
    }

    /**
     * Replace variables in a string template: #{obj.property}.
     * @see http://docs.spring.io/spring/docs/current/spring-framework-reference/html/expressions.html
     * @param template
     * @param context
     * @return
     */
    protected String formatter(String template, Map<String, Object> context) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext(context);
        evalContext.addPropertyAccessor(new MapAccessor());

        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(template, new TemplateParserContext());
        String formatted = exp.getValue(evalContext, String.class);
        return formatted;
    }

    @Override
    public String getConfigComponentName() {
        return GraphiteAutoScaleStatsCollector.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ GraphiteTargetPattern };
    }
}