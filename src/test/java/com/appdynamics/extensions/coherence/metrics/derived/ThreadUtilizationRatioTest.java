package com.appdynamics.extensions.coherence.metrics.derived;


import com.appdynamics.extensions.coherence.metrics.Metric;
import com.appdynamics.extensions.coherence.metrics.MetricProperties;
import com.appdynamics.extensions.coherence.metrics.MetricsGenerator;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class ThreadUtilizationRatioTest {

    BigDecimal minusOne = BigDecimal.valueOf(-1);

    @Test
    public void whenNoThreadUtilMetrics_thenReturnMinusOne(){
        DerivedMetric dm = new ThreadUtilizationRatio();
        Assert.assertTrue(dm.getMetricValue(MetricsGenerator.generateWithNoBaseMetrics()).equals(minusOne));
    }

    @Test
    public void whenNullMetrics_thenReturnMinusOne(){
        DerivedMetric dm = new ThreadUtilizationRatio();
        Assert.assertTrue(dm.getMetricValue(null).equals(minusOne));
    }

    @Test
    public void whenEmptyMetrics_thenReturnMinusOne(){
        DerivedMetric dm = new ThreadUtilizationRatio();
        Assert.assertTrue(dm.getMetricValue(Lists.<Metric>newArrayList()).equals(minusOne));
    }

    @Test
    public void whenValidThreadUtilMetrics_thenReturnValidValue(){
        DerivedMetric dm = new ThreadUtilizationRatio();
        BigDecimal value = dm.getMetricValue(MetricsGenerator.generateWithThreadUtilMetrics());
        Assert.assertTrue(value.intValue() == 83);
    }

    @Test
    public void whenValidThreadUtilMetrics_thenSetAggregationValidly(){
        DerivedMetric dm = new ThreadUtilizationRatio();
        BigDecimal value = dm.getMetricValue(MetricsGenerator.generateWithThreadUtilMetrics());
        MetricProperties props = dm.getMetricProperties(null);
        Assert.assertTrue(!props.isAggregation());
    }


}
