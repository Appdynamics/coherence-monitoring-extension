package com.appdynamics.extensions.coherence.metrics.derived;


import com.appdynamics.extensions.coherence.metrics.Metric;
import com.appdynamics.extensions.coherence.metrics.MetricProperties;
import com.appdynamics.extensions.coherence.metrics.MetricsGenerator;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class CacheHitRatioTest {

    BigDecimal minusOne = BigDecimal.valueOf(-1);

    @Test
    public void whenNoCacheMetrics_thenReturnMinusOne(){
        DerivedMetric dm = new CacheHitRatio();
        Assert.assertTrue(dm.getMetricValue(MetricsGenerator.generateWithNoBaseMetrics()).equals(minusOne));
    }

    @Test
    public void whenNullMetrics_thenReturnMinusOne(){
        DerivedMetric dm = new CacheHitRatio();
        Assert.assertTrue(dm.getMetricValue(null).equals(minusOne));
    }

    @Test
    public void whenEmptyMetrics_thenReturnMinusOne(){
        DerivedMetric dm = new CacheHitRatio();
        Assert.assertTrue(dm.getMetricValue(Lists.<Metric>newArrayList()).equals(minusOne));
    }

    @Test
    public void whenValidCacheMetrics_thenReturnValidValue(){
        DerivedMetric dm = new CacheHitRatio();
        BigDecimal value = dm.getMetricValue(MetricsGenerator.generateWithCacheMetrics());
        Assert.assertTrue(value.intValue() == 20);
    }

    @Test
    public void whenValidCacheMetrics_thenReturnSetAggregationValidly(){
        DerivedMetric dm = new CacheHitRatio();
        BigDecimal value = dm.getMetricValue(MetricsGenerator.generateWithCacheMetrics());
        MetricProperties props = dm.getMetricProperties(null);
        Assert.assertTrue(props.isAggregation());
    }


}
