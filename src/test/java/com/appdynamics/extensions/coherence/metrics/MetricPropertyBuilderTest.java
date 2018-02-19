/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence.metrics;


import com.appdynamics.extensions.coherence.DictionaryGenerator;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class MetricPropertyBuilderTest {

    @Test
    public void whenIncludeMetricsIsNull_thenReturnEmptyMap(){
        MetricPropertiesBuilder builder = new MetricPropertiesBuilder();
        Map<String,MetricProperties> propsMap = builder.build(null);
        Assert.assertTrue(propsMap.size() == 0);
    }

    @Test
    public void whenIncludeMetricsIsEmpty_thenReturnEmptyMap(){
        MetricPropertiesBuilder builder = new MetricPropertiesBuilder();
        Map<String,MetricProperties> propsMap = builder.build(Maps.newHashMap());
        Assert.assertTrue(propsMap.size() == 0);
    }

    @Test
    public void whenNothing_thenBuildMapWithDefaults(){
        List<Map> dictionary = DictionaryGenerator.createIncludeDictionaryWithDefaults();
        Map metricsMap = getMetricsMap(dictionary);
        MetricPropertiesBuilder builder = new MetricPropertiesBuilder();
        Map<String,MetricProperties> propsMap = builder.build(metricsMap);
        Assert.assertTrue(propsMap.size() == dictionary.size());
        for(Map.Entry<String,MetricProperties> entry : propsMap.entrySet()){
            MetricProperties props = entry.getValue();
            Assert.assertTrue(props.getAggregationType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE));
            Assert.assertTrue(props.getTimeRollupType().equals(MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE));
            Assert.assertTrue(props.getClusterRollupType().equals(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL));
            Assert.assertTrue(!props.isAggregation());
        }
    }

    private Map getMetricsMap(List<Map> dictionary) {
        Map includeMap = Maps.newHashMap();
        includeMap.put("include",dictionary);
        Map metricsMap = Maps.newHashMap();
        metricsMap.put("metrics",includeMap);
        return metricsMap;
    }

    @Test
    public void whenGlobalOverrides_thensBuildMapWithGlobalOverrides(){
        List<Map> dictionary = DictionaryGenerator.createIncludeDictionaryWithDefaults();
        Map metricsMap = getMetricsMap(dictionary);
        metricsMap.put("metricType","SUM SUM COLLECTIVE");
        metricsMap.put("aggregation",true);
        MetricPropertiesBuilder builder = new MetricPropertiesBuilder();
        Map<String,MetricProperties> propsMap = builder.build(metricsMap);
        Assert.assertTrue(propsMap.size() == dictionary.size());
        for(Map.Entry<String,MetricProperties> entry : propsMap.entrySet()){
            MetricProperties props = entry.getValue();
            Assert.assertTrue(props.getAggregationType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_SUM));
            Assert.assertTrue(props.getTimeRollupType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_SUM));
            Assert.assertTrue(props.getClusterRollupType().equals(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE));
            Assert.assertTrue(props.isAggregation());
        }
    }


    @Test
    public void whenLocalOverrides_thenBuildMapWithLocalOverrides(){
        List<Map> dictionary = DictionaryGenerator.createIncludeDictionaryWithLocalOverrides();
        Map metricsMap = getMetricsMap(dictionary);
        MetricPropertiesBuilder builder = new MetricPropertiesBuilder();
        Map<String,MetricProperties> propsMap = builder.build(metricsMap);
        Assert.assertTrue(propsMap.size() == dictionary.size());
        for(Map.Entry<String,MetricProperties> entry : propsMap.entrySet()){
            MetricProperties props = entry.getValue();
            if(entry.getKey().equals("CacheHits")){
                Assert.assertTrue(props.getAggregationType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_SUM));
                Assert.assertTrue(props.getTimeRollupType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_SUM));
                Assert.assertTrue(props.getClusterRollupType().equals(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE));
            }
            else if(entry.getKey().equals("TotalGets")){
                Assert.assertTrue(props.isAggregation());
            }
            else {
                Assert.assertTrue(props.getAggregationType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE));
                Assert.assertTrue(props.getTimeRollupType().equals(MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE));
                Assert.assertTrue(props.getClusterRollupType().equals(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL));
                Assert.assertTrue(!props.isAggregation());
            }
        }
    }

    @Test
    public void whenComboOfLocalAndGlobalOverrides_thenBuildMapWithCombo(){
        List<Map> dictionary = DictionaryGenerator.createIncludeDictionaryWithLocalOverrides();
        Map metricsMap = getMetricsMap(dictionary);
        metricsMap.put("metricType","OBSERVATION CURRENT COLLECTIVE");
        MetricPropertiesBuilder builder = new MetricPropertiesBuilder();
        Map<String,MetricProperties> propsMap = builder.build(metricsMap);
        Assert.assertTrue(propsMap.size() == dictionary.size());
        for(Map.Entry<String,MetricProperties> entry : propsMap.entrySet()){
            MetricProperties props = entry.getValue();
            if(entry.getKey().equals("CacheHits")){
                Assert.assertTrue(props.getAggregationType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_SUM));
                Assert.assertTrue(props.getTimeRollupType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_SUM));
                Assert.assertTrue(props.getClusterRollupType().equals(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE));
                Assert.assertTrue(!props.isAggregation());
            }
            else if(entry.getKey().equals("TotalGets")){
                Assert.assertTrue(props.isAggregation());
                Assert.assertTrue(props.getAggregationType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION));
                Assert.assertTrue(props.getTimeRollupType().equals(MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT));
                Assert.assertTrue(props.getClusterRollupType().equals(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE));
            }
            else {
                Assert.assertTrue(!props.isAggregation());
                Assert.assertTrue(props.getAggregationType().equals(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION));
                Assert.assertTrue(props.getTimeRollupType().equals(MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT));
                Assert.assertTrue(props.getClusterRollupType().equals(MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE));
            }
        }
    }

}
