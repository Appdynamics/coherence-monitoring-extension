package com.appdynamics.extensions.coherence.metrics;


import com.google.common.collect.Lists;

import java.math.BigDecimal;
import java.util.List;

public class MetricsGenerator {

    public static List<Metric> generateWithCacheMetrics(){
        List<Metric> metrics = Lists.newArrayList();
        Metric metric1 = new Metric();
        metric1.setMetricName("CacheHits");
        metric1.setMetricValue(BigDecimal.valueOf(20));
        MetricProperties props = new DefaultMetricProperties();
        props.setAggregation(true);
        metric1.setProperties(props);
        metrics.add(metric1);
        Metric metric2 = new Metric();
        metric2.setMetricName("TotalGets");
        metric2.setMetricValue(BigDecimal.valueOf(100));
        metric2.setProperties(props);
        metrics.add(metric2);
        Metric metric3 = new Metric();
        metric3.setMetricName("TotalPuts");
        metric3.setMetricValue(BigDecimal.valueOf(98));
        MetricProperties props1 = new DefaultMetricProperties();
        metric3.setProperties(props1);
        metrics.add(metric3);
        return metrics;
    }

    public static List<Metric> generateWithThreadUtilMetrics(){
        List<Metric> metrics = Lists.newArrayList();
        Metric metric1 = new Metric();
        metric1.setMetricName("ThreadCount");
        metric1.setMetricValue(BigDecimal.valueOf(30));
        MetricProperties props = new DefaultMetricProperties();
        props.setAggregation(true);
        metric1.setProperties(props);
        metrics.add(metric1);
        Metric metric2 = new Metric();
        metric2.setMetricName("ThreadIdleCount");
        metric2.setMetricValue(BigDecimal.valueOf(5));
        metric2.setProperties(new DefaultMetricProperties());
        metrics.add(metric2);
        Metric metric3 = new Metric();
        metric3.setMetricName("TotalPuts");
        metric3.setMetricValue(BigDecimal.valueOf(98));
        metric3.setProperties(new DefaultMetricProperties());
        metrics.add(metric3);
        return metrics;
    }

    public static List<Metric> generateWithNoBaseMetrics(){
        List<Metric> metrics = Lists.newArrayList();
        Metric metric3 = new Metric();
        metric3.setMetricName("TotalPuts");
        metric3.setMetricValue(BigDecimal.valueOf(45));
        metric3.setProperties(new DefaultMetricProperties());
        metrics.add(metric3);
        return metrics;
    }


    static List<Metric> generateNodeMetricsForClusterAggregationAsIndividual(){
        List<Metric> metrics = Lists.newArrayList();
        Metric metric1 = new Metric();
        metric1.setMetricName("CacheHits");
        metric1.setClusterKey("ClusterA|");
        metric1.setMetricKey("MachineA|BackendCache|CacheHits");
        metric1.setMetricValue(BigDecimal.valueOf(20));
        MetricProperties props = new DefaultMetricProperties();
        props.setAggregation(true);
        metric1.setProperties(props);
        metrics.add(metric1);
        Metric metric1a = new Metric();
        metric1a.setMetricName("CacheHits");
        metric1a.setClusterKey("ClusterA|");
        metric1a.setMetricKey("MachineB|BackendCache|CacheHits");
        metric1a.setMetricValue(BigDecimal.valueOf(30));
        metric1a.setProperties(props);
        metrics.add(metric1a);
        Metric metric1c = new Metric();
        metric1c.setMetricName("CacheHits");
        metric1c.setClusterKey("ClusterA|");
        metric1c.setMetricKey("MachineC|BackendCache|CacheHits");
        metric1c.setMetricValue(BigDecimal.valueOf(50));
        metric1c.setProperties(props);
        metrics.add(metric1c);
        Metric metric2 = new Metric();
        metric2.setMetricName("TotalGets");
        metric2.setClusterKey("ClusterA|");
        metric2.setMetricValue(BigDecimal.valueOf(100));
        metric2.setProperties(new DefaultMetricProperties());
        metrics.add(metric2);
        Metric metric3 = new Metric();
        metric3.setMetricName("TotalPuts");
        metric3.setClusterKey("ClusterA|");
        metric3.setMetricValue(BigDecimal.valueOf(98));
        MetricProperties props1 = new DefaultMetricProperties();
        metric3.setProperties(props1);
        metrics.add(metric3);
        return metrics;
    }

    static List<Metric> generateNodeMetricsForClusterAggregationAsCollective(){
        List<Metric> metrics = Lists.newArrayList();
        Metric metric1 = new Metric();
        metric1.setMetricName("CacheHits");
        metric1.setClusterKey("ClusterA|");
        metric1.setMetricKey("MachineA|BackendCache|CacheHits");
        metric1.setMetricValue(BigDecimal.valueOf(20));
        MetricProperties props = new DefaultMetricProperties();
        props.setAggregation(true);
        props.setAggregationFields("SUM SUM COLLECTIVE");
        metric1.setProperties(props);
        metrics.add(metric1);
        Metric metric1a = new Metric();
        metric1a.setMetricName("CacheHits");
        metric1a.setClusterKey("ClusterA|");
        metric1a.setMetricKey("MachineB|BackendCache|CacheHits");
        metric1a.setMetricValue(BigDecimal.valueOf(30));
        metric1a.setProperties(props);
        metrics.add(metric1a);
        Metric metric1c = new Metric();
        metric1c.setMetricName("CacheHits");
        metric1c.setClusterKey("ClusterA|");
        metric1c.setMetricKey("MachineC|BackendCache|CacheHits");
        metric1c.setMetricValue(BigDecimal.valueOf(50));
        metric1c.setProperties(props);
        metrics.add(metric1c);
        Metric metric2 = new Metric();
        metric2.setMetricName("TotalGets");
        metric2.setClusterKey("ClusterA|");
        metric2.setMetricValue(BigDecimal.valueOf(100));
        metric2.setProperties(new DefaultMetricProperties());
        metrics.add(metric2);
        Metric metric3 = new Metric();
        metric3.setMetricName("TotalPuts");
        metric3.setMetricValue(BigDecimal.valueOf(98));
        MetricProperties props1 = new DefaultMetricProperties();
        metric3.setProperties(props1);
        metric3.setClusterKey("ClusterA|");
        metrics.add(metric3);
        return metrics;
    }
}
