/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence.metrics;

import com.appdynamics.extensions.util.AggregatorFactory;
import com.appdynamics.extensions.util.AggregatorKey;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.List;

public class ClusterMetricsProcessor {

    static final String IND = "IND";
    static final String SUM = "SUM";

    public void collect(AggregatorFactory aggregatorFactory, List<Metric> nodeMetrics) {
        if(nodeMetrics == null){
            return;
        }
        for(Metric nodeMetric : nodeMetrics){
            if(nodeMetric.getProperties().isAggregation()){
                String metricType = getMetricTypeInOtherFormat(nodeMetric.getProperties());
                AggregatorKey aggKey = new AggregatorKey(nodeMetric.getClusterKey() + nodeMetric.getMetricNameOrAlias(),metricType);
                aggregatorFactory.getAggregator(metricType).add(aggKey,nodeMetric.getMetricValue().toString());
            }
        }
    }

    private String getMetricTypeInOtherFormat(MetricProperties properties) {
        //converting it into a string so that commons lib can handle it.
        String str = properties.getAggregationType() + "." + properties.getTimeRollupType();
        if(properties.getClusterRollupType().equalsIgnoreCase(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL)){
            return  str + "." + IND;
        }
        return str + "." + SUM;
    }
}
