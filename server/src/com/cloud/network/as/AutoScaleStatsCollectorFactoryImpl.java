package com.cloud.network.as;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import javax.inject.Inject;

public class AutoScaleStatsCollectorFactoryImpl implements AutoScaleStatsCollectorFactory, Configurable {

    @Inject
    RRDAutoScaleStatsCollector rrdAutoScaleStatsCollector;
    @Inject
    GraphiteAutoScaleStatsCollector graphiteAutoScaleStatsCollector;

    private static final String RRD = "rrd";
    private static final String GRAPHITE = "graphite";

    private static final ConfigKey<String> StatsDataSource = new ConfigKey<String>("Advanced", String.class, "autosacale.stats.datasource", "rrd",
            "Auto scale VM stats data source (rrd/graphite)", true, ConfigKey.Scope.Global);

    @Override
    public AutoScaleStatsCollector getStatsCollector() {
        if(RRD.equals(StatsDataSource.value())){
            return rrdAutoScaleStatsCollector;
        }else if(GRAPHITE.equals(StatsDataSource.value())){
            return graphiteAutoScaleStatsCollector;
        }
        return rrdAutoScaleStatsCollector;
    }

    @Override
    public String getConfigComponentName() {
        return AutoScaleStatsCollectorFactoryImpl.class.getName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ StatsDataSource };
    }
}
